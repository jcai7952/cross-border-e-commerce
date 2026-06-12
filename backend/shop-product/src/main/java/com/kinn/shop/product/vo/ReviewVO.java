package com.kinn.shop.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品评论。
 */
@Data
@Schema(description = "商品评论")
public class ReviewVO implements Serializable {

    private Long id;

    private String userNickname;

    @Schema(description = "购买规格，如 Black / M")
    private String skuText;

    private Integer rating;

    private String content;

    @Schema(description = "晒图完整 URL")
    private List<String> images;

    private LocalDateTime createTime;
}
