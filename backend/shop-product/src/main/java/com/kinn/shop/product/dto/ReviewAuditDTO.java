package com.kinn.shop.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评论审核入参。
 */
@Data
@Schema(description = "评论审核入参")
public class ReviewAuditDTO {

    @NotNull
    @Schema(description = "true 通过 false 拒绝/撤下")
    private Boolean approve;
}
