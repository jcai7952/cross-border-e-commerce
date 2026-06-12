package com.kinn.shop.order.controller;

import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.order.service.AdminOrderService;
import com.kinn.shop.order.vo.AdminOrderDetailVO;
import com.kinn.shop.order.vo.AdminOrderPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单（管理端）")
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "全量订单分页（金额 USD 分，含明细摘要）")
    @GetMapping("/page")
    public Result<PageResult<AdminOrderPageVO>> page(@RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String orderNo,
                                                     @RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(adminOrderService.page(status, orderNo, pageNum, pageSize));
    }

    @Operation(summary = "订单全字段详情")
    @GetMapping("/{orderNo}")
    public Result<AdminOrderDetailVO> detail(@PathVariable String orderNo) {
        return Result.ok(adminOrderService.detail(orderNo));
    }
}
