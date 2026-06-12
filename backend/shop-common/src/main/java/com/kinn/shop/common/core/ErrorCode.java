package com.kinn.shop.common.core;

import lombok.Getter;

/**
 * 全局错误码。0 成功；4xx 客户端；5xx 系统；1xxxx 业务（按域分段）。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后再试"),

    // 1xxxx 用户域
    USER_NOT_FOUND(10001, "用户不存在"),
    USER_DISABLED(10002, "账号已被禁用"),
    EMAIL_EXISTS(10003, "邮箱已注册"),
    EMAIL_CODE_ERROR(10004, "验证码错误或已过期"),
    PASSWORD_ERROR(10005, "邮箱或密码错误"),
    IDENTITY_INVALID(10006, "身份证号校验不通过"),

    // 2xxxx 商品域
    PRODUCT_NOT_FOUND(20001, "商品不存在或已下架"),
    SKU_NOT_FOUND(20002, "商品规格不存在"),
    STOCK_NOT_ENOUGH(20003, "库存不足"),
    CURRENCY_NOT_SUPPORTED(20004, "不支持的币种"),

    // 3xxxx 交易域
    CART_ITEM_NOT_FOUND(30001, "购物车条目不存在"),
    ORDER_NOT_FOUND(30002, "订单不存在"),
    ORDER_STATUS_ILLEGAL(30003, "订单状态不允许该操作"),
    COUPON_NOT_AVAILABLE(30004, "优惠券不可用"),
    IDENTITY_REQUIRED(30005, "该目的地清关需要订购人实名信息"),

    // 4xxxx 支付域
    PAY_ORDER_NOT_FOUND(40001, "支付单不存在"),
    PAY_CHANNEL_UNAVAILABLE(40002, "支付渠道暂不可用"),
    PAY_STATUS_ILLEGAL(40003, "支付单状态不允许该操作"),
    REFUND_EXCEED(40004, "退款金额超过可退余额"),

    // 5xxxx 物流域
    SHIPMENT_NOT_FOUND(50001, "物流单不存在"),
    SHIPPING_ZONE_NOT_COVERED(50002, "该国家/地区暂不支持配送");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
