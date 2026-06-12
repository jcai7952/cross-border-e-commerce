package com.kinn.shop.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 购物车条目（user_id + sku_id 唯一）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cart_item")
public class CartItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Long productId;

    private Long skuId;

    private Integer quantity;

    /** 1勾选 0未勾选 */
    private Integer checked;
}
