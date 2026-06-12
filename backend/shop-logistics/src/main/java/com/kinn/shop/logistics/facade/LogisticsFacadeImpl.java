package com.kinn.shop.logistics.facade;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.api.logistics.LogisticsFacade;
import com.kinn.shop.api.logistics.dto.ShippingQuoteDTO;
import com.kinn.shop.logistics.entity.ShippingTemplate;
import com.kinn.shop.logistics.entity.ShippingZone;
import com.kinn.shop.logistics.mapper.ShippingTemplateMapper;
import com.kinn.shop.logistics.mapper.ShippingZoneMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 物流服务对内 RPC：结算运费试算（首重+续重）。
 */
@DubboService
@RequiredArgsConstructor
public class LogisticsFacadeImpl implements LogisticsFacade {

    private final ShippingTemplateMapper templateMapper;
    private final ShippingZoneMapper zoneMapper;

    @Override
    public ShippingQuoteDTO quote(String countryCode, int totalWeightGrams) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        String country = countryCode.trim().toUpperCase();
        // 取启用模板中 id 最小的一个
        ShippingTemplate template = templateMapper.selectOne(Wrappers.<ShippingTemplate>lambdaQuery()
                .eq(ShippingTemplate::getStatus, 1)
                .orderByAsc(ShippingTemplate::getId)
                .last("LIMIT 1"));
        if (template == null) {
            return null;
        }
        List<ShippingZone> zones = zoneMapper.selectList(Wrappers.<ShippingZone>lambdaQuery()
                .eq(ShippingZone::getTemplateId, template.getId())
                .orderByAsc(ShippingZone::getId));
        ShippingZone zone = zones.stream()
                .filter(z -> containsCountry(z.getCountries(), country))
                .findFirst()
                .orElse(null);
        if (zone == null) {
            return null; // 不在任何配送区域
        }
        ShippingQuoteDTO dto = new ShippingQuoteDTO();
        dto.setFeeCents(calcFee(zone, totalWeightGrams));
        dto.setZoneName(zone.getZoneName());
        dto.setEstDaysMin(zone.getEstDaysMin() == null ? 0 : zone.getEstDaysMin());
        dto.setEstDaysMax(zone.getEstDaysMax() == null ? 0 : zone.getEstDaysMax());
        return dto;
    }

    /** 运费 = first_fee + max(0, ceil((weight - first_weight) / add_weight)) * add_fee；不超首重只收首重。 */
    private long calcFee(ShippingZone zone, int weightGrams) {
        long fee = zone.getFirstFeeCents() == null ? 0 : zone.getFirstFeeCents();
        int firstW = zone.getFirstWeightG() == null ? 0 : zone.getFirstWeightG();
        int addW = zone.getAddWeightG() == null ? 0 : zone.getAddWeightG();
        if (weightGrams > firstW && addW > 0) {
            long addUnits = ((long) weightGrams - firstW + addW - 1) / addW; // 向上取整
            long addFee = zone.getAddFeeCents() == null ? 0 : zone.getAddFeeCents();
            fee += addUnits * addFee;
        }
        return fee;
    }

    /** 逗号分隔国家列表大写比对。 */
    private boolean containsCountry(String countries, String country) {
        if (countries == null || countries.isBlank()) {
            return false;
        }
        for (String c : countries.split(",")) {
            if (c.trim().toUpperCase().equals(country)) {
                return true;
            }
        }
        return false;
    }
}
