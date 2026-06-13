package com.kinn.shop.payment.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.payment.channel.CurrencyMinor;
import com.kinn.shop.payment.channel.PaypalChannel;
import com.kinn.shop.payment.channel.SimulatorChannel;
import com.kinn.shop.payment.channel.StripeChannel;
import com.kinn.shop.payment.config.PayProperties;
import com.kinn.shop.payment.dto.SimulatorNotifyDTO;
import com.kinn.shop.payment.entity.PayOrder;
import com.kinn.shop.payment.mapper.PayOrderMapper;
import com.kinn.shop.payment.service.PayService;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 渠道回调入口（网关白名单）：模拟收银台页 / 模拟回调 / Stripe webhook / PayPal 回跳捕获。
 * 全部经由 PayService.settle 统一幂等入账。
 */
@Slf4j
@Tag(name = "支付回调（渠道侧）")
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayNotifyController {

    private final PayService payService;
    private final PayProperties payProperties;
    private final PayOrderMapper payOrderMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<PaypalChannel> paypalChannel;

    /** 模拟收银台页面（演示渠道）。 */
    @Operation(summary = "模拟渠道收银台页")
    @GetMapping(value = "/simulator/{payNo}", produces = MediaType.TEXT_HTML_VALUE)
    public String simulatorPage(@PathVariable String payNo) {
        PayOrder po = payService.byPayNo(payNo);
        if (po == null) {
            return "<html><body><h3>Pay order not found</h3></body></html>";
        }
        String amount = CurrencyMinor.display(po.getAmountCents(), po.getCurrency());
        String resultUrl = payProperties.getReturnBase() + "/pay/result?payNo=" + po.getPayNo();
        return """
                <!DOCTYPE html><html lang="en"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>KinnPay Simulator</title>
                <style>
                  body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;
                       background:#0f172a;font-family:Helvetica,Arial,sans-serif}
                  .card{background:#1e293b;color:#e2e8f0;border-radius:14px;padding:36px 40px;width:340px;
                        box-shadow:0 20px 60px rgba(0,0,0,.5)}
                  h2{margin:0 0 4px;font-size:18px}.muted{color:#94a3b8;font-size:12px;margin-bottom:24px}
                  .amt{font-size:34px;font-weight:800;margin:10px 0 2px}.cur{color:#94a3b8;font-size:13px}
                  .row{color:#94a3b8;font-size:12px;margin:16px 0 24px;word-break:break-all}
                  button{width:100%%;padding:12px;border:0;border-radius:8px;font-size:15px;font-weight:700;
                         cursor:pointer;margin-top:10px}
                  .ok{background:#22c55e;color:#052e16}.fail{background:#334155;color:#cbd5e1}
                  #msg{margin-top:16px;font-size:14px;text-align:center;min-height:20px}
                </style></head><body>
                <div class="card">
                  <h2>KinnPay <span style="color:#38bdf8">Simulator</span></h2>
                  <div class="muted">演示支付渠道 · 不产生真实扣款</div>
                  <div class="amt">%s</div><div class="cur">%s</div>
                  <div class="row">Order: %s<br>PayNo: %s</div>
                  <button class="ok" onclick="pay('SUCCESS')">支付成功 Pay Success</button>
                  <button class="fail" onclick="pay('FAILED')">支付失败 Pay Failed</button>
                  <div id="msg"></div>
                </div>
                <script>
                  async function pay(result){
                    const payNo='%s';
                    document.getElementById('msg').textContent='Processing...';
                    await fetch('/api/pay/notify/simulator',{method:'POST',
                      headers:{'Content-Type':'application/json'},
                      body:JSON.stringify({payNo:payNo,result:result,eventId:'SIMEVT-'+payNo+'-'+result})});
                    document.getElementById('msg').textContent=result==='SUCCESS'?'✓ Paid':'✗ Failed';
                    setTimeout(()=>{location.href='%s'},1500);
                  }
                </script></body></html>
                """.formatted(amount, po.getCurrency(), po.getOrderNo(), po.getPayNo(), po.getPayNo(), resultUrl);
    }

    @Operation(summary = "模拟渠道回调")
    @PostMapping("/notify/simulator")
    public Result<Void> simulatorNotify(@Valid @RequestBody SimulatorNotifyDTO dto) {
        payService.settle(SimulatorChannel.CODE, dto.getEventId(), dto.getPayNo(),
                "SUCCESS".equalsIgnoreCase(dto.getResult()), toJson(dto));
        return Result.ok();
    }

    /** Stripe webhook：验签后按事件入账。本地无公网时走 /api/pay/{payNo}/sync 兜底。 */
    @Operation(summary = "Stripe webhook")
    @PostMapping("/notify/stripe")
    public ResponseEntity<String> stripeNotify(@RequestBody String payload,
                                               @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        String secret = payProperties.getStripe().getWebhookSecret();
        if (!payProperties.getStripe().isEnabled() || secret == null || secret.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("stripe disabled");
        }
        try {
            Webhook.constructEvent(payload, signature, secret); // 验签失败抛异常
        } catch (Exception e) {
            log.warn("[stripe] webhook signature invalid: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature");
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("type").asText("");
            String eventId = root.path("id").asText("");
            String payNo = root.path("data").path("object").path("metadata").path("payNo").asText("");
            if (payNo.isBlank()) {
                return ResponseEntity.ok("ignored: no payNo metadata");
            }
            if ("payment_intent.succeeded".equals(type)) {
                payService.settle(StripeChannel.CODE, eventId, payNo, true, payload);
            } else if ("payment_intent.payment_failed".equals(type)) {
                payService.settle(StripeChannel.CODE, eventId, payNo, false, payload);
            }
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            log.error("[stripe] webhook handle failed", e);
            // 返回 500 让 Stripe 重试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
    }

    /** PayPal 审批完成回跳：capture 捕获即入账，随后跳回商城支付结果页。 */
    @Operation(summary = "PayPal 回跳捕获")
    @GetMapping("/paypal/return")
    public ResponseEntity<Void> paypalReturn(@RequestParam String payNo, @RequestParam String token) {
        String target = payProperties.getReturnBase() + "/pay/result?payNo=" + payNo;
        PaypalChannel channel = paypalChannel.getIfAvailable();
        if (channel == null) {
            return redirect(target + "&fail=1");
        }
        try {
            PaypalChannel.CaptureOutcome outcome = channel.capture(token);
            if ("COMPLETED".equals(outcome.status())) {
                // 退款依赖 captureId，补存到渠道载荷
                mergeCaptureId(payNo, outcome.captureId());
                payService.settle(PaypalChannel.CODE, "PPCAP-" + outcome.captureId(), payNo, true, outcome.raw());
            } else {
                log.warn("[paypal] capture status={} payNo={}", outcome.status(), payNo);
            }
        } catch (Exception e) {
            log.error("[paypal] return capture failed, payNo={}", payNo, e);
            return redirect(target + "&fail=1");
        }
        return redirect(target);
    }

    private void mergeCaptureId(String payNo, String captureId) {
        if (captureId == null) {
            return;
        }
        try {
            PayOrder po = payService.byPayNo(payNo);
            var node = (po == null || po.getChannelPayload() == null || po.getChannelPayload().isBlank())
                    ? objectMapper.createObjectNode()
                    : (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(po.getChannelPayload());
            node.put("captureId", captureId);
            payOrderMapper.update(null, Wrappers.<PayOrder>lambdaUpdate()
                    .eq(PayOrder::getPayNo, payNo)
                    .set(PayOrder::getChannelPayload, node.toString()));
        } catch (Exception e) {
            log.warn("[paypal] merge captureId failed, payNo={}", payNo, e);
        }
    }

    private ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, url).build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
