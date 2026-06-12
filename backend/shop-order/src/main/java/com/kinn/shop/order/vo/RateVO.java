package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "币种与汇率（base 恒为 USD）")
public class RateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String currency;

    private String symbol;

    private int decimalDigits;

    @Schema(description = "1 USD = rate 目标币")
    private BigDecimal rate;
}
