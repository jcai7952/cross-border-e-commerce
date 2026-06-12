package com.kinn.shop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "结算预览入参（items 直传与 fromCart 二选一）")
public class CheckoutPreviewDTO {

    @NotNull
    @Schema(description = "收货地址 id")
    private Long addressId;

    @Valid
    @Schema(description = "立即购买明细，与 fromCart 二选一")
    private List<CheckoutItemDTO> items;

    @Schema(description = "true 取购物车勾选项结算")
    private Boolean fromCart;

    @Schema(description = "我的券 id（user_coupon.id），可空")
    private Long userCouponId;

    @Schema(description = "展示币种", defaultValue = "USD")
    private String currency = "USD";

    @Schema(description = "语言", defaultValue = "en-US")
    private String locale = "en-US";
}
