package com.kinn.shop.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 汇率手工覆盖入参。
 */
@Data
@Schema(description = "汇率手工覆盖入参")
public class RateUpdateDTO {

    @NotBlank
    @Schema(description = "目标币种代码，如 CNY")
    private String quote;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Schema(description = "1 USD = rate 目标币")
    private BigDecimal rate;
}
