package com.kinn.shop.product.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.product.dto.FlashSaleSaveDTO;
import com.kinn.shop.product.dto.StatusDTO;
import com.kinn.shop.product.service.AdminFlashSaleService;
import com.kinn.shop.product.vo.AdminFlashSaleVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "闪购（管理端）")
@RestController
@RequestMapping("/api/admin/flash-sale")
@RequiredArgsConstructor
public class AdminFlashSaleController {

    private final AdminFlashSaleService adminFlashSaleService;

    @Operation(summary = "活动列表（含各自商品明细：英文名/折扣/限量/已售）")
    @GetMapping("/list")
    public Result<List<AdminFlashSaleVO>> list() {
        LoginContext.requireAdminId();
        return Result.ok(adminFlashSaleService.list());
    }

    @Operation(summary = "创建活动（事务，校验商品存在）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody FlashSaleSaveDTO dto) {
        LoginContext.requireAdminId();
        return Result.ok(adminFlashSaleService.create(dto));
    }

    @Operation(summary = "全量更新活动（items 删后插）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FlashSaleSaveDTO dto) {
        LoginContext.requireAdminId();
        adminFlashSaleService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "启停活动")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        LoginContext.requireAdminId();
        adminFlashSaleService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }
}
