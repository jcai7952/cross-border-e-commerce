package com.kinn.shop.order.service;

import com.kinn.shop.api.product.ProductTradeFacade;
import com.kinn.shop.api.product.dto.RateDTO;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.util.CurrencyUtil;
import com.kinn.shop.order.vo.PriceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 价格出参装配：汇率来自商品服务（base 恒为 USD），一次请求解析一遍后批量复用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {

    private final ProductTradeFacade productTradeFacade;

    /** 解析展示/支付币种；不支持抛 CURRENCY_NOT_SUPPORTED。 */
    public RateDTO resolve(String currency) {
        String code = (currency == null || currency.isBlank()) ? "USD" : currency.trim().toUpperCase();
        RateDTO rate = productTradeFacade.getRate(code);
        if (rate == null) {
            throw new BizException(ErrorCode.CURRENCY_NOT_SUPPORTED);
        }
        return rate;
    }

    /**
     * 仅取币种元数据（symbol/小数位）用于历史订单展示：订单金额按下单锁定汇率计算，
     * 商品服务不可达或币种已下线时回退「码即符号 + 2 位小数」，不阻断查询。
     */
    public RateDTO metaOrFallback(String currency) {
        String code = (currency == null || currency.isBlank()) ? "USD" : currency.trim().toUpperCase();
        try {
            RateDTO rate = productTradeFacade.getRate(code);
            if (rate != null) {
                return rate;
            }
        } catch (Exception e) {
            log.warn("[price] getRate({}) failed, fallback to code/2-digits: {}", code, e.getMessage());
        }
        RateDTO fallback = new RateDTO();
        fallback.setCurrency(code);
        fallback.setSymbol(code);
        fallback.setDecimalDigits("JPY".equals(code) || "KRW".equals(code) ? 0 : 2);
        fallback.setRate(BigDecimal.ONE);
        return fallback;
    }

    /** USD 分 → 目标币 PriceVO（用 RateDTO 自带汇率）。 */
    public PriceVO build(long usdCents, RateDTO rate) {
        long minor = CurrencyUtil.convert(usdCents, rate.getRate(), rate.getDecimalDigits());
        return new PriceVO(rate.getCurrency(), rate.getSymbol(),
                minor, CurrencyUtil.display(minor, rate.getDecimalDigits()));
    }

    /** USD 分 → 目标币 PriceVO（订单锁定汇率，meta 只取 symbol/小数位）。 */
    public PriceVO buildLocked(long usdCents, BigDecimal lockedRate, RateDTO meta) {
        long minor = CurrencyUtil.convert(usdCents, lockedRate, meta.getDecimalDigits());
        return new PriceVO(meta.getCurrency(), meta.getSymbol(),
                minor, CurrencyUtil.display(minor, meta.getDecimalDigits()));
    }

    /** 已是目标币最小单位的金额 → PriceVO（订单 pay_amount_cents 直出）。 */
    public PriceVO buildMinor(long minorUnits, RateDTO meta) {
        return new PriceVO(meta.getCurrency(), meta.getSymbol(),
                minorUnits, CurrencyUtil.display(minorUnits, meta.getDecimalDigits()));
    }
}
