package com.kinn.shop.product.service;

import com.kinn.shop.common.util.CurrencyUtil;
import com.kinn.shop.product.entity.Currency;
import com.kinn.shop.product.vo.PriceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 价格出参装配：解析一次币种上下文（symbol/小数位/汇率），批量换算复用。
 */
@Service
@RequiredArgsConstructor
public class PriceService {

    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;

    /** 币种上下文：一次请求只解析一遍。 */
    public record CurrencyCtx(Currency currency, BigDecimal rate) {
    }

    public CurrencyCtx resolve(String currencyCode) {
        String code = (currencyCode == null || currencyCode.isBlank()) ? "USD" : currencyCode.trim().toUpperCase();
        Currency currency = currencyService.getRequired(code);
        BigDecimal rate = exchangeRateService.getRate(code);
        return new CurrencyCtx(currency, rate);
    }

    /** USD 分 → 目标币 PriceVO。 */
    public PriceVO build(long usdCents, CurrencyCtx ctx) {
        int digits = ctx.currency().getDecimalDigits() == null ? 2 : ctx.currency().getDecimalDigits();
        long minor = CurrencyUtil.convert(usdCents, ctx.rate(), digits);
        return new PriceVO(ctx.currency().getCode(), ctx.currency().getSymbol(), minor, CurrencyUtil.display(minor, digits));
    }

    /** 闪购：先在 USD 分上折算，再换汇。 */
    public static long flashCents(long usdCents, int discountPercent) {
        return usdCents * (100 - discountPercent) / 100;
    }
}
