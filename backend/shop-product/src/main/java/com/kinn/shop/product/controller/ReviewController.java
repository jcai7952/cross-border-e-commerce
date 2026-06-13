package com.kinn.shop.product.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.product.dto.ReviewCreateDTO;
import com.kinn.shop.product.service.ReviewService;
import com.kinn.shop.product.vo.ReviewVO;
import com.kinn.shop.product.vo.UploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "评论（前台）")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "商品评论分页（仅已审核通过，游客可访问）")
    @GetMapping("/list")
    public Result<PageResult<ReviewVO>> list(@RequestParam Long productId,
                                             @RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(reviewService.page(productId, pageNum, pageSize));
    }

    @Operation(summary = "发布评论（需登录，订单完成且含该商品，每单每商品一条，待审核后展示）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ReviewCreateDTO dto) {
        return Result.ok(reviewService.create(dto));
    }

    @Operation(summary = "上传晒图（需登录，仅图片、最大 5MB），返回对象 key 与完整 URL")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        LoginContext.requireUserId();
        return Result.ok(reviewService.uploadImage(file));
    }
}
