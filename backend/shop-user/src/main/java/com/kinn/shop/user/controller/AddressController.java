package com.kinn.shop.user.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.user.dto.AddressDTO;
import com.kinn.shop.user.service.AddressService;
import com.kinn.shop.user.vo.AddressVO;
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

@Tag(name = "收货地址", description = "收货地址管理（需登录）")
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "地址列表")
    @GetMapping("/list")
    public Result<List<AddressVO>> list() {
        return Result.ok(addressService.list(LoginContext.requireUserId()));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<AddressVO> add(@Valid @RequestBody AddressDTO dto) {
        return Result.ok(addressService.add(LoginContext.requireUserId(), dto));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        return Result.ok(addressService.update(LoginContext.requireUserId(), id, dto));
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(LoginContext.requireUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "设为默认地址（同一用户互斥）")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(LoginContext.requireUserId(), id);
        return Result.ok();
    }
}
