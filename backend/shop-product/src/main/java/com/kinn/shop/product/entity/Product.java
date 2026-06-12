package com.kinn.shop.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品 SPU。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {

    /** 叶子类目 id */
    private Long categoryId;

    private String spuCode;

    private String brand;

    /** BONDED 保税仓 / DIRECT 海外直邮 */
    private String tradeMode;

    /** 发货地国家二位码 */
    private String originCountry;

    /** 主图 MinIO 对象 key */
    private String mainImage;

    /** 冗余：最低 SKU 价（USD 分） */
    private Long minPriceCents;

    private Integer salesCount;

    private BigDecimal ratingAvg;

    private Integer ratingCount;

    /** 1 上架 0 下架 */
    private Integer status;
}
