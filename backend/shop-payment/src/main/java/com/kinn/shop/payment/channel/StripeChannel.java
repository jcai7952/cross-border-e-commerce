package com.kinn.shop.payment.channel;

import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.payment.config.PayProperties;
import com.kinn.shop.payment.entity.PayOrder;
import com.kinn.shop.payment.entity.RefundOrder;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Stripe 渠道：PaymentIntent（automatic_payment_methods），前端用 clientSecret 走 Stripe.js。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "shop.pay.stripe", name = "enabled", havingValue = "true")
public class StripeChannel implements PayChannel {

    public static final String CODE = "STRIPE";

    private final PayProperties payProperties;

    public StripeChannel(PayProperties payProperties) {
        this.payProperties = payProperties;
        Stripe.apiKey = payProperties.getStripe().getApiKey();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ChannelCreateResult create(PayOrder po) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(po.getAmountCents())
                    .setCurrency(po.getCurrency().toLowerCase(Locale.ROOT))
                    .putMetadata("payNo", po.getPayNo())
                    .putMetadata("orderNo", po.getOrderNo())
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build())
                    .build();
            PaymentIntent pi = PaymentIntent.create(params);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("clientSecret", pi.getClientSecret());
            payload.put("publishableKey", payProperties.getStripe().getPublishableKey());
            return new ChannelCreateResult(pi.getId(), PayloadType.CLIENT_SECRET, payload);
        } catch (StripeException e) {
            log.error("[stripe] create PaymentIntent failed, payNo={}", po.getPayNo(), e);
            throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
        }
    }

    @Override
    public ChannelRefundResult refund(RefundOrder ro, PayOrder po) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(po.getChannelTradeNo())
                    .setAmount(ro.getAmountCents())
                    .build();
            Refund refund = Refund.create(params);
            ChannelStatus status = switch (refund.getStatus() == null ? "" : refund.getStatus()) {
                case "succeeded" -> ChannelStatus.SUCCESS;
                case "failed", "canceled" -> ChannelStatus.FAILED;
                default -> ChannelStatus.PROCESSING; // pending / requires_action
            };
            return new ChannelRefundResult(status, refund.getId());
        } catch (StripeException e) {
            log.error("[stripe] refund failed, refundNo={}", ro.getRefundNo(), e);
            throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
        }
    }

    @Override
    public ChannelQueryResult query(PayOrder po) {
        if (po.getChannelTradeNo() == null || po.getChannelTradeNo().isBlank()) {
            return new ChannelQueryResult(ChannelStatus.PENDING, null);
        }
        try {
            PaymentIntent pi = PaymentIntent.retrieve(po.getChannelTradeNo());
            ChannelStatus status = switch (pi.getStatus() == null ? "" : pi.getStatus()) {
                case "succeeded" -> ChannelStatus.SUCCESS;
                case "canceled" -> ChannelStatus.FAILED;
                default -> ChannelStatus.PENDING;
            };
            return new ChannelQueryResult(status, pi.getId());
        } catch (StripeException e) {
            log.error("[stripe] query PaymentIntent failed, payNo={}", po.getPayNo(), e);
            throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
        }
    }
}
