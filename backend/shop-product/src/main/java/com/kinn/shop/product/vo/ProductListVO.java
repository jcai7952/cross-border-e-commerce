package com.kinn.shop.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品列表卡片。
 */
@Data
@Schema(description = "商品列表项")
public class ProductListVO implements Serializable {

    private Long id;

    @Schema(description = "按 locale 解析后的商品名")
    private String name;

    @Schema(description = "主图完整 URL")
    private String mainImage;

    @Schema(description = "原价（最低 SKU 价）")
    private PriceVO price;

    @Schema(description = "闪购价，未参加闪购为 null")
    private PriceVO flashPrice;

    @Schema(description = "闪购折扣力度（off %），未参加为 null")
    private Integer discountPercent;

    private Integer salesCount;

    private BigDecimal ratingAvg;

    private Integer ratingCount;
}
