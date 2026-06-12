package com.kinn.shop.product.controller;

import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.product.service.ProductService;
import com.kinn.shop.product.vo.ProductDetailVO;
import com.kinn.shop.product.vo.ProductListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品（前台）")
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品分页搜索（游客可访问）",
            description = "sort ∈ new|sales|price_asc|price_desc；categoryId 为一级类目时含其子类目商品")
    @GetMapping("/page")
    public Result<PageResult<ProductListVO>> page(@RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String sort,
                                                  @RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "20") long pageSize,
                                                  @RequestParam(defaultValue = "en-US") String locale,
                                                  @RequestParam(defaultValue = "USD") String currency) {
        return Result.ok(productService.page(categoryId, keyword, sort, pageNum, pageSize, locale, currency));
    }

    @Operation(summary = "商品详情（游客可访问，登录用户带收藏状态）")
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id,
                                          @RequestParam(defaultValue = "en-US") String locale,
                                          @RequestParam(defaultValue = "USD") String currency) {
        return Result.ok(productService.detail(id, locale, currency));
    }
}
