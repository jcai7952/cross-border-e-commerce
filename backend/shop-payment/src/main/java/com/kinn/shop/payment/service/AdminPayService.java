package com.kinn.shop.payment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kinn.shop.api.order.OrderFacade;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.payment.channel.ChannelRefundResult;
import com.kinn.shop.payment.channel.ChannelRegistry;
import com.kinn.shop.payment.channel.ChannelStatus;
import com.kinn.shop.payment.dto.RefundCreateDTO;
import com.kinn.shop.payment.entity.PayOrder;
import com.kinn.shop.payment.entity.RefundOrder;
import com.kinn.shop.payment.mapper.PayOrderMapper;
import com.kinn.shop.payment.mapper.RefundOrderMapper;
import com.kinn.shop.payment.vo.AdminPayDetailVO;
import com.kinn.shop.payment.vo.AdminPayPageVO;
import com.kinn.shop.payment.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端：支付流水/退款查询 + 退款（支持部分退款，超可退余额拒绝）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPayService {

    private final PayOrderMapper payOrderMapper;
    private final RefundOrderMapper refundOrderMapper;
    private final ChannelRegistry channelRegistry;
    private final OrderFacade orderFacade;

    /**
     * 退款：pay_order 必须 SUCCESS；已退合计(SUCCESS+PROCESSING)+本次 ≤ 支付金额。
     * 全额退完（SUCCESS 合计==支付金额）后 best-effort 通知订单服务关单。
     */
    public RefundVO refund(String payNo, RefundCreateDTO dto) {
        LoginContext.requireAdminId();
        PayOrder po = requirePayOrder(payNo);
        if (!PayStatus.SUCCESS.equals(po.getStatus())) {
            throw new BizException(ErrorCode.PAY_STATUS_ILLEGAL);
        }
        long occupied = sumRefunded(payNo, List.of(RefundStatus.SUCCESS, RefundStatus.PROCESSING));
        if (occupied + dto.getAmountMinor() > po.getAmountCents()) {
            throw new BizException(ErrorCode.REFUND_EXCEED);
        }

        RefundOrder ro = new RefundOrder();
        ro.setRefundNo(PayNos.gen("R"));
        ro.setPayNo(po.getPayNo());
        ro.setOrderNo(po.getOrderNo());
        ro.setUserId(po.getUserId());
        ro.setAmountCents(dto.getAmountMinor());
        ro.setCurrency(po.getCurrency());
        ro.setStatus(RefundStatus.PROCESSING);
        ro.setReason(dto.getReason());
        refundOrderMapper.insert(ro);

        ChannelRefundResult result;
        try {
            result = channelRegistry.byCode(po.getChannel()).refund(ro, po);
        } catch (BizException e) {
            markRefund(ro, RefundStatus.FAILED, null);
            throw e;
        } catch (Exception e) {
            log.error("[refund] channel refund failed, refundNo={}", ro.getRefundNo(), e);
            markRefund(ro, RefundStatus.FAILED, null);
            throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
        }

        switch (result.status()) {
            case SUCCESS -> markRefund(ro, RefundStatus.SUCCESS, result.channelRefundNo());
            case FAILED -> markRefund(ro, RefundStatus.FAILED, result.channelRefundNo());
            default -> markRefund(ro, RefundStatus.PROCESSING, result.channelRefundNo());
        }

        if (RefundStatus.SUCCESS.equals(ro.getStatus())
                && sumRefunded(payNo, List.of(RefundStatus.SUCCESS)) >= po.getAmountCents()) {
            markOrderRefundedQuietly(po, ro);
        }
        return RefundVO.from(ro);
    }

    public PageResult<AdminPayPageVO> page(String orderNo, String channel, String status,
                                           long pageNum, long pageSize) {
        LoginContext.requireAdminId();
        long pn = Math.max(pageNum, 1);
        long ps = Math.min(Math.max(pageSize, 1), 100);
        Page<PayOrder> page = payOrderMapper.selectPage(new Page<>(pn, ps), Wrappers.<PayOrder>lambdaQuery()
                .eq(orderNo != null && !orderNo.isBlank(), PayOrder::getOrderNo, orderNo)
                .eq(channel != null && !channel.isBlank(), PayOrder::getChannel, channel)
                .eq(status != null && !status.isBlank(), PayOrder::getStatus, status)
                .orderByDesc(PayOrder::getId));
        return PageResult.of(page.getTotal(), pn, ps,
                page.getRecords().stream().map(AdminPayPageVO::from).toList());
    }

    public PageResult<RefundVO> refundPage(String payNo, long pageNum, long pageSize) {
        LoginContext.requireAdminId();
        long pn = Math.max(pageNum, 1);
        long ps = Math.min(Math.max(pageSize, 1), 100);
        Page<RefundOrder> page = refundOrderMapper.selectPage(new Page<>(pn, ps), Wrappers.<RefundOrder>lambdaQuery()
                .eq(payNo != null && !payNo.isBlank(), RefundOrder::getPayNo, payNo)
                .orderByDesc(RefundOrder::getId));
        return PageResult.of(page.getTotal(), pn, ps,
                page.getRecords().stream().map(RefundVO::from).toList());
    }

    public AdminPayDetailVO detail(String payNo) {
        LoginContext.requireAdminId();
        PayOrder po = requirePayOrder(payNo);
        AdminPayDetailVO vo = AdminPayDetailVO.from(po);
        List<RefundOrder> refunds = refundOrderMapper.selectList(Wrappers.<RefundOrder>lambdaQuery()
                .eq(RefundOrder::getPayNo, payNo)
                .orderByDesc(RefundOrder::getId));
        vo.setRefunds(refunds.stream().map(RefundVO::from).toList());
        vo.setRefundedCents(refunds.stream()
                .filter(r -> RefundStatus.SUCCESS.equals(r.getStatus()))
                .mapToLong(RefundOrder::getAmountCents)
                .sum());
        return vo;
    }

    private long sumRefunded(String payNo, List<String> statuses) {
        return refundOrderMapper.selectList(Wrappers.<RefundOrder>lambdaQuery()
                        .eq(RefundOrder::getPayNo, payNo)
                        .in(RefundOrder::getStatus, statuses))
                .stream().mapToLong(RefundOrder::getAmountCents).sum();
    }

    private void markRefund(RefundOrder ro, String status, String channelRefundNo) {
        ro.setStatus(status);
        ro.setChannelRefundNo(channelRefundNo);
        if (RefundStatus.SUCCESS.equals(status)) {
            ro.setRefundedAt(LocalDateTime.now());
        }
        refundOrderMapper.updateById(ro);
    }

    /** 全额退完通知订单关单：订单可能已发货（false），仅 log.warn，不回滚退款。 */
    private void markOrderRefundedQuietly(PayOrder po, RefundOrder ro) {
        try {
            boolean ok = orderFacade.markRefunded(po.getOrderNo(), ro.getRefundNo());
            if (!ok) {
                log.warn("[refund] markRefunded rejected (order may be shipped), orderNo={}, refundNo={}",
                        po.getOrderNo(), ro.getRefundNo());
            }
        } catch (Exception e) {
            log.warn("[refund] markRefunded rpc failed, orderNo={}, refundNo={}", po.getOrderNo(), ro.getRefundNo(), e);
        }
    }

    private PayOrder requirePayOrder(String payNo) {
        PayOrder po = payOrderMapper.selectOne(Wrappers.<PayOrder>lambdaQuery().eq(PayOrder::getPayNo, payNo));
        if (po == null) {
            throw new BizException(ErrorCode.PAY_ORDER_NOT_FOUND);
        }
        return po;
    }
}
