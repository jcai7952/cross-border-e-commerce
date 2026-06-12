package com.kinn.shop.common.constant;

/**
 * Redis key 规范：shop:{域}:{用途}:{标识}
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 邮箱验证码：shop:user:email-code:{scene}:{email} -> 6位码，TTL 5min */
    public static String emailCode(String scene, String email) {
        return "shop:user:email-code:" + scene + ":" + email;
    }

    /** 邮箱验证码发送频控：TTL 60s */
    public static String emailCodeLimit(String scene, String email) {
        return "shop:user:email-code-limit:" + scene + ":" + email;
    }

    /** SKU 库存预扣：shop:stock:sku:{skuId} */
    public static String skuStock(long skuId) {
        return "shop:stock:sku:" + skuId;
    }

    /** 支付回调幂等：shop:pay:notify:{channel}:{eventId}，SETNX TTL 24h */
    public static String payNotify(String channel, String eventId) {
        return "shop:pay:notify:" + channel + ":" + eventId;
    }

    /** 汇率缓存：shop:currency:rates (hash quote->rate) */
    public static final String EXCHANGE_RATES = "shop:currency:rates";

    /** 分布式锁前缀 */
    public static String lock(String biz, String key) {
        return "shop:lock:" + biz + ":" + key;
    }
}
