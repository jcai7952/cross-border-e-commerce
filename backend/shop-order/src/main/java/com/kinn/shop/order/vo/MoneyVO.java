package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 金额出参：USD 分原值 + 目标币展示。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "金额（USD 分 + 展示币）")
public class MoneyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "USD 分")
    private long usdCents;

    @Schema(description = "展示币价格")
    private PriceVO display;
}
