package com.kinn.shop.payment.controller;

import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.payment.dto.RefundCreateDTO;
import com.kinn.shop.payment.service.AdminPayService;
import com.kinn.shop.payment.vo.AdminPayDetailVO;
import com.kinn.shop.payment.vo.AdminPayPageVO;
import com.kinn.shop.payment.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "支付（管理端）")
@RestController
@RequestMapping("/api/admin/pay")
@RequiredArgsConstructor
public class AdminPayController {

    private final AdminPayService adminPayService;

    @Operation(summary = "发起退款（全额/部分；累计不可超支付金额）")
    @PostMapping("/{payNo}/refund")
    public Result<RefundVO> refund(@PathVariable String payNo, @Valid @RequestBody RefundCreateDTO dto) {
        return Result.ok(adminPayService.refund(payNo, dto));
    }

    @Operation(summary = "支付流水分页")
    @GetMapping("/page")
    public Result<PageResult<AdminPayPageVO>> page(@RequestParam(required = false) String orderNo,
                                                   @RequestParam(required = false) String channel,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(adminPayService.page(orderNo, channel, status, pageNum, pageSize));
    }

    @Operation(summary = "退款单分页")
    @GetMapping("/refund/page")
    public Result<PageResult<RefundVO>> refundPage(@RequestParam(required = false) String payNo,
                                                   @RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(adminPayService.refundPage(payNo, pageNum, pageSize));
    }

    @Operation(summary = "支付单详情（含退款列表）")
    @GetMapping("/{payNo}")
    public Result<AdminPayDetailVO> detail(@PathVariable String payNo) {
        return Result.ok(adminPayService.detail(payNo));
    }
}
