package com.kinn.shop.product.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.product.dto.RateUpdateDTO;
import com.kinn.shop.product.entity.ExchangeRate;
import com.kinn.shop.product.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "汇率（管理端）")
@RestController
@RequestMapping("/api/admin/currency")
@RequiredArgsConstructor
public class AdminCurrencyController {

    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "汇率全量列表（含 update_time/source）")
    @GetMapping("/rates")
    public Result<List<ExchangeRate>> rates() {
        LoginContext.requireAdminId();
        return Result.ok(exchangeRateService.listAll());
    }

    @Operation(summary = "手工覆盖汇率（source=MANUAL，清缓存）")
    @PutMapping("/rates")
    public Result<List<ExchangeRate>> override(@Valid @RequestBody RateUpdateDTO dto) {
        LoginContext.requireAdminId();
        exchangeRateService.manualOverride(dto.getQuote(), dto.getRate());
        return Result.ok(exchangeRateService.listAll());
    }

    @Operation(summary = "立即调 frankfurter 刷新汇率，返回最新列表")
    @PostMapping("/rates/refresh")
    public Result<List<ExchangeRate>> refresh() {
        LoginContext.requireAdminId();
        return Result.ok(exchangeRateService.refreshFromApi());
    }
}
