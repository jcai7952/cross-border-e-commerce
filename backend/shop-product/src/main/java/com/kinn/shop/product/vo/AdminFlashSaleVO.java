package com.kinn.shop.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端闪购活动（含商品明细）。
 */
@Data
@Schema(description = "管理端闪购活动")
public class AdminFlashSaleVO implements Serializable {

    private Long id;

    private String title;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Schema(description = "1 启用 0 停用")
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<ItemVO> items;

    @Data
    @Schema(description = "闪购商品明细")
    public static class ItemVO implements Serializable {

        private Long id;

        private Long productId;

        @Schema(description = "商品英文名（en-US）")
        private String productNameEn;

        @Schema(description = "折扣力度：30 = off 30%（七折）")
        private Integer discountPercent;

        @Schema(description = "限量，0 不限量")
        private Integer quota;

        private Integer sold;
    }
}
