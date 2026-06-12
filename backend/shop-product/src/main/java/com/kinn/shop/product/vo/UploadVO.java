package com.kinn.shop.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件上传出参。
 */
@Schema(description = "文件上传结果")
public record UploadVO(
        @Schema(description = "MinIO 对象 key") String key,
        @Schema(description = "完整访问 URL") String url) {
}
