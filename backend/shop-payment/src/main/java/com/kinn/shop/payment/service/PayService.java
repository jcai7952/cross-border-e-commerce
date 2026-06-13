package com.kinn.shop.payment.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinn.shop.api.order.OrderFacade;
import com.kinn.shop.api.order.dto.OrderPayInfoDTO;
import com.kinn.shop.common.constant.RedisKeys;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.payment.channel.ChannelCreateResult;
import com.kinn.shop.payment.channel.ChannelQueryResult;
import com.kinn.shop.payment.channel.ChannelRegistry;
import com.kinn.shop.payment.channel.ChannelStatus;
import com.kinn.shop.payment.channel.PayChannel;
import com.kinn.shop.payment.dto.PayCreateDTO;
import com.kinn.shop.payment.entity.LocalMessage;
import com.kinn.shop.payment.entity.PayNotifyLog;
import com.kinn.shop.payment.entity.PayOrder;
import com.kinn.shop.payment.mapper.LocalMessageMapper;
import com.kinn.shop.payment.mapper.PayNotifyLogMapper;
import com.kinn.shop.payment.mapper.PayOrderMapper;
import com.kinn.shop.payment.vo.PayCreateVO;
import com.kinn.shop.payment.vo.PayOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付核心：发起支付（同单同渠道复用）+ 统一幂等入账 settle + 主动同步兜底。
 * settle 幂等三层：Redis SETNX → pay_notify_log 唯一索引 → pay_order 状态 CAS。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayService {

    public static final String BIZ_ORDER_MARK_PAID = "ORDER_MARK_PAID";

    private final PayOrderMapper payOrderMapper;
    private final PayNotifyLogMapper payNotifyLogMapper;
    private final LocalMessageMapper localMessageMapper;
    private final ChannelRegistry channelRegistry;
    private final OrderFacade orderFacade;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 发起支付：校验订单（归属/状态/时限）后创建或复用支付单，调渠道拿前端载荷。
     */
    public PayCreateVO create(PayCreateDTO dto) {
        long userId = LoginContext.requireUserId();
        PayChannel channel = channelRegistry.byCode(dto.getChannel());

        OrderPayInfoDTO info = orderFacade.getPayInfo(dto.getOrderNo());
        if (info == null) {
            throw new BizException(ErrorCode.PAY_ORDER_NOT_FOUND, ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
        if (!Long.valueOf(userId).equals(info.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (!"WAIT_PAY".equals(info.getStatus())) {
            throw new BizException(ErrorCode.PAY_STATUS_ILLEGAL);
        }
        if (info.getPayDeadline() != null && info.getPayDeadline().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.PAY_STATUS_ILLEGAL, "订单已超时");
        }
        // 任何渠道已有成功支付单（订单态可能尚未同步）：禁止重复支付
        Long successCount = payOrderMapper.selectCount(Wrappers.<PayOrder>lambdaQuery()
                .eq(PayOrder::getOrderNo, info.getOrderNo())
                .eq(PayOrder::getStatus, PayStatus.SUCCESS));
        if (successCount != null && successCount > 0) {
            throw new BizException(ErrorCode.PAY_STATUS_ILLEGAL, "订单已存在成功支付单，请勿重复支付");
        }

        // 同 orderNo+channel 的未付单复用，重新 create 刷新载荷
        PayOrder po = payOrderMapper.selectList(Wrappers.<PayOrder>lambdaQuery()
                        .eq(PayOrder::getOrderNo, info.getOrderNo())
                        .eq(PayOrder::getChannel, channel.code())
                        .in(PayOrder::getStatus, PayStatus.CREATED, PayStatus.PENDING)
                        .orderByDesc(PayOrder::getId)
                        .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
        if (po == null) {
            po = new PayOrder();
            po.setPayNo(PayNos.gen("P"));
            po.setOrderNo(info.getOrderNo());
            po.setUserId(info.getUserId());
            po.setChannel(channel.code());
            po.setAmountCents(info.getPayAmountMinor());
            po.setCurrency(info.getPayCurrency());
            po.setStatus(PayStatus.CREATED);
            po.setVersion(0);
            payOrderMapper.insert(po);
        }

        ChannelCreateResult created = channel.create(po);
        payOrderMapper.update(null, Wrappers.<PayOrder>lambdaUpdate()
                .eq(PayOrder::getId, po.getId())
                .set(PayOrder::getStatus, PayStatus.PENDING)
                .set(PayOrder::getChannelTradeNo, created.channelTradeNo())
                .set(PayOrder::getChannelPayload, toJson(created.payload())));

        PayCreateVO vo = new PayCreateVO();
        vo.setPayNo(po.getPayNo());
        vo.setChannel(channel.code());
        vo.setPayloadType(created.payloadType().name());
        vo.setPayload(created.payload());
        return vo;
    }

    /** 支付单状态查询（归属校验）。 */
    public PayOrderVO detail(String payNo) {
        return PayOrderVO.from(requireOwned(payNo));
    }

    /**
     * 主动同步渠道状态：本地无公网收不到 webhook 的兜底，前端支付完成回跳后调用。
     * 渠道侧 SUCCESS → 统一入账（eventId=SYNC-{payNo}，与 webhook 幂等互斥）。
     */
    public PayOrderVO sync(String payNo) {
        PayOrder po = requireOwned(payNo);
        if (PayStatus.SUCCESS.equals(po.getStatus()) || PayStatus.FAILED.equals(po.getStatus())
                || PayStatus.CLOSED.equals(po.getStatus())) {
            return PayOrderVO.from(po);
        }
        PayChannel channel = channelRegistry.byCode(po.getChannel());
        ChannelQueryResult query = channel.query(po);
        if (query.status() == ChannelStatus.SUCCESS) {
            settle(po.getChannel(), "SYNC-" + payNo, payNo, true, "{\"source\":\"sync\"}");
        } else if (query.status() == ChannelStatus.FAILED) {
            settle(po.getChannel(), "SYNC-" + payNo, payNo, false, "{\"source\":\"sync\"}");
        }
        return PayOrderVO.from(byPayNo(payNo));
    }

    /**
     * 统一幂等入账。幂等三层：
     * 1) Redis SETNX shop:pay:notify:{channel}:{eventId} TTL 24h；
     * 2) pay_notify_log uk(channel,event_id) insert，冲突即已处理静默返回；
     * 3) pay_order CAS：status IN (CREATED,PENDING) → SUCCESS/FAILED。
     * SUCCESS 后通知订单服务；RPC 异常或返回 false 落 local_message 由 Job 重试兜底。
     */
    public void settle(String channel, String eventId, String payNo, boolean success, String rawPayload) {
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeys.payNotify(channel, eventId), "1", Duration.ofHours(24));
        if (!Boolean.TRUE.equals(first)) {
            log.info("[pay] settle duplicated by redis, channel={}, eventId={}", channel, eventId);
            return;
        }

        PayOrder po = byPayNo(payNo);
        PayNotifyLog notifyLog = new PayNotifyLog();
        notifyLog.setChannel(channel);
        notifyLog.setEventId(eventId);
        notifyLog.setPayload(rawPayload);
        notifyLog.setResult(po == null ? "IGNORED" : "OK");
        try {
            payNotifyLogMapper.insert(notifyLog);
        } catch (DuplicateKeyException e) {
            log.info("[pay] settle duplicated by db, channel={}, eventId={}", channel, eventId);
            return;
        }
        if (po == null) {
            log.warn("[pay] settle but pay order missing, payNo={}, eventId={}", payNo, eventId);
            return;
        }

        LambdaUpdateWrapper<PayOrder> cas = Wrappers.<PayOrder>lambdaUpdate()
                .eq(PayOrder::getPayNo, payNo)
                .in(PayOrder::getStatus, PayStatus.CREATED, PayStatus.PENDING)
                .set(PayOrder::getStatus, success ? PayStatus.SUCCESS : PayStatus.FAILED)
                .setSql("version = version + 1");
        if (success) {
            cas.set(PayOrder::getPaidAt, LocalDateTime.now());
        }
        if (payOrderMapper.update(null, cas) == 0) {
            log.info("[pay] settle skipped, pay order already final, payNo={}, status={}", payNo, po.getStatus());
            return;
        }
        log.info("[pay] settled, payNo={}, success={}, channel={}, eventId={}", payNo, success, channel, eventId);
        if (success) {
            notifyOrderPaid(po);
        }
    }

    /** 通知订单服务支付成功；失败落本地消息表（uk 冲突静默）由 Job 重试。 */
    private void notifyOrderPaid(PayOrder po) {
        boolean ok = false;
        try {
            ok = orderFacade.markPaid(po.getOrderNo(), po.getPayNo(), po.getChannel());
        } catch (Exception e) {
            log.error("[pay] markPaid rpc failed, payNo={}", po.getPayNo(), e);
        }
        if (ok) {
            return;
        }
        log.warn("[pay] markPaid not ok, fallback to local message, payNo={}", po.getPayNo());
        LocalMessage message = new LocalMessage();
        message.setBizType(BIZ_ORDER_MARK_PAID);
        message.setBizKey(po.getPayNo());
        message.setPayload(toJson(Map.of(
                "orderNo", po.getOrderNo(),
                "payNo", po.getPayNo(),
                "channel", po.getChannel())));
        message.setStatus(0);
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        try {
            localMessageMapper.insert(message);
        } catch (DuplicateKeyException e) {
            log.info("[pay] local message exists, payNo={}", po.getPayNo());
        }
    }

    public PayOrder byPayNo(String payNo) {
        return payOrderMapper.selectOne(Wrappers.<PayOrder>lambdaQuery().eq(PayOrder::getPayNo, payNo));
    }

    /** 取支付单并校验归属（登录用户）。 */
    private PayOrder requireOwned(String payNo) {
        long userId = LoginContext.requireUserId();
        PayOrder po = byPayNo(payNo);
        if (po == null) {
            throw new BizException(ErrorCode.PAY_ORDER_NOT_FOUND);
        }
        if (!Long.valueOf(userId).equals(po.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return po;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("json serialize failed", e);
        }
    }
}
