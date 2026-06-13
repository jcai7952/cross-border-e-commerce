package com.kinn.shop.payment.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 单号生成：前缀 + yyyyMMddHHmmssSSS + 4 位随机数（uk 兜底防撞）。
 */
public final class PayNos {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private PayNos() {
    }

    public static String gen(String prefix) {
        return prefix + TS.format(LocalDateTime.now())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
