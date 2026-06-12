package com.kinn.shop.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.order.entity.OrderItem;
import com.kinn.shop.order.entity.OrderStatusLog;
import com.kinn.shop.order.entity.Orders;
import com.kinn.shop.order.mapper.OrderItemMapper;
import com.kinn.shop.order.mapper.OrderStatusLogMapper;
import com.kinn.shop.order.mapper.OrdersMapper;
import com.kinn.shop.order.vo.AdminOrderDetailVO;
import com.kinn.shop.order.vo.AdminOrderPageVO;
import com.kinn.shop.order.vo.OrderItemVO;
import com.kinn.shop.order.vo.StatusLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单查询（管理端，金额展示 USD 分原值）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final ObjectMapper objectMapper;

    public PageResult<AdminOrderPageVO> page(String status, String orderNo, long pageNum, long pageSize) {
        LoginContext.requireAdminId();
        long pn = Math.max(pageNum, 1);
        long ps = Math.min(Math.max(pageSize, 1), 100);
        Page<Orders> page = ordersMapper.selectPage(new Page<>(pn, ps), Wrappers.<Orders>lambdaQuery()
                .eq(status != null && !status.isBlank(), Orders::getStatus, status)
                .eq(orderNo != null && !orderNo.isBlank(), Orders::getOrderNo, orderNo)
                .orderByDesc(Orders::getId));
        if (page.getRecords().isEmpty()) {
            return PageResult.of(page.getTotal(), pn, ps, List.of());
        }
        Map<String, List<OrderItem>> itemMap = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .in(OrderItem::getOrderNo, page.getRecords().stream().map(Orders::getOrderNo).toList())
                        .orderByAsc(OrderItem::getId))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderNo));
        List<AdminOrderPageVO> list = page.getRecords().stream().map(order -> {
            AdminOrderPageVO vo = new AdminOrderPageVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setUserId(order.getUserId());
            vo.setStatus(order.getStatus());
            vo.setTradeMode(order.getTradeMode());
            vo.setGoodsAmountCents(order.getGoodsAmountCents());
            vo.setTotalAmountCents(order.getTotalAmountCents());
            vo.setPayCurrency(order.getPayCurrency());
            vo.setPayAmountCents(order.getPayAmountCents());
            vo.setPayDeadline(order.getPayDeadline());
            vo.setCreateTime(order.getCreateTime());
            vo.setItems(itemMap.getOrDefault(order.getOrderNo(), List.of())
                    .stream().map(OrderItemVO::from).toList());
            return vo;
        }).toList();
        return PageResult.of(page.getTotal(), pn, ps, list);
    }

    public AdminOrderDetailVO detail(String orderNo) {
        LoginContext.requireAdminId();
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery().eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        AdminOrderDetailVO vo = new AdminOrderDetailVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setStatus(order.getStatus());
        vo.setTradeMode(order.getTradeMode());
        vo.setLocale(order.getLocale());
        vo.setGoodsAmountCents(order.getGoodsAmountCents());
        vo.setDiscountAmountCents(order.getDiscountAmountCents());
        vo.setShippingAmountCents(order.getShippingAmountCents());
        vo.setTaxAmountCents(order.getTaxAmountCents());
        vo.setTotalAmountCents(order.getTotalAmountCents());
        vo.setPayCurrency(order.getPayCurrency());
        vo.setExchangeRate(order.getExchangeRate());
        vo.setPayAmountCents(order.getPayAmountCents());
        vo.setUserCouponId(order.getUserCouponId());
        vo.setReceiver(fromJson(order.getReceiverJson()));
        vo.setIdentity(fromJson(order.getIdentityJson()));
        vo.setRemark(order.getRemark());
        vo.setPayDeadline(order.getPayDeadline());
        vo.setPaidAt(order.getPaidAt());
        vo.setShippedAt(order.getShippedAt());
        vo.setFinishedAt(order.getFinishedAt());
        vo.setClosedAt(order.getClosedAt());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderNo, orderNo).orderByAsc(OrderItem::getId))
                .stream().map(OrderItemVO::from).toList());
        vo.setLogs(statusLogMapper.selectList(Wrappers.<OrderStatusLog>lambdaQuery()
                        .eq(OrderStatusLog::getOrderNo, orderNo).orderByAsc(OrderStatusLog::getId))
                .stream().map(StatusLogVO::from).toList());
        return vo;
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("[admin-order] snapshot json parse failed: {}", e.getMessage());
            return null;
        }
    }
}
