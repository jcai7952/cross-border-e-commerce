package com.kinn.shop.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "重置密码")
public class ResetPasswordDTO {

    @NotBlank
    @Email
    @Schema(description = "邮箱")
    private String email;

    @NotBlank
    @Schema(description = "邮箱验证码（reset 场景）")
    private String code;

    @NotBlank
    @Size(min = 6, max = 32)
    @Schema(description = "新密码 6-32 位")
    private String newPassword;
}
