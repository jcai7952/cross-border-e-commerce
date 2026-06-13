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

import java.util.List;

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

    @Operation(summary = "规则推荐（游客可访问）",
            description = "同叶子类目在售按销量降序排除自身，不足 limit 用全站热销补足（去重）")
    @GetMapping("/recommend")
    public Result<List<ProductListVO>> recommend(@RequestParam Long productId,
                                                 @RequestParam(defaultValue = "en-US") String locale,
                                                 @RequestParam(defaultValue = "USD") String currency,
                                                 @RequestParam(defaultValue = "8") Integer limit) {
        return Result.ok(productService.recommend(productId, locale, currency, limit));
    }

    @Operation(summary = "全站热销（游客可访问，在售按销量降序）")
    @GetMapping("/best-sellers")
    public Result<List<ProductListVO>> bestSellers(@RequestParam(defaultValue = "en-US") String locale,
                                                   @RequestParam(defaultValue = "USD") String currency,
                                                   @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(productService.bestSellers(locale, currency, limit));
    }

    @Operation(summary = "商品详情（游客可访问，登录用户带收藏状态）")
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id,
                                          @RequestParam(defaultValue = "en-US") String locale,
                                          @RequestParam(defaultValue = "USD") String currency) {
        return Result.ok(productService.detail(id, locale, currency));
    }
}
