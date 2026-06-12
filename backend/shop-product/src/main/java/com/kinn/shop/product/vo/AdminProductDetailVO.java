package com.kinn.shop.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端商品完整编辑视图。
 */
@Data
@Schema(description = "管理端商品编辑视图")
public class AdminProductDetailVO implements Serializable {

    private Long id;

    private Long categoryId;

    private String spuCode;

    private String brand;

    private String tradeMode;

    private String originCountry;

    @Schema(description = "主图对象 key")
    private String mainImage;

    @Schema(description = "主图完整 URL")
    private String mainImageUrl;

    private Long minPriceCents;

    private Integer salesCount;

    private BigDecimal ratingAvg;

    private Integer ratingCount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<I18nVO> i18ns;

    private List<ImageVO> images;

    private List<SkuVO> skus;

    @Data
    @Schema(description = "商品多语言")
    public static class I18nVO implements Serializable {

        private String locale;

        private String name;

        private String subtitle;

        private String detail;
    }

    @Data
    @Schema(description = "图册项")
    public static class ImageVO implements Serializable {

        private Long id;

        @Schema(description = "对象 key")
        private String key;

        @Schema(description = "完整 URL")
        private String url;

        private Integer sort;
    }

    @Data
    @Schema(description = "管理端 SKU（真实库存）")
    public static class SkuVO implements Serializable {

        private Long id;

        private String skuCode;

        private String color;

        private String colorZh;

        private String size;

        @Schema(description = "USD 分")
        private Long priceCents;

        @Schema(description = "真实库存")
        private Integer stock;

        @Schema(description = "色卡图对象 key")
        private String image;

        @Schema(description = "色卡图完整 URL")
        private String imageUrl;

        private Integer weightGrams;

        private Integer status;
    }
}
