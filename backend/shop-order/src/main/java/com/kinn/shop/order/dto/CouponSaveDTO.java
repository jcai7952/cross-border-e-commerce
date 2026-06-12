package com.kinn.shop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建优惠券入参（管理端）")
public class CouponSaveDTO {

    @NotBlank
    @Size(max = 64)
    private String title;

    @NotBlank
    @Pattern(regexp = "FIXED|PERCENT", message = "必须为 FIXED 或 PERCENT")
    @Schema(description = "FIXED 满减 / PERCENT 折扣")
    private String type;

    @NotNull
    @Min(1)
    @Schema(description = "FIXED=USD分 PERCENT=off百分比(1~99)")
    private Long value;

    @NotNull
    @Min(0)
    @Schema(description = "门槛（商品金额 USD 分），0 无门槛")
    private Long minAmountCents;

    @NotNull
    @Min(0)
    @Schema(description = "发行总量，0 不限量")
    private Integer totalCount;

    @NotNull
    @Min(1)
    @Schema(description = "每人限领张数")
    private Integer perUserLimit;

    @NotNull
    private LocalDateTime validFrom;

    @NotNull
    private LocalDateTime validTo;
}
