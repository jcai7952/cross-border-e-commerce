package com.kinn.shop.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "模拟渠道回调入参（收银台页面按钮触发）")
public class SimulatorNotifyDTO {

    @NotBlank
    private String payNo;

    @NotBlank
    @Pattern(regexp = "SUCCESS|FAILED")
    @Schema(description = "SUCCESS/FAILED")
    private String result;

    @NotBlank
    @Schema(description = "幂等事件 id，约定 SIMEVT-{payNo}-{result}")
    private String eventId;
}
