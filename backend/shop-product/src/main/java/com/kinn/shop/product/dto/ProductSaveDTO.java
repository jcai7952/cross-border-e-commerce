package com.kinn.shop.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 商品创建/全量更新入参：product + i18n(zh/en) + images + skus 一次提交。
 */
@Data
@Schema(description = "商品创建/全量更新入参")
public class ProductSaveDTO {

    @NotNull
    @Schema(description = "叶子类目 id")
    private Long categoryId;

    @NotBlank
    @Schema(description = "SPU 编码")
    private String spuCode;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "BONDED 保税仓 / DIRECT 海外直邮")
    private String tradeMode = "BONDED";

    @Schema(description = "发货地国家二位码")
    private String originCountry = "CN";

    @NotBlank
    @Schema(description = "主图对象 key")
    private String mainImage;

    @Schema(description = "状态 1上架 0下架")
    private Integer status = 1;

    @NotEmpty
    @Valid
    @Schema(description = "多语言（zh-CN / en-US 各一条）")
    private List<I18nDTO> i18ns;

    @Schema(description = "图册对象 key 列表（按顺序存 sort）")
    private List<String> images;

    @NotEmpty
    @Valid
    @Schema(description = "SKU 列表")
    private List<SkuDTO> skus;

    @Data
    @Schema(description = "商品多语言入参")
    public static class I18nDTO {

        @NotBlank
        @Schema(description = "zh-CN / en-US")
        private String locale;

        @NotBlank
        @Schema(description = "商品名")
        private String name;

        @Schema(description = "副标题")
        private String subtitle;

        @Schema(description = "详情 HTML")
        private String detail;
    }

    @Data
    @Schema(description = "SKU 入参")
    public static class SkuDTO {

        @Schema(description = "SKU id（更新时已有 SKU 带 id；不带 id 表示新增）")
        private Long id;

        @NotBlank
        @Schema(description = "SKU 编码")
        private String skuCode;

        @NotBlank
        @Schema(description = "颜色（英文）")
        private String color;

        @NotBlank
        @Schema(description = "颜色（中文）")
        private String colorZh;

        @NotBlank
        @Schema(description = "尺码")
        private String size;

        @NotNull
        @Min(1)
        @Schema(description = "价格（USD 分）")
        private Long priceCents;

        @NotNull
        @Min(0)
        @Schema(description = "库存")
        private Integer stock;

        @Schema(description = "色卡图对象 key")
        private String image;

        @Min(1)
        @Max(50000)
        @Schema(description = "重量（克）")
        private Integer weightGrams = 300;

        @Schema(description = "状态 1启用 0停用")
        private Integer status = 1;
    }
}
