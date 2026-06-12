package com.kinn.shop.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "购物车条目更新入参（两个字段均可空，只更新传入的）")
public class CartUpdateDTO {

    @Min(1)
    @Max(99)
    @Schema(description = "数量 1~99，可空")
    private Integer quantity;

    @Schema(description = "是否勾选，可空")
    private Boolean checked;
}
