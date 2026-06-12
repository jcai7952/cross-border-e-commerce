package com.kinn.shop.product.controller;

import com.kinn.shop.common.core.Result;
import com.kinn.shop.product.service.ProductService;
import com.kinn.shop.product.vo.FlashSaleCurrentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "闪购（前台）")
@RestController
@RequestMapping("/api/flash-sale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final ProductService productService;

    @Operation(summary = "进行中的闪购活动（无则 data=null，游客可访问）")
    @GetMapping("/current")
    public Result<FlashSaleCurrentVO> current(@RequestParam(defaultValue = "en-US") String locale,
                                              @RequestParam(defaultValue = "USD") String currency) {
        return Result.ok(productService.flashSaleCurrent(locale, currency));
    }
}
