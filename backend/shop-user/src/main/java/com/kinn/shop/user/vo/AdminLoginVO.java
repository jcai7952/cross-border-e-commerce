package com.kinn.shop.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员登录结果")
public class AdminLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "satoken 令牌")
    private String token;

    private AdminVO admin;
}
