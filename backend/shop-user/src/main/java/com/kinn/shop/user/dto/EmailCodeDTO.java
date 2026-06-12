package com.kinn.shop.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "发送邮箱验证码")
public class EmailCodeDTO {

    @NotBlank
    @Email
    @Schema(description = "邮箱")
    private String email;

    @NotBlank
    @Pattern(regexp = "register|reset", message = "scene 仅支持 register/reset")
    @Schema(description = "场景 register|reset")
    private String scene;
}
