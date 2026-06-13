package com.kinn.shop.payment.channel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.payment.config.PayProperties;
import com.kinn.shop.payment.entity.PayOrder;
import com.kinn.shop.payment.entity.RefundOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * PayPal 渠道：Spring RestClient 直连 REST API（Orders v2 / Payments v2），不引 SDK。
 * 流程：create order(intent=CAPTURE) → 买家 approve → 回跳 /api/pay/paypal/return → capture 入账。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "shop.pay.paypal", name = "enabled", havingValue = "true")
public class PaypalChannel implements PayChannel {

    public static final String CODE = "PAYPAL";

    private final PayProperties payProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    /** token 缓存：过期前 60s 刷新 */
    private volatile String cachedToken;
    private volatile Instant tokenExpireAt = Instant.EPOCH;

    public PaypalChannel(PayProperties payProperties, ObjectMapper objectMapper) {
        this.payProperties = payProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(payProperties.getPaypal().getBaseUrl()).build();
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ChannelCreateResult create(PayOrder po) {
        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "reference_id", po.getPayNo(),
                        "amount", Map.of(
                                "currency_code", po.getCurrency(),
                                "value", CurrencyMinor.display(po.getAmountCents(), po.getCurrency())))),
                "payment_source", Map.of("paypal", Map.of("experience_context", Map.of(
                        "return_url", payProperties.getApiBase() + "/api/pay/paypal/return?payNo=" + po.getPayNo(),
                        "cancel_url", payProperties.getReturnBase() + "/pay/result?payNo=" + po.getPayNo() + "&cancel=1"))));
        try {
            JsonNode res = restClient.post().uri("/v2/checkout/orders")
                    .headers(h -> h.setBearerAuth(token()))
                    .header("Prefer", "return=representation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String paypalOrderId = res == null ? null : res.path("id").asText(null);
            String approveUrl = res == null ? null : approveLink(res.path("links"));
            if (paypalOrderId == null || approveUrl == null) {
                log.error("[paypal] create order missing id/approve link, payNo={}, res={}", po.getPayNo(), res);
                throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
            }
            return new ChannelCreateResult(paypalOrderId, PayloadType.REDIRECT, Map.of("redirectUrl", approveUrl));
        } catch (RestClientException e) {
            log.error("[paypal] create order failed, payNo={}", po.getPayNo(), e);
            throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
        }
    }

    /** 买家 approve 后 capture（回跳端点调用）。 */
    public CaptureOutcome capture(String paypalOrderId) {
        try {
            JsonNode res = restClient.post().uri("/v2/checkout/orders/{id}/capture", paypalOrderId)
                    .headers(h -> h.setBearerAuth(token()))
                    .header("Prefer", "return=representation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .body(JsonNode.class);
            if (res == null) {
                throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
            }
            String status = res.path("status").asText("");
            String captureId = res.path("purchase_units").path(0)
                    .path("payments").path("captures").path(0).path("id").asText(null);
            return new CaptureOutcome(status, captureId, res.toString());
        } catch (RestClientException e) {
            log.error("[paypal] capture failed, paypalOrderId={}", paypalOrderId, e);
            throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
        }
    }

    @Override
    public ChannelRefundResult refund(RefundOrder ro, PayOrder po) {
        String captureId = captureIdFromPayload(po.getChannelPayload());
        if (captureId == null) {
            throw new BizException(ErrorCode.PAY_STATUS_ILLEGAL, "未找到渠道捕获号(captureId)，无法退款");
        }
        Map<String, Object> body = Map.of("amount", Map.of(
                "currency_code", ro.getCurrency(),
                "value", CurrencyMinor.display(ro.getAmountCents(), ro.getCurrency())));
        try {
            JsonNode res = restClient.post().uri("/v2/payments/captures/{id}/refund", captureId)
                    .headers(h -> h.setBearerAuth(token()))
                    .header("Prefer", "return=representation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String status = res == null ? "" : res.path("status").asText("");
            String refundId = res == null ? null : res.path("id").asText(null);
            ChannelStatus channelStatus = switch (status) {
                case "COMPLETED" -> ChannelStatus.SUCCESS;
                case "PENDING" -> ChannelStatus.PROCESSING;
                default -> ChannelStatus.FAILED;
            };
            return new ChannelRefundResult(channelStatus, refundId);
        } catch (RestClientException e) {
            log.error("[paypal] refund failed, refundNo={}", ro.getRefundNo(), e);
            throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
        }
    }

    @Override
    public ChannelQueryResult query(PayOrder po) {
        if (po.getChannelTradeNo() == null || po.getChannelTradeNo().isBlank()) {
            return new ChannelQueryResult(ChannelStatus.PENDING, null);
        }
        try {
            JsonNode res = restClient.get().uri("/v2/checkout/orders/{id}", po.getChannelTradeNo())
                    .headers(h -> h.setBearerAuth(token()))
                    .retrieve()
                    .body(JsonNode.class);
            String status = res == null ? "" : res.path("status").asText("");
            ChannelStatus channelStatus = switch (status) {
                case "COMPLETED" -> ChannelStatus.SUCCESS;
                case "VOIDED" -> ChannelStatus.FAILED;
                default -> ChannelStatus.PENDING; // CREATED/SAVED/APPROVED/PAYER_ACTION_REQUIRED
            };
            return new ChannelQueryResult(channelStatus, po.getChannelTradeNo());
        } catch (RestClientException e) {
            log.error("[paypal] query order failed, payNo={}", po.getPayNo(), e);
            throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
        }
    }

    private String token() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpireAt)) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null && Instant.now().isBefore(tokenExpireAt)) {
                return cachedToken;
            }
            JsonNode res = restClient.post().uri("/v1/oauth2/token")
                    .headers(h -> h.setBasicAuth(payProperties.getPaypal().getClientId(),
                            payProperties.getPaypal().getClientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(JsonNode.class);
            if (res == null || res.path("access_token").isMissingNode()) {
                throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
            }
            cachedToken = res.path("access_token").asText();
            tokenExpireAt = Instant.now().plusSeconds(Math.max(res.path("expires_in").asLong() - 60, 0));
            return cachedToken;
        }
    }

    private String approveLink(JsonNode links) {
        String approve = null;
        for (JsonNode link : links) {
            String rel = link.path("rel").asText("");
            if ("payer-action".equals(rel)) {
                return link.path("href").asText(null);
            }
            if ("approve".equals(rel)) {
                approve = link.path("href").asText(null);
            }
        }
        return approve;
    }

    /** capture 响应快照里取 captureId（回跳时已存 channel_payload）。 */
    private String captureIdFromPayload(String channelPayload) {
        if (channelPayload == null || channelPayload.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(channelPayload);
            String captureId = node.path("purchase_units").path(0)
                    .path("payments").path("captures").path(0).path("id").asText(null);
            return captureId == null || captureId.isBlank() ? null : captureId;
        } catch (Exception e) {
            log.warn("[paypal] parse channel_payload failed: {}", e.getMessage());
            return null;
        }
    }

    /** capture 结果：status=COMPLETED 时按成功入账，raw 存 channel_payload 供退款取 captureId。 */
    public record CaptureOutcome(String status, String captureId, String raw) {
    }
}
