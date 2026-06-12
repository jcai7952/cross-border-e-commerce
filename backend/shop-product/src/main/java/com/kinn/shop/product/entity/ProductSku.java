package com.kinn.shop.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SKU（颜色×尺码）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_sku")
public class ProductSku extends BaseEntity {

    private Long productId;

    private String skuCode;

    /** 颜色（英文） */
    private String color;

    /** 颜色（中文） */
    private String colorZh;

    private String size;

    /** USD 分 */
    private Long priceCents;

    private Integer stock;

    /** 色卡图 MinIO 对象 key */
    private String image;

    private Integer weightGrams;

    /** 乐观锁版本号（M1 不启用 @Version） */
    private Integer version;

    private Integer status;
}
