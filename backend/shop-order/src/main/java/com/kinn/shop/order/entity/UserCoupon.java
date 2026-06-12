package com.kinn.shop.order.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户持券（表无 create_time/update_time，不继承 BaseEntity；
 * received_at 由 DB DEFAULT CURRENT_TIMESTAMP 维护，插入不写）。
 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long couponId;

    private Long userId;

    /** 0未用 1已用 2过期 */
    private Integer status;

    /** 核销订单号 */
    private String orderNo;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime receivedAt;

    private LocalDateTime usedAt;
}
