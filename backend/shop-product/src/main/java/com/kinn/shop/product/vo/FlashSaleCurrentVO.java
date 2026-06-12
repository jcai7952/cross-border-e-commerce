package com.kinn.shop.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 进行中的闪购活动。
 */
@Data
@Schema(description = "进行中的闪购")
public class FlashSaleCurrentVO implements Serializable {

    private Long id;

    private String title;

    private LocalDateTime endTime;

    private List<ProductListVO> items;
}
