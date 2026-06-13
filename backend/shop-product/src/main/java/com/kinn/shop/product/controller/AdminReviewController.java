package com.kinn.shop.product.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.product.dto.ReviewAuditDTO;
import com.kinn.shop.product.service.AdminReviewService;
import com.kinn.shop.product.vo.AdminReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "评论（管理端）")
@RestController
@RequestMapping("/api/admin/review")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @Operation(summary = "评论分页（含待审/拒绝，images 输出完整 URL）")
    @GetMapping("/page")
    public Result<PageResult<AdminReviewVO>> page(@RequestParam(required = false) Long productId,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "20") long pageSize) {
        LoginContext.requireAdminId();
        return Result.ok(adminReviewService.page(productId, status, pageNum, pageSize));
    }

    @Operation(summary = "审核（approve=true 通过并聚合商品评分，false 拒绝/撤下并反向扣减）")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody ReviewAuditDTO dto) {
        LoginContext.requireAdminId();
        adminReviewService.audit(id, dto.getApprove());
        return Result.ok();
    }
}
