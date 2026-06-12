package com.kinn.shop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "结算明细行（立即购买直传）")
public class CheckoutItemDTO {

    @NotNull
    private Long skuId;

    @NotNull
    @Min(1)
    @Max(99)
    private Integer quantity;
}
