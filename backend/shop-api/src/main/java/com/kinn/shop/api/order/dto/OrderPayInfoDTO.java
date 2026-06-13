package com.kinn.shop.api.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 支付视角的订单信息。 */
@Data
public class OrderPayInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private Long userId;
    /** WAIT_PAY/PAID/SHIPPED/FINISHED/CLOSED */
    private String status;
    /** 支付币种（下单锁定） */
    private String payCurrency;
    /** 支付币最小单位金额（下单锁汇率算定） */
    private Long payAmountMinor;
    private LocalDateTime payDeadline;
}
