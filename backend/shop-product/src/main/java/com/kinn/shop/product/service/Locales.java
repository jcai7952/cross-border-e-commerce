package com.kinn.shop.product.service;

/**
 * 语言参数规范化：仅支持 zh-CN / en-US，其余一律回退 en-US。
 */
public final class Locales {

    public static final String ZH = "zh-CN";
    public static final String EN = "en-US";

    private Locales() {
    }

    public static String normalize(String locale) {
        return ZH.equalsIgnoreCase(locale) ? ZH : EN;
    }

    public static boolean isZh(String locale) {
        return ZH.equalsIgnoreCase(locale);
    }
}
