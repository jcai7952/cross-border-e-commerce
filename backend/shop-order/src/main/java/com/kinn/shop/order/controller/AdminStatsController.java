package com.kinn.shop.order.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.order.mapper.OrdersMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "经营统计（管理端）")
@RestController
@RequestMapping("/api/admin/order/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final OrdersMapper ordersMapper;

    @Operation(summary = "仪表盘统计：今日/累计销售、状态分布、近N天趋势（金额 USD 分）")
    @GetMapping
    public Result<Map<String, Object>> stats(@RequestParam(defaultValue = "7") int days) {
        LoginContext.requireAdminId();
        int d = Math.min(Math.max(days, 1), 90);
        Map<String, Object> result = new HashMap<>();
        result.put("today", ordersMapper.todayPaid());
        result.put("total", ordersMapper.totalPaid());
        result.put("statusCounts", ordersMapper.statusCounts());
        result.put("daily", ordersMapper.dailyPaidStats(d));
        return Result.ok(result);
    }
}
