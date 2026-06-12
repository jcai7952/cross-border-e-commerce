package com.kinn.shop.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 类目创建/更新入参。
 */
@Data
@Schema(description = "类目创建/更新入参")
public class CategorySaveDTO {

    @Schema(description = "父类目 id，0 为一级类目")
    private Long parentId = 0L;

    @NotBlank
    @Schema(description = "中文名")
    private String nameZh;

    @NotBlank
    @Schema(description = "英文名")
    private String nameEn;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort = 0;

    @Schema(description = "直邮行邮税率% 13/20/50")
    private Integer postalTaxRate = 20;

    @Schema(description = "状态 1启用 0停用")
    private Integer status = 1;
}
