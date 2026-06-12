package com.kinn.shop.order.vo;

import com.kinn.shop.order.entity.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "订单明细行（下单时点快照）")
public class OrderItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;

    private Long skuId;

    @Schema(description = "商品名（下单语言快照）")
    private String productName;

    private String skuText;

    private String image;

    @Schema(description = "成交单价 USD 分")
    private Long priceCents;

    private Integer quantity;

    @Schema(description = "小计 USD 分")
    private Long totalCents;

    public static OrderItemVO from(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setProductId(item.getProductId());
        vo.setSkuId(item.getSkuId());
        vo.setProductName(item.getProductName());
        vo.setSkuText(item.getSkuText());
        vo.setImage(item.getImage());
        vo.setPriceCents(item.getPriceCents());
        vo.setQuantity(item.getQuantity());
        vo.setTotalCents(item.getTotalCents());
        return vo;
    }
}
