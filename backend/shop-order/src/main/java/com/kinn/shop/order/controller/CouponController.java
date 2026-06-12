package com.kinn.shop.order.controller;

import com.kinn.shop.common.core.Result;
import com.kinn.shop.order.service.CouponService;
import com.kinn.shop.order.vo.CouponVO;
import com.kinn.shop.order.vo.UserCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "优惠券（需登录）")
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "可领券列表（含当前用户已领张数/是否可再领）")
    @GetMapping("/available")
    public Result<List<CouponVO>> available(@RequestParam(defaultValue = "en-US") String locale) {
        return Result.ok(couponService.available());
    }

    @Operation(summary = "领取优惠券（原子占名额，超发即拒）")
    @PostMapping("/{id}/claim")
    public Result<Void> claim(@PathVariable Long id) {
        couponService.claim(id);
        return Result.ok();
    }

    @Operation(summary = "我的券（0未用 1已用 2过期；不传查全部，过期未用自动置 2）")
    @GetMapping("/mine")
    public Result<List<UserCouponVO>> mine(@RequestParam(required = false) Integer status) {
        return Result.ok(couponService.mine(status));
    }
}
