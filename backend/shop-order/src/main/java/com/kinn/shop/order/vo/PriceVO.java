package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 多币种价格出参：USD 分换算到目标币最小单位（与 shop-product 同名同形）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "多币种价格")
public class PriceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "币种代码，如 USD")
    private String currency;

    @Schema(description = "币种符号，如 $")
    private String symbol;

    @Schema(description = "目标币最小单位金额（分/円）")
    private long amountMinor;

    @Schema(description = "展示串，如 25.99")
    private String text;
}
