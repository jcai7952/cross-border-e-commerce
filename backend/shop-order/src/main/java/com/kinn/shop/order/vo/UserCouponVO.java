package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "我的优惠券")
public class UserCouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "user_coupon id（下单时传 userCouponId）")
    private Long id;

    private Long couponId;

    private String title;

    @Schema(description = "FIXED 满减 / PERCENT 折扣")
    private String type;

    @Schema(description = "FIXED=USD分 PERCENT=off百分比")
    private Long value;

    @Schema(description = "门槛（商品金额 USD 分）")
    private Long minAmountCents;

    @Schema(description = "0未用 1已用 2过期")
    private Integer status;

    @Schema(description = "核销订单号")
    private String orderNo;

    private LocalDateTime receivedAt;

    private LocalDateTime usedAt;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;
}
