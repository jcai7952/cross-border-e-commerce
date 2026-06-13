package com.kinn.shop.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付配置：回跳基地址 + 渠道开关/密钥（环境变量注入，密钥不入库不入码）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "shop.pay")
public class PayProperties {

    /** 支付完成后跳回商城前端的基地址 */
    private String returnBase;

    /** 网关地址（渠道回调/回跳用） */
    private String apiBase;

    private Stripe stripe = new Stripe();

    private Paypal paypal = new Paypal();

    @Data
    public static class Stripe {
        private boolean enabled;
        private String apiKey;
        private String webhookSecret;
        private String publishableKey;
    }

    @Data
    public static class Paypal {
        private boolean enabled;
        private String clientId;
        private String clientSecret;
        private String baseUrl;
    }
}
