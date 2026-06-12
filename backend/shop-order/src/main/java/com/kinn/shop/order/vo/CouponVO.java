package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "可领优惠券")
public class CouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    @Schema(description = "FIXED 满减 / PERCENT 折扣")
    private String type;

    @Schema(description = "FIXED=USD分 PERCENT=off百分比")
    private Long value;

    @Schema(description = "门槛（商品金额 USD 分），0 无门槛")
    private Long minAmountCents;

    @Schema(description = "发行总量，0 不限量")
    private Integer totalCount;

    private Integer receivedCount;

    private Integer perUserLimit;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    @Schema(description = "当前用户已领张数")
    private long claimedCount;

    @Schema(description = "当前用户是否可再领")
    private boolean canClaim;
}
