package com.kinn.shop.payment.channel;

import com.kinn.shop.common.util.CurrencyUtil;

import java.util.Locale;
import java.util.Set;

/**
 * 支付币最小单位 ↔ 渠道展示金额：0 小数位币种（JPY 等）"1234"，其余 "12.34"。
 */
public final class CurrencyMinor {

    /** 0 小数位币种（对齐 Stripe/PayPal zero-decimal 清单常用项） */
    private static final Set<String> ZERO_DECIMAL = Set.of(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA",
            "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF");

    private CurrencyMinor() {
    }

    public static int decimalDigits(String currency) {
        return currency != null && ZERO_DECIMAL.contains(currency.toUpperCase(Locale.ROOT)) ? 0 : 2;
    }

    /** 最小单位 → 渠道金额字符串（JPY: 1234→"1234"；USD: 1234→"12.34"）。 */
    public static String display(long minorUnits, String currency) {
        return CurrencyUtil.display(minorUnits, decimalDigits(currency));
    }
}
