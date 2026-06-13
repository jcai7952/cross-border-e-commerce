package com.kinn.shop.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 闪购活动创建/全量更新入参。
 */
@Data
@Schema(description = "闪购活动入参")
public class FlashSaleSaveDTO {

    @NotBlank
    @Size(max = 64)
    private String title;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @NotEmpty
    @Valid
    private List<ItemDTO> items;

    @Data
    @Schema(description = "闪购商品入参")
    public static class ItemDTO {

        @NotNull
        private Long productId;

        @NotNull
        @Min(1)
        @Max(90)
        @Schema(description = "折扣力度：30 = off 30%（七折）")
        private Integer discountPercent;

        @Min(0)
        @Schema(description = "限量，0/缺省不限量")
        private Integer quota;
    }
}
