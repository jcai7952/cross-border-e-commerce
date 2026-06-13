package com.kinn.shop.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 评论发布入参（需已购且订单完成）。
 */
@Data
@Schema(description = "评论发布入参")
public class ReviewCreateDTO {

    @NotBlank
    @Schema(description = "已完成订单号")
    private String orderNo;

    @NotNull
    private Long productId;

    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "评分 1-5")
    private Integer rating;

    @Size(max = 1000)
    private String content;

    @Size(max = 6)
    @Schema(description = "晒图 MinIO 对象 key，最多 6 张")
    private List<String> images;
}
