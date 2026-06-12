package com.kinn.shop.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品类目（两级）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    private Long parentId;

    /** 1 一级 2 二级 */
    private Integer level;

    private String nameZh;

    private String nameEn;

    private String icon;

    private Integer sort;

    /** 直邮行邮税率% 13/20/50 */
    private Integer postalTaxRate;

    private Integer status;
}
