package com.kinn.shop.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改密码")
public class PasswordUpdateDTO {

    @NotBlank
    @Schema(description = "原密码")
    private String oldPassword;

    @NotBlank
    @Size(min = 6, max = 32)
    @Schema(description = "新密码 6-32 位")
    private String newPassword;
}
