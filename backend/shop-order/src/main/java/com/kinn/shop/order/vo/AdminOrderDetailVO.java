package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "订单详情（管理端全字段，金额 USD 分）")
public class AdminOrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private Long userId;

    private String status;

    private String tradeMode;

    private String locale;

    @Schema(description = "商品金额 USD 分")
    private Long goodsAmountCents;

    @Schema(description = "券抵扣 USD 分")
    private Long discountAmountCents;

    @Schema(description = "运费 USD 分")
    private Long shippingAmountCents;

    @Schema(description = "税费 USD 分")
    private Long taxAmountCents;

    @Schema(description = "应付 USD 分")
    private Long totalAmountCents;

    private String payCurrency;

    @Schema(description = "下单锁定汇率 USD->支付币")
    private BigDecimal exchangeRate;

    @Schema(description = "支付币最小单位金额")
    private Long payAmountCents;

    private Long userCouponId;

    @Schema(description = "收货地址快照")
    private Map<String, Object> receiver;

    @Schema(description = "清关实名快照")
    private Map<String, Object> identity;

    private String remark;

    private LocalDateTime payDeadline;

    private LocalDateTime paidAt;

    private LocalDateTime shippedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime closedAt;

    private LocalDateTime createTime;

    private List<OrderItemVO> items;

    @Schema(description = "状态流转时间线")
    private List<StatusLogVO> logs;
}
