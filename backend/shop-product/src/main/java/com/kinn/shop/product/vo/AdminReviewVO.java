package com.kinn.shop.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端评论（含待审/拒绝，images 输出完整 URL）。
 */
@Data
@Schema(description = "管理端评论")
public class AdminReviewVO implements Serializable {

    private Long id;

    private Long productId;

    private String orderNo;

    private Long userId;

    private String userNickname;

    @Schema(description = "购买规格，如 Black / M")
    private String skuText;

    private Integer rating;

    private String content;

    @Schema(description = "晒图完整 URL")
    private List<String> images;

    @Schema(description = "0 待审 1 通过 2 拒绝")
    private Integer status;

    private LocalDateTime createTime;
}
