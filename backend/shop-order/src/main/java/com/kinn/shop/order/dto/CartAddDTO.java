package com.kinn.shop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "加入购物车入参")
public class CartAddDTO {

    @NotNull
    @Schema(description = "SKU id")
    private Long skuId;

    @NotNull
    @Min(1)
    @Max(99)
    @Schema(description = "数量 1~99")
    private Integer quantity;
}
