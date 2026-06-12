package com.kinn.shop.product.controller;

import com.kinn.shop.common.core.Result;
import com.kinn.shop.product.service.CurrencyService;
import com.kinn.shop.product.vo.CurrencyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "币种（前台）")
@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @Operation(summary = "可用币种列表（游客可访问）")
    @GetMapping("/list")
    public Result<List<CurrencyVO>> list() {
        return Result.ok(currencyService.listEnabled());
    }
}
