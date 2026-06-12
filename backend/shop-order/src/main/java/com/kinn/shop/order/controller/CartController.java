package com.kinn.shop.order.controller;

import com.kinn.shop.common.core.Result;
import com.kinn.shop.order.dto.CartAddDTO;
import com.kinn.shop.order.dto.CartCheckAllDTO;
import com.kinn.shop.order.dto.CartUpdateDTO;
import com.kinn.shop.order.service.CartService;
import com.kinn.shop.order.vo.CartListVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "购物车（需登录）")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "购物车列表（实时价格/库存/上架状态）")
    @GetMapping("/list")
    public Result<CartListVO> list(@RequestParam(defaultValue = "en-US") String locale,
                                   @RequestParam(defaultValue = "USD") String currency) {
        return Result.ok(cartService.list(locale, currency));
    }

    @Operation(summary = "加入购物车（已存在则累加，单条上限 99），返回购物车总件数")
    @PostMapping
    public Result<Map<String, Long>> add(@Valid @RequestBody CartAddDTO dto) {
        return Result.ok(Map.of("count", cartService.add(dto)));
    }

    @Operation(summary = "修改数量/勾选")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CartUpdateDTO dto) {
        cartService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "全选/全不选")
    @PutMapping("/check-all")
    public Result<Void> checkAll(@Valid @RequestBody CartCheckAllDTO dto) {
        cartService.checkAll(dto.getChecked());
        return Result.ok();
    }

    @Operation(summary = "删除单条")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "删除全部勾选项")
    @DeleteMapping("/checked")
    public Result<Void> deleteChecked() {
        cartService.deleteChecked();
        return Result.ok();
    }

    @Operation(summary = "购物车总件数（角标）")
    @GetMapping("/count")
    public Result<Map<String, Long>> count() {
        return Result.ok(Map.of("count", cartService.count()));
    }
}
