package com.kinn.shop.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "结算预览")
public class CheckoutPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CheckoutLineVO> items;

    @Schema(description = "商品金额")
    private MoneyVO goods;

    @Schema(description = "券抵扣")
    private MoneyVO discount;

    @Schema(description = "运费")
    private MoneyVO shipping;

    @Schema(description = "进口税费")
    private MoneyVO tax;

    @Schema(description = "应付合计")
    private MoneyVO total;

    @Schema(description = "清关是否需要订购人实名")
    private boolean identityRequired;

    @Schema(description = "税费简短说明")
    private String taxNote;

    @Schema(description = "预计送达天数下限")
    private int estDaysMin;

    @Schema(description = "预计送达天数上限")
    private int estDaysMax;

    @Schema(description = "展示币种与汇率")
    private RateVO rate;

    @Schema(description = "我的可用券（满足门槛的未用券）")
    private List<CheckoutCouponVO> availableCoupons;
}
