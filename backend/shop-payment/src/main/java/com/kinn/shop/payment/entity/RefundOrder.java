package com.kinn.shop.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 退款单（管理端发起，金额支付币最小单位）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("refund_order")
public class RefundOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String refundNo;

    private String payNo;

    private String orderNo;

    private Long userId;

    /** 支付币最小单位 */
    private Long amountCents;

    private String currency;

    /** PROCESSING/SUCCESS/FAILED */
    private String status;

    private String channelRefundNo;

    private String reason;

    private LocalDateTime refundedAt;
}
