package com.kinn.shop.product.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.product.dto.CategorySaveDTO;
import com.kinn.shop.product.service.CategoryService;
import com.kinn.shop.product.vo.AdminCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "类目（管理端）")
@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "类目树（含税率/排序/状态）")
    @GetMapping("/tree")
    public Result<List<AdminCategoryVO>> tree() {
        LoginContext.requireAdminId();
        return Result.ok(categoryService.adminTree());
    }

    @Operation(summary = "创建类目")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategorySaveDTO dto) {
        LoginContext.requireAdminId();
        return Result.ok(categoryService.create(dto));
    }

    @Operation(summary = "更新类目")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategorySaveDTO dto) {
        LoginContext.requireAdminId();
        categoryService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除类目（存在子类目或商品时拒绝）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        LoginContext.requireAdminId();
        categoryService.delete(id);
        return Result.ok();
    }
}
