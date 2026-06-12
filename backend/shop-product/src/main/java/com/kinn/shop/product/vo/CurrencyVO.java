package com.kinn.shop.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 可用币种。
 */
@Data
@Schema(description = "币种")
public class CurrencyVO implements Serializable {

    private String code;

    private String symbol;

    private String nameZh;

    private String nameEn;

    private Integer decimalDigits;
}
