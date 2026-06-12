package com.kinn.shop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "购物车全选/全不选入参")
public class CartCheckAllDTO {

    @NotNull
    @Schema(description = "true 全选 / false 全不选")
    private Boolean checked;
}
