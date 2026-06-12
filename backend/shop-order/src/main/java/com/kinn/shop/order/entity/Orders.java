package com.kinn.shop.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表（表名 orders，避开 SQL 关键字 order）。
 * 金额一律 USD 分；支付币金额按下单时锁定汇率换算。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Orders extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private Long userId;

    /** WAIT_PAY/PAID/SHIPPED/FINISHED/CLOSED */
    private String status;

    /** BONDED / DIRECT / MIXED（明细同时含两种贸易模式） */
    private String tradeMode;

    /** 商品金额 USD 分（成交价，含闪购折扣） */
    private Long goodsAmountCents;

    private Long shippingAmountCents;

    private Long taxAmountCents;

    /** 券抵扣 USD 分 */
    private Long discountAmountCents;

    /** 应付 USD 分 = goods - discount + shipping + tax */
    private Long totalAmountCents;

    private String payCurrency;

    /** 下单锁定汇率 USD -> 支付币 */
    private BigDecimal exchangeRate;

    /** 支付币最小单位金额 */
    private Long payAmountCents;

    private Long userCouponId;

    /** 下单语言（明细快照语种） */
    private String locale;

    /** 收货地址快照 JSON */
    private String receiverJson;

    /** 清关实名快照 JSON（realName + idCardMask），不需要实名时为 null */
    private String identityJson;

    private String remark;

    private LocalDateTime payDeadline;

    private LocalDateTime paidAt;

    private LocalDateTime shippedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime closedAt;

    /** 乐观锁版本（状态机 CAS 时 version+1，不用 MP @Version 自动机制） */
    private Integer version;
}
