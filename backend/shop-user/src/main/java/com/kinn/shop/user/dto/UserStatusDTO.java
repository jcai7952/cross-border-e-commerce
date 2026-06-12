package com.kinn.shop.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "修改用户状态")
public class UserStatusDTO {

    @NotNull
    @Min(0)
    @Max(1)
    @Schema(description = "1正常 0禁用")
    private Integer status;
}
