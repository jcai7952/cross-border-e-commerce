package com.kinn.shop.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "退款入参（管理端）")
public class RefundCreateDTO {

    @NotNull
    @Min(1)
    @Schema(description = "退款金额（支付币最小单位），支持部分退款")
    private Long amountMinor;

    @Size(max = 255)
    @Schema(description = "退款原因")
    private String reason;
}
