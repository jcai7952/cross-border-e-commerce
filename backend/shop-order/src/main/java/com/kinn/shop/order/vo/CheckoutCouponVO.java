package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "结算可用券（我的未用券中满足门槛的）")
public class CheckoutCouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "user_coupon id（下单时传 userCouponId）")
    private Long userCouponId;

    private Long couponId;

    private String title;

    @Schema(description = "FIXED 满减 / PERCENT 折扣")
    private String type;

    @Schema(description = "FIXED=USD分 PERCENT=off百分比")
    private Long value;

    @Schema(description = "门槛（商品金额 USD 分）")
    private Long minAmountCents;

    private LocalDateTime validTo;

    @Schema(description = "对本单可抵扣金额 USD 分")
    private long discountCents;
}
