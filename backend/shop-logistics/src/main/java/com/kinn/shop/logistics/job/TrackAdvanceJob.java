package com.kinn.shop.logistics.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.api.order.OrderFacade;
import com.kinn.shop.logistics.entity.LogisticsOrder;
import com.kinn.shop.logistics.mapper.LogisticsOrderMapper;
import com.kinn.shop.logistics.service.TrackAdvanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 轨迹推进 Job：每轮取一批在途物流单，各推进一个节点，模拟跨境履约全链路。
 * 推进到 SIGNED 时通知订单服务完成订单；RPC 失败仅告警不回滚——
 * 下一轮 Job 不再处理已 SIGNED 单，订单完成兜底靠买家手动确认收货。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackAdvanceJob {

    private final LogisticsOrderMapper logisticsOrderMapper;
    private final TrackAdvanceService trackAdvanceService;
    private final OrderFacade orderFacade;

    @Scheduled(fixedDelayString = "${shop.logistics.advance-interval-ms:30000}")
    public void advance() {
        List<LogisticsOrder> batch = logisticsOrderMapper.selectList(Wrappers.<LogisticsOrder>lambdaQuery()
                .select(LogisticsOrder::getId)
                .eq(LogisticsOrder::getStatus, "IN_TRANSIT")
                .orderByAsc(LogisticsOrder::getId)
                .last("LIMIT 100"));
        for (LogisticsOrder row : batch) {
            try {
                String signedOrderNo = trackAdvanceService.advanceOne(row.getId());
                if (signedOrderNo != null) {
                    notifyOrderFinished(signedOrderNo);
                }
            } catch (Exception e) {
                log.error("[track-job] advance failed, logisticsOrderId={}", row.getId(), e);
            }
        }
    }

    private void notifyOrderFinished(String orderNo) {
        try {
            boolean ok = orderFacade.markFinished(orderNo);
            if (!ok) {
                log.warn("[track-job] markFinished returned false, orderNo={}", orderNo);
            }
        } catch (Exception e) {
            // 物流单已 SIGNED，不回滚；订单完成兜底靠买家手动确认收货
            log.warn("[track-job] markFinished rpc failed, orderNo={}: {}", orderNo, e.getMessage());
        }
    }
}
