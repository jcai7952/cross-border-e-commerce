package com.kinn.shop.api.logistics.dto;

import lombok.Data;

import java.io.Serializable;

/** 发货建单参数（订单服务 → 物流服务）。 */
@Data
public class ShipmentCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private Long userId;
    /** ISO 3166-1 alpha-2 目的国 */
    private String countryCode;
    /** 收件人快照 JSON（与订单 receiver_json 同构） */
    private String receiverJson;
    private Integer weightGrams;
    /** 运费 USD 分（订单已收） */
    private Long feeCents;
    /** 承运商，空则默认 KinnExpress */
    private String carrier;
}
