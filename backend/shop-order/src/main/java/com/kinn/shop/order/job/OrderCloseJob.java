package com.kinn.shop.order.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.common.constant.RedisKeys;
import com.kinn.shop.order.entity.Orders;
import com.kinn.shop.order.mapper.OrdersMapper;
import com.kinn.shop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 超时关单：每分钟扫一批过期未付订单，逐单加 Redisson 分布式锁后 CAS 关单。
 * 与支付回调（WAIT_PAY→PAID）天然竞争：CAS 失败即被抢先支付，直接跳过。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCloseJob {

    private final OrdersMapper ordersMapper;
    private final OrderService orderService;
    private final RedissonClient redissonClient;

    @Scheduled(fixedDelay = 60000)
    public void closeExpiredOrders() {
        List<Orders> expired = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .select(Orders::getOrderNo)
                .eq(Orders::getStatus, "WAIT_PAY")
                .lt(Orders::getPayDeadline, LocalDateTime.now())
                .last("LIMIT 100"));
        for (Orders row : expired) {
            String orderNo = row.getOrderNo();
            RLock lock = redissonClient.getLock(RedisKeys.lock("order-close", orderNo));
            try {
                // 不等待、10s 自动释放：拿不到锁说明别的实例正在处理
                if (!lock.tryLock(0, 10, TimeUnit.SECONDS)) {
                    continue;
                }
                try {
                    orderService.closeTimeoutOrder(orderNo);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("[job] close order failed, orderNo={}", orderNo, e);
            }
        }
    }
}
