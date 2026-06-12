package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "订单详情")
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    @Schema(description = "WAIT_PAY/PAID/SHIPPED/FINISHED/CLOSED")
    private String status;

    @Schema(description = "BONDED / DIRECT / MIXED")
    private String tradeMode;

    @Schema(description = "下单语言（快照语种）")
    private String locale;

    @Schema(description = "商品金额（USD 分 + 支付币显示）")
    private MoneyVO goods;

    @Schema(description = "券抵扣")
    private MoneyVO discount;

    @Schema(description = "运费")
    private MoneyVO shipping;

    @Schema(description = "税费")
    private MoneyVO tax;

    @Schema(description = "应付合计")
    private MoneyVO total;

    private String payCurrency;

    @Schema(description = "下单锁定汇率 USD->支付币")
    private BigDecimal exchangeRate;

    @Schema(description = "支付币最小单位金额")
    private Long payAmountCents;

    private Long userCouponId;

    @Schema(description = "收货地址快照")
    private Map<String, Object> receiver;

    @Schema(description = "清关实名快照（realName + idCardMask），无需实名为 null")
    private Map<String, Object> identity;

    private String remark;

    private LocalDateTime payDeadline;

    @Schema(description = "WAIT_PAY 剩余支付秒数，其它状态为 null")
    private Long countdownSeconds;

    private LocalDateTime paidAt;

    private LocalDateTime shippedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime closedAt;

    private LocalDateTime createTime;

    private List<OrderItemVO> items;

    @Schema(description = "状态流转时间线")
    private List<StatusLogVO> logs;
}
