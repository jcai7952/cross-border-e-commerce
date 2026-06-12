package com.kinn.shop.api.logistics;

import com.kinn.shop.api.logistics.dto.ShippingQuoteDTO;

/**
 * 物流服务对内 RPC：结算运费试算。
 */
public interface LogisticsFacade {

    /** 按目的国与总重量报价；不在任何配送区域返回 null。 */
    ShippingQuoteDTO quote(String countryCode, int totalWeightGrams);
}
