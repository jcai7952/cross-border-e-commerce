package com.kinn.shop.user.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.user.dto.IdentityAddDTO;
import com.kinn.shop.user.service.IdentityService;
import com.kinn.shop.user.vo.IdentityVO;
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

@Tag(name = "清关实名", description = "清关实名信息管理（需登录，仅返回脱敏数据）")
@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;

    @Operation(summary = "实名信息列表（脱敏）")
    @GetMapping("/list")
    public Result<List<IdentityVO>> list() {
        return Result.ok(identityService.list(LoginContext.requireUserId()));
    }

    @Operation(summary = "新增实名信息（身份证校验位校验 + AES 加密存储）")
    @PostMapping
    public Result<IdentityVO> add(@Valid @RequestBody IdentityAddDTO dto) {
        return Result.ok(identityService.add(LoginContext.requireUserId(), dto));
    }

    @Operation(summary = "删除实名信息")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        identityService.delete(LoginContext.requireUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "设为默认实名（同一用户互斥）")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        identityService.setDefault(LoginContext.requireUserId(), id);
        return Result.ok();
    }
}
