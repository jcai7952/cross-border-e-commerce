package com.kinn.shop.product.controller;

import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.product.dto.ProductSaveDTO;
import com.kinn.shop.product.dto.StatusDTO;
import com.kinn.shop.product.dto.StockDTO;
import com.kinn.shop.product.service.AdminProductService;
import com.kinn.shop.product.service.FileService;
import com.kinn.shop.product.vo.AdminProductDetailVO;
import com.kinn.shop.product.vo.AdminProductPageVO;
import com.kinn.shop.product.vo.UploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "商品（管理端）")
@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final FileService fileService;

    @Operation(summary = "商品分页（keyword 匹配 en-US 名称或 SPU 编码）")
    @GetMapping("/page")
    public Result<PageResult<AdminProductPageVO>> page(@RequestParam(required = false) Long categoryId,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "20") long pageSize) {
        LoginContext.requireAdminId();
        return Result.ok(adminProductService.page(categoryId, keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "商品完整编辑视图（i18n 两条 + 图册 + SKU 真实库存）")
    @GetMapping("/{id}")
    public Result<AdminProductDetailVO> detail(@PathVariable Long id) {
        LoginContext.requireAdminId();
        return Result.ok(adminProductService.detail(id));
    }

    @Operation(summary = "创建商品（product+i18n+images+skus 一次提交，事务）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ProductSaveDTO dto) {
        LoginContext.requireAdminId();
        return Result.ok(adminProductService.create(dto));
    }

    @Operation(summary = "全量更新商品（i18n/images 删后插，skus 按 id 更新/新增/删除）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductSaveDTO dto) {
        LoginContext.requireAdminId();
        adminProductService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "上下架")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        LoginContext.requireAdminId();
        adminProductService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    @Operation(summary = "调整 SKU 库存")
    @PutMapping("/sku/{skuId}/stock")
    public Result<Void> updateStock(@PathVariable Long skuId, @Valid @RequestBody StockDTO dto) {
        LoginContext.requireAdminId();
        adminProductService.updateStock(skuId, dto.getStock());
        return Result.ok();
    }

    @Operation(summary = "上传商品图片，返回对象 key 与完整 URL")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        LoginContext.requireAdminId();
        String key = fileService.upload(file, "products");
        return Result.ok(new UploadVO(key, fileService.url(key)));
    }
}
