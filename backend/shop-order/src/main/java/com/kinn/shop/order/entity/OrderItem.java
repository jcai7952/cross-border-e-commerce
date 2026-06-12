package com.kinn.shop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单明细（下单时点快照；表无时间列，不继承 BaseEntity）。
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long productId;

    private Long skuId;

    /** 下单语言的商品名快照 */
    private String productName;

    /** 如 "Black / M" */
    private String skuText;

    private String image;

    /** 成交单价 USD 分（含闪购） */
    private Long priceCents;

    private Integer quantity;

    private Long totalCents;
}
