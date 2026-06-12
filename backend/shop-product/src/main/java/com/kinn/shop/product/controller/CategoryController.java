package com.kinn.shop.product.controller;

import com.kinn.shop.common.core.Result;
import com.kinn.shop.product.service.CategoryService;
import com.kinn.shop.product.vo.CategoryTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "类目（前台）")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "两级类目树（游客可访问）")
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> tree(@RequestParam(defaultValue = "en-US") String locale) {
        return Result.ok(categoryService.tree(locale));
    }
}
