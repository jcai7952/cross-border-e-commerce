package com.kinn.shop.api.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockOpDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long skuId;
    private Long productId;
    private int quantity;
}
