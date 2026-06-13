package com.kinn.shop.api.logistics;

import com.kinn.shop.api.logistics.dto.ShipmentCreateDTO;
import com.kinn.shop.api.logistics.dto.ShippingQuoteDTO;

/**
 * 物流服务对内 RPC：结算运费试算、发货建单。
 */
public interface LogisticsFacade {

    /** 按目的国与总重量报价；不在任何配送区域返回 null。 */
    ShippingQuoteDTO quote(String countryCode, int totalWeightGrams);

    /**
     * 发货建单并落首条轨迹（已揽收）；幂等：同 orderNo 已有物流单直接返回其单号。
     * 返回 shipmentNo；参数非法返回 null。
     */
    String createShipment(ShipmentCreateDTO dto);
}
