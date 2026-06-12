package com.kinn.shop.order.controller;

import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.order.dto.CouponSaveDTO;
import com.kinn.shop.order.dto.CouponStatusDTO;
import com.kinn.shop.order.entity.Coupon;
import com.kinn.shop.order.service.AdminCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "优惠券（管理端）")
@RestController
@RequestMapping("/api/admin/coupon")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @Operation(summary = "优惠券分页")
    @GetMapping("/page")
    public Result<PageResult<Coupon>> page(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(adminCouponService.page(pageNum, pageSize));
    }

    @Operation(summary = "创建优惠券")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CouponSaveDTO dto) {
        return Result.ok(adminCouponService.create(dto));
    }

    @Operation(summary = "启用/停用")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody CouponStatusDTO dto) {
        adminCouponService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }
}
