package com.kinn.shop.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "发起支付入参")
public class PayCreateDTO {

    @NotBlank
    @Schema(description = "订单号")
    private String orderNo;

    @NotBlank
    @Schema(description = "支付渠道 SIMULATOR/STRIPE/PAYPAL")
    private String channel;
}
