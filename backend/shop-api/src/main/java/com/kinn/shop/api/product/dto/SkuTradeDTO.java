package com.kinn.shop.api.product.dto;

import lombok.Data;

import java.io.Serializable;

/** 交易用 SKU 快照。 */
@Data
public class SkuTradeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long skuId;
    private Long productId;
    /** 按请求 locale 的商品名（i18n 回退 en-US）。 */
    private String productName;
    /** 如 "Floral Blue / M" */
    private String skuText;
    /** 对象 key 对应的完整图片 URL */
    private String image;
    /** 成交单价 USD 分（已应用当前闪购折扣） */
    private long priceCents;
    /** 原价 USD 分 */
    private long originalPriceCents;
    /** 闪购折扣（无=null），如 30 表示 off 30% */
    private Integer discountPercent;
    private int stock;
    private int weightGrams;
    /** BONDED / DIRECT */
    private String tradeMode;
    /** 类目行邮税率 13/20/50（中国直邮模式计税用） */
    private int postalTaxRate;
    /** SKU 与商品都上架才为 1 */
    private int status;
}
