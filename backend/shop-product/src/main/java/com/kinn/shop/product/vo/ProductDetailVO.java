package com.kinn.shop.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情。
 */
@Data
@Schema(description = "商品详情")
public class ProductDetailVO implements Serializable {

    private Long id;

    private Long categoryId;

    @Schema(description = "按 locale 解析后的商品名")
    private String name;

    private String subtitle;

    @Schema(description = "详情 HTML")
    private String detail;

    private String brand;

    @Schema(description = "BONDED 保税仓 / DIRECT 海外直邮")
    private String tradeMode;

    private String originCountry;

    @Schema(description = "图册完整 URL")
    private List<String> images;

    private List<SkuVO> skus;

    @Schema(description = "尺码表 JSON（按 locale，缺省 null）")
    private Object sizeChart;

    private BigDecimal ratingAvg;

    private Integer ratingCount;

    private Integer salesCount;

    @Schema(description = "当前登录用户是否已收藏（游客恒为 false）")
    private boolean favorite;

    @Schema(description = "闪购信息，未参加为 null")
    private FlashVO flash;

    @Data
    @Schema(description = "SKU 出参")
    public static class SkuVO implements Serializable {

        private Long id;

        private String color;

        private String colorZh;

        private String size;

        private PriceVO price;

        @Schema(description = "闪购价，未参加为 null")
        private PriceVO flashPrice;

        @Schema(description = "展示库存 min(stock,99)")
        private int stock;

        @Schema(description = "色卡图完整 URL")
        private String image;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "闪购信息")
    public static class FlashVO implements Serializable {

        @Schema(description = "折扣力度（off %）")
        private Integer discountPercent;

        private LocalDateTime endTime;
    }
}
