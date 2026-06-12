package com.kinn.shop.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 闪购商品（全 SKU 按比例折扣；表无时间列，不继承 BaseEntity）。
 */
@Data
@TableName("flash_sale_item")
public class FlashSaleItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long saleId;

    private Long productId;

    /** 折扣力度：30 = off 30%（七折） */
    private Integer discountPercent;

    /** 0 不限量 */
    private Integer quota;

    private Integer sold;
}
