package com.kinn.shop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "下单入参（金额全部服务端重算，不信任前端）")
public class OrderCreateDTO {

    @NotNull
    @Schema(description = "收货地址 id")
    private Long addressId;

    @Valid
    @Schema(description = "立即购买明细，与 fromCart 二选一")
    private List<CheckoutItemDTO> items;

    @Schema(description = "true 取购物车勾选项下单")
    private Boolean fromCart;

    @Schema(description = "我的券 id（user_coupon.id），可空")
    private Long userCouponId;

    @Schema(description = "清关实名记录 id；目的国要求实名时必须可解析（null 取默认实名）")
    private Long identityId;

    @Schema(description = "支付币种", defaultValue = "USD")
    private String payCurrency = "USD";

    @Schema(description = "下单语言（明细快照语种）", defaultValue = "en-US")
    private String locale = "en-US";

    @Size(max = 255)
    private String remark;
}
