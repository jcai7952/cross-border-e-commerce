package com.kinn.shop.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品多语言（表无 create_time/update_time，不继承 BaseEntity）。
 */
@Data
@TableName("product_i18n")
public class ProductI18n implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** zh-CN / en-US */
    private String locale;

    private String name;

    private String subtitle;

    private String detail;
}
