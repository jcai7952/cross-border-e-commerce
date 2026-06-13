package com.kinn.shop.payment.service;

/**
 * 支付单状态常量（pay_order.status）。
 */
public final class PayStatus {

    public static final String CREATED = "CREATED";
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String CLOSED = "CLOSED";

    private PayStatus() {
    }
}
