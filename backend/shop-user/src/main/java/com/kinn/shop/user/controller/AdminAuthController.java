package com.kinn.shop.user.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.user.dto.AdminLoginDTO;
import com.kinn.shop.user.service.AdminAuthService;
import com.kinn.shop.user.vo.AdminLoginVO;
import com.kinn.shop.user.vo.AdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-认证", description = "后台管理员登录/登出")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.ok(adminAuthService.login(dto));
    }

    @Operation(summary = "管理员个人信息")
    @GetMapping("/profile")
    public Result<AdminVO> profile() {
        return Result.ok(adminAuthService.profile(LoginContext.requireAdminId()));
    }

    @Operation(summary = "管理员登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        adminAuthService.logout();
        return Result.ok();
    }
}
