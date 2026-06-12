package com.kinn.shop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "优惠券启停入参")
public class CouponStatusDTO {

    @NotNull
    @Min(0)
    @Max(1)
    @Schema(description = "1启用 0停用")
    private Integer status;
}
