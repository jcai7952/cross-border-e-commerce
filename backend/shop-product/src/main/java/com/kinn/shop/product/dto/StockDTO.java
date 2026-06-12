package com.kinn.shop.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * SKU 库存调整入参。
 */
@Data
@Schema(description = "库存入参")
public class StockDTO {

    @NotNull
    @Min(0)
    @Schema(description = "库存数量")
    private Integer stock;
}
