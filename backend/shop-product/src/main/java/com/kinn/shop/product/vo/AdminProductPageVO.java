package com.kinn.shop.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端商品分页项（含两种语言名称）。
 */
@Data
@Schema(description = "管理端商品分页项")
public class AdminProductPageVO implements Serializable {

    private Long id;

    private Long categoryId;

    private String spuCode;

    private String brand;

    private String tradeMode;

    private String nameZh;

    private String nameEn;

    @Schema(description = "主图对象 key")
    private String mainImage;

    @Schema(description = "主图完整 URL")
    private String mainImageUrl;

    @Schema(description = "最低 SKU 价（USD 分）")
    private Long minPriceCents;

    private Integer salesCount;

    private BigDecimal ratingAvg;

    private Integer ratingCount;

    private Integer status;

    private LocalDateTime createTime;
}
