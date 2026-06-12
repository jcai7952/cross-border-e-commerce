package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "我的订单（分页行）")
public class OrderPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    @Schema(description = "WAIT_PAY/PAID/SHIPPED/FINISHED/CLOSED")
    private String status;

    @Schema(description = "BONDED / DIRECT / MIXED")
    private String tradeMode;

    @Schema(description = "应付 USD 分")
    private Long totalUsdCents;

    @Schema(description = "应付（支付币，按下单锁定汇率）")
    private PriceVO totalDisplay;

    private String payCurrency;

    @Schema(description = "WAIT_PAY 剩余支付秒数，其它状态为 null")
    private Long countdownSeconds;

    private LocalDateTime payDeadline;

    private LocalDateTime createTime;

    @Schema(description = "明细摘要")
    private List<OrderItemVO> items;
}
