package com.kinn.shop.user.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.user.dto.UserStatusDTO;
import com.kinn.shop.user.service.AdminUserService;
import com.kinn.shop.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-用户管理", description = "后台用户列表/禁用启用（需管理员登录）")
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "用户分页列表")
    @GetMapping("/page")
    public Result<PageResult<UserVO>> page(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        LoginContext.requireAdminId();
        return Result.ok(adminUserService.page(email, status, pageNum, pageSize));
    }

    @Operation(summary = "禁用/启用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        LoginContext.requireAdminId();
        adminUserService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }
}
