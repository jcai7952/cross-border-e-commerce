package com.kinn.shop.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品图册（url 存 MinIO 对象 key；表无时间列，不继承 BaseEntity）。
 */
@Data
@TableName("product_image")
public class ProductImage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** MinIO 对象 key */
    private String url;

    private Integer sort;
}
