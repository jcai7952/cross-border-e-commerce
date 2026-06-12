package com.kinn.shop.product.controller;

import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.product.service.FavoriteService;
import com.kinn.shop.product.vo.ProductListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "收藏夹（需登录）")
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "切换收藏状态")
    @PostMapping("/{productId}")
    public Result<Map<String, Boolean>> toggle(@PathVariable Long productId) {
        return Result.ok(Map.of("favorite", favoriteService.toggle(productId)));
    }

    @Operation(summary = "收藏列表")
    @GetMapping("/list")
    public Result<PageResult<ProductListVO>> list(@RequestParam(defaultValue = "en-US") String locale,
                                                  @RequestParam(defaultValue = "USD") String currency,
                                                  @RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(favoriteService.list(locale, currency, pageNum, pageSize));
    }
}
