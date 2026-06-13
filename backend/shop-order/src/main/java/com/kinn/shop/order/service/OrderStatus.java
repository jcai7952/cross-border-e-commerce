package com.kinn.shop.order.service;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态机。
 * 转移表：WAIT_PAY→PAID（支付回调，M3）、WAIT_PAY→CLOSED（取消/超时）、
 * PAID→CLOSED（全额退款关单，M3）、PAID→SHIPPED（发货，M4）、
 * SHIPPED→FINISHED（确认收货/自动完成）。
 */
public enum OrderStatus {

    WAIT_PAY, PAID, SHIPPED, FINISHED, CLOSED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            WAIT_PAY, Set.of(PAID, CLOSED),
            PAID, Set.of(SHIPPED, CLOSED),
            SHIPPED, Set.of(FINISHED)
    );

    public static boolean canTransit(OrderStatus from, OrderStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
