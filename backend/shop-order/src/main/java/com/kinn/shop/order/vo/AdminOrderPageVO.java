package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "订单分页行（管理端，金额 USD 分）")
public class AdminOrderPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private Long userId;

    private String status;

    private String tradeMode;

    @Schema(description = "商品金额 USD 分")
    private Long goodsAmountCents;

    @Schema(description = "应付 USD 分")
    private Long totalAmountCents;

    private String payCurrency;

    @Schema(description = "支付币最小单位金额")
    private Long payAmountCents;

    private LocalDateTime payDeadline;

    private LocalDateTime createTime;

    @Schema(description = "明细摘要")
    private List<OrderItemVO> items;
}
