package com.kinn.shop.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 上下架入参。
 */
@Data
@Schema(description = "状态入参")
public class StatusDTO {

    @NotNull
    @Min(0)
    @Max(1)
    @Schema(description = "1 上架 0 下架")
    private Integer status;
}
