package com.kinn.shop.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 支付单。金额一律支付币最小单位（订单下单时已锁汇率，支付侧不换算）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pay_order")
public class PayOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String payNo;

    private String orderNo;

    private Long userId;

    /** STRIPE/PAYPAL/SIMULATOR */
    private String channel;

    /** 支付币最小单位 */
    private Long amountCents;

    private String currency;

    /** CREATED/PENDING/SUCCESS/FAILED/CLOSED */
    private String status;

    /** 渠道单号 pi_xxx / PayPal order id */
    private String channelTradeNo;

    /** 渠道响应快照（client_secret/approve链接/capture响应等） */
    private String channelPayload;

    /** 乐观锁版本（状态 CAS 时 version+1，不用 MP @Version 自动机制） */
    private Integer version;

    private LocalDateTime paidAt;
}
