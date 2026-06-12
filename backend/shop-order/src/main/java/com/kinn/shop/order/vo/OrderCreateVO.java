package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "下单结果")
public class OrderCreateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    @Schema(description = "支付币最小单位金额")
    private long payAmountMinor;

    private String payCurrency;

    @Schema(description = "支付截止时间（30 分钟）")
    private LocalDateTime payDeadline;
}
