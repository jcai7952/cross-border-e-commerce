package com.kinn.shop.order.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.order.entity.OrderStatusLog;
import com.kinn.shop.order.entity.Orders;
import com.kinn.shop.order.mapper.OrderStatusLogMapper;
import com.kinn.shop.order.mapper.OrdersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订单状态机执行器：CAS（WHERE status=from）+ version+1 + 按目标态落时间戳 + 流转日志。
 * M3 支付回调走 transit(orderNo, WAIT_PAY, PAID, "system", ...)，本期不暴露 HTTP。
 */
@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private final OrdersMapper ordersMapper;
    private final OrderStatusLogMapper statusLogMapper;

    /** CAS 流转，失败（已被并发流转/状态不符）抛 ORDER_STATUS_ILLEGAL。 */
    @Transactional
    public void transit(String orderNo, OrderStatus from, OrderStatus to, String operator, String remark) {
        if (!tryTransit(orderNo, from, to, operator, remark)) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
    }

    /** CAS 流转，返回是否成功（超时关单 Job 用：被支付回调抢先时静默跳过）。 */
    @Transactional
    public boolean tryTransit(String orderNo, OrderStatus from, OrderStatus to, String operator, String remark) {
        if (!OrderStatus.canTransit(from, to)) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<Orders>()
                .eq(Orders::getOrderNo, orderNo)
                .eq(Orders::getStatus, from.name())
                .set(Orders::getStatus, to.name())
                .setSql("version = version + 1");
        switch (to) {
            case PAID -> wrapper.set(Orders::getPaidAt, now);
            case SHIPPED -> wrapper.set(Orders::getShippedAt, now);
            case FINISHED -> wrapper.set(Orders::getFinishedAt, now);
            case CLOSED -> wrapper.set(Orders::getClosedAt, now);
            default -> { }
        }
        if (ordersMapper.update(null, wrapper) == 0) {
            return false;
        }
        log(orderNo, from.name(), to.name(), operator, remark);
        return true;
    }

    /** 写流转日志（下单 CREATE→WAIT_PAY 也走这里）。 */
    public void log(String orderNo, String from, String to, String operator, String remark) {
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderNo(orderNo);
        statusLog.setFromStatus(from);
        statusLog.setToStatus(to);
        statusLog.setOperator(operator);
        statusLog.setRemark(remark);
        statusLogMapper.insert(statusLog);
    }
}
