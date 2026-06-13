package com.kinn.shop.payment.controller;

import com.kinn.shop.common.core.Result;
import com.kinn.shop.payment.dto.PayCreateDTO;
import com.kinn.shop.payment.service.PayService;
import com.kinn.shop.payment.vo.PayCreateVO;
import com.kinn.shop.payment.vo.PayOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "支付（需登录）")
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    @Operation(summary = "发起支付（同单同渠道未付单复用并刷新载荷）")
    @PostMapping("/create")
    public Result<PayCreateVO> create(@Valid @RequestBody PayCreateDTO dto) {
        return Result.ok(payService.create(dto));
    }

    @Operation(summary = "支付单状态查询（归属校验）")
    @GetMapping("/{payNo}")
    public Result<PayOrderVO> detail(@PathVariable String payNo) {
        return Result.ok(payService.detail(payNo));
    }

    @Operation(summary = "主动同步渠道状态（支付完成回跳后兜底，与 webhook 幂等互斥）")
    @PostMapping("/{payNo}/sync")
    public Result<PayOrderVO> sync(@PathVariable String payNo) {
        return Result.ok(payService.sync(payNo));
    }
}
