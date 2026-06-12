package com.kinn.shop.user.controller;

import com.kinn.shop.common.core.Result;
import com.kinn.shop.user.dto.EmailCodeDTO;
import com.kinn.shop.user.dto.LoginDTO;
import com.kinn.shop.user.dto.RegisterDTO;
import com.kinn.shop.user.dto.ResetPasswordDTO;
import com.kinn.shop.user.service.AuthService;
import com.kinn.shop.user.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证", description = "C 端注册/登录/验证码/重置密码")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "发送邮箱验证码（scene: register|reset）")
    @PostMapping("/email-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody EmailCodeDTO dto) {
        authService.sendEmailCode(dto);
        return Result.ok();
    }

    @Operation(summary = "邮箱验证码注册（注册即登录）")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(authService.register(dto));
    }

    @Operation(summary = "邮箱密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "邮箱验证码重置密码（scene: reset）")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return Result.ok();
    }
}
