package com.kinn.shop.user.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.user.dto.PasswordUpdateDTO;
import com.kinn.shop.user.dto.ProfileUpdateDTO;
import com.kinn.shop.user.service.UserService;
import com.kinn.shop.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户", description = "个人资料（需登录）")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "查询个人资料")
    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.ok(userService.profile(LoginContext.requireUserId()));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        return Result.ok(userService.updateProfile(LoginContext.requireUserId(), dto));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        userService.updatePassword(LoginContext.requireUserId(), dto);
        return Result.ok();
    }
}
