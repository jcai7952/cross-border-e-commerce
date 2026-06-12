package com.kinn.shop.api.logistics.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ShippingQuoteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 运费 USD 分 */
    private long feeCents;
    private String zoneName;
    private int estDaysMin;
    private int estDaysMax;
}
