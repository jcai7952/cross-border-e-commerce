package com.kinn.shop.api.product.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 币种与汇率（base 恒为 USD）。 */
@Data
public class RateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String currency;
    private String symbol;
    private int decimalDigits;
    /** 1 USD = rate 目标币 */
    private BigDecimal rate;
}
