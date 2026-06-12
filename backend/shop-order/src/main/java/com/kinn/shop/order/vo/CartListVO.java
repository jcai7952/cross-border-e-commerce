package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "购物车列表")
public class CartListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CartItemVO> items;

    @Schema(description = "已勾选（且有效）条目数")
    private int checkedCount;

    @Schema(description = "勾选项小计")
    private PriceVO subtotal;
}
