package com.kinn.shop.payment.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinn.shop.api.order.OrderFacade;
import com.kinn.shop.payment.entity.LocalMessage;
import com.kinn.shop.payment.mapper.LocalMessageMapper;
import com.kinn.shop.payment.service.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表重试：支付成功但订单服务通知失败的兜底（最终一致性）。
 * 指数退避 2^n 分钟封顶 10 分钟，重试 8 次后转死信告警。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMessageRetryJob {

    private static final int MAX_RETRY = 8;

    private final LocalMessageMapper localMessageMapper;
    private final OrderFacade orderFacade;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 30000)
    public void retry() {
        List<LocalMessage> messages = localMessageMapper.selectList(Wrappers.<LocalMessage>lambdaQuery()
                .eq(LocalMessage::getStatus, 0)
                .le(LocalMessage::getNextRetryTime, LocalDateTime.now())
                .orderByAsc(LocalMessage::getId)
                .last("LIMIT 50"));
        for (LocalMessage message : messages) {
            if (PayService.BIZ_ORDER_MARK_PAID.equals(message.getBizType())) {
                retryMarkPaid(message);
            }
        }
    }

    private void retryMarkPaid(LocalMessage message) {
        boolean ok = false;
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            ok = orderFacade.markPaid(
                    node.path("orderNo").asText(),
                    node.path("payNo").asText(),
                    node.path("channel").asText());
        } catch (Exception e) {
            log.warn("[pay] local message retry failed, id={}: {}", message.getId(), e.getMessage());
        }
        if (ok) {
            localMessageMapper.update(null, Wrappers.<LocalMessage>lambdaUpdate()
                    .eq(LocalMessage::getId, message.getId())
                    .set(LocalMessage::getStatus, 1));
            log.info("[pay] local message delivered, id={}, key={}", message.getId(), message.getBizKey());
            return;
        }
        int retry = message.getRetryCount() == null ? 0 : message.getRetryCount();
        if (retry + 1 >= MAX_RETRY) {
            localMessageMapper.update(null, Wrappers.<LocalMessage>lambdaUpdate()
                    .eq(LocalMessage::getId, message.getId())
                    .set(LocalMessage::getStatus, 2)
                    .set(LocalMessage::getRetryCount, retry + 1));
            log.error("[pay] local message DEAD-LETTER, id={}, key={} — 需人工介入", message.getId(), message.getBizKey());
            return;
        }
        long delayMinutes = Math.min(1L << (retry + 1), 10L);
        localMessageMapper.update(null, Wrappers.<LocalMessage>lambdaUpdate()
                .eq(LocalMessage::getId, message.getId())
                .set(LocalMessage::getRetryCount, retry + 1)
                .set(LocalMessage::getNextRetryTime, LocalDateTime.now().plusMinutes(delayMinutes)));
    }
}
