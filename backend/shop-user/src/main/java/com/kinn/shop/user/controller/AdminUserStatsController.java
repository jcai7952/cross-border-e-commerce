package com.kinn.shop.user.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.user.entity.User;
import com.kinn.shop.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "用户统计（管理端）")
@RestController
@RequestMapping("/api/admin/user/stats")
@RequiredArgsConstructor
public class AdminUserStatsController {

    private final UserMapper userMapper;

    @Operation(summary = "用户总数与今日新增")
    @GetMapping
    public Result<Map<String, Object>> stats() {
        LoginContext.requireAdminId();
        Long total = userMapper.selectCount(null);
        Long todayNew = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .ge(User::getCreateTime, LocalDate.now().atStartOfDay()));
        return Result.ok(Map.of("totalUsers", total, "todayNew", todayNew));
    }
}
