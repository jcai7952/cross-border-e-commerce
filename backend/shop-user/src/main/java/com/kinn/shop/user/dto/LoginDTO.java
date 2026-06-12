package com.kinn.shop.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "登录")
public class LoginDTO {

    @NotBlank
    @Email
    @Schema(description = "邮箱")
    private String email;

    @NotBlank
    @Schema(description = "密码")
    private String password;
}
