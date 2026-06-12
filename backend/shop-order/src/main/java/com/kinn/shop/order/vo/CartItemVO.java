package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "购物车条目")
public class CartItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车条目 id")
    private Long id;

    private Long skuId;

    private Long productId;

    @Schema(description = "商品名（请求 locale）")
    private String name;

    @Schema(description = "规格文本，如 Black / M")
    private String skuText;

    private String image;

    @Schema(description = "成交单价（含闪购折扣）")
    private PriceVO price;

    @Schema(description = "原价")
    private PriceVO originalPrice;

    @Schema(description = "闪购折扣百分比，无=null")
    private Integer discountPercent;

    private Integer quantity;

    private Boolean checked;

    @Schema(description = "当前库存")
    private Integer stock;

    @Schema(description = "失效（SKU 不存在或已下架）")
    private Boolean invalid;
}
