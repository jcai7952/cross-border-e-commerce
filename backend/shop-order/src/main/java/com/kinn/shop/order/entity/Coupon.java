package com.kinn.shop.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 优惠券模板。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon")
public class Coupon extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String title;

    /** FIXED 满减 / PERCENT 折扣 */
    private String type;

    /** FIXED=USD分 PERCENT=off百分比 */
    @TableField("`value`")
    private Long value;

    /** 门槛（商品金额 USD 分） */
    private Long minAmountCents;

    /** 发行总量，0 不限量 */
    private Integer totalCount;

    private Integer receivedCount;

    /** 每人限领张数 */
    private Integer perUserLimit;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    /** 1启用 0停用 */
    private Integer status;
}
