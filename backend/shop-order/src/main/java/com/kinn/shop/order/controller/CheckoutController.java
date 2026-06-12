package com.kinn.shop.order.controller;

import com.kinn.shop.common.core.Result;
import com.kinn.shop.order.dto.CheckoutPreviewDTO;
import com.kinn.shop.order.service.CheckoutService;
import com.kinn.shop.order.vo.CheckoutPreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "结算（需登录）")
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @Operation(summary = "结算预览：商品+券+运费+税+应付全量试算（items 直传或 fromCart 取勾选）")
    @PostMapping("/preview")
    public Result<CheckoutPreviewVO> preview(@Valid @RequestBody CheckoutPreviewDTO dto) {
        return Result.ok(checkoutService.preview(dto));
    }
}
