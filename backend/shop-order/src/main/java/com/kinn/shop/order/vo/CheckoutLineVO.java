package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "结算明细行")
public class CheckoutLineVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long skuId;

    private Long productId;

    @Schema(description = "商品名（请求 locale）")
    private String name;

    private String skuText;

    private String image;

    @Schema(description = "BONDED / DIRECT")
    private String tradeMode;

    private Integer quantity;

    @Schema(description = "成交单价 USD 分")
    private long unitPriceUsdCents;

    @Schema(description = "小计 USD 分")
    private long lineTotalUsdCents;

    @Schema(description = "成交单价（展示币）")
    private PriceVO unitPrice;

    @Schema(description = "小计（展示币）")
    private PriceVO lineTotal;
}
