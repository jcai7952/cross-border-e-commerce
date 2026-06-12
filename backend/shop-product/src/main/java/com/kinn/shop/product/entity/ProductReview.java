package com.kinn.shop.product.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论晒图（表无 update_time，不继承 BaseEntity）。
 */
@Data
@TableName("product_review")
public class ProductReview implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 已购验证订单号 */
    private String orderNo;

    private Long userId;

    private String userNickname;

    /** 如 "Black / M" */
    private String skuText;

    private Integer rating;

    private String content;

    /** JSON 数组（MinIO 对象 key） */
    private String images;

    /** 0 待审 1 通过 2 拒绝 */
    private Integer status;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
}
