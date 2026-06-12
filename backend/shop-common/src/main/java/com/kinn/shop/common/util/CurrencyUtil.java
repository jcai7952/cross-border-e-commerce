package com.kinn.shop.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 币种换算：内部金额一律以「基准币 USD 的分」存储（BIGINT），
 * 展示/支付币金额 = USD分 / 100 × 汇率 × 10^目标币小数位，四舍五入为目标币最小单位。
 */
public final class CurrencyUtil {

    private CurrencyUtil() {
    }

    /**
     * USD 分 → 目标币最小单位（如 CNY 分、JPY 円）。
     *
     * @param usdCents      USD 分
     * @param rate          1 USD = rate 目标币
     * @param decimalDigits 目标币小数位（USD/CNY/EUR=2，JPY=0）
     */
    public static long convert(long usdCents, BigDecimal rate, int decimalDigits) {
        return BigDecimal.valueOf(usdCents)
                .divide(BigDecimal.valueOf(100))
                .multiply(rate)
                .movePointRight(decimalDigits)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /** 最小单位 → 展示字符串（如 2599, 2 → "25.99"；155000, 0 → "155000"）。 */
    public static String display(long minorUnits, int decimalDigits) {
        return BigDecimal.valueOf(minorUnits)
                .movePointLeft(decimalDigits)
                .setScale(decimalDigits, RoundingMode.UNNECESSARY)
                .toPlainString();
    }
}
