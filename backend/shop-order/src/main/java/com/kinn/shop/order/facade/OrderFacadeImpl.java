package com.kinn.shop.order.facade;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.api.order.OrderFacade;
import com.kinn.shop.api.order.dto.OrderPayInfoDTO;
import com.kinn.shop.api.product.ProductTradeFacade;
import com.kinn.shop.api.product.dto.StockOpDTO;
import com.kinn.shop.order.entity.OrderItem;
import com.kinn.shop.order.entity.Orders;
import com.kinn.shop.order.mapper.OrderItemMapper;
import com.kinn.shop.order.mapper.OrdersMapper;
import com.kinn.shop.order.service.OrderStatus;
import com.kinn.shop.order.service.OrderStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * 订单服务对内 RPC：支付服务驱动订单状态。
 * markPaid/markRefunded 设计为幂等——支付回调可能重复投递。
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class OrderFacadeImpl implements OrderFacade {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusService orderStatusService;
    private final ProductTradeFacade productTradeFacade;

    @Override
    public OrderPayInfoDTO getPayInfo(String orderNo) {
        Orders order = byNo(orderNo);
        if (order == null) {
            return null;
        }
        OrderPayInfoDTO dto = new OrderPayInfoDTO();
        dto.setOrderNo(order.getOrderNo());
        dto.setUserId(order.getUserId());
        dto.setStatus(order.getStatus());
        dto.setPayCurrency(order.getPayCurrency());
        dto.setPayAmountMinor(order.getPayAmountCents());
        dto.setPayDeadline(order.getPayDeadline());
        return dto;
    }

    @Override
    public boolean markPaid(String orderNo, String payNo, String channel) {
        Orders order = byNo(orderNo);
        if (order == null) {
            return false;
        }
        if (OrderStatus.PAID.name().equals(order.getStatus())
                || OrderStatus.SHIPPED.name().equals(order.getStatus())
                || OrderStatus.FINISHED.name().equals(order.getStatus())) {
            return true; // 幂等：重复回调
        }
        boolean ok = orderStatusService.tryTransit(orderNo, OrderStatus.WAIT_PAY, OrderStatus.PAID,
                "system", "pay:" + channel + ":" + payNo);
        if (!ok) {
            // 可能已被超时关单（极端时序）：调用方应发起退款/人工处理
            log.warn("[order] markPaid failed, order not WAIT_PAY: {} pay={}", orderNo, payNo);
            return false;
        }
        addSalesQuietly(orderNo);
        return true;
    }

    @Override
    public boolean markRefunded(String orderNo, String refundNo) {
        Orders order = byNo(orderNo);
        if (order == null) {
            return false;
        }
        if (OrderStatus.CLOSED.name().equals(order.getStatus())) {
            return true; // 幂等
        }
        boolean ok = orderStatusService.tryTransit(orderNo, OrderStatus.PAID, OrderStatus.CLOSED,
                "system", "refund:" + refundNo);
        if (!ok) {
            log.warn("[order] markRefunded failed, order not PAID: {} refund={}", orderNo, refundNo);
            return false;
        }
        // 未发货全额退款：回滚库存（券不退还）
        try {
            productTradeFacade.restoreStock(stockOps(orderNo));
        } catch (Exception e) {
            log.error("[order] restoreStock after refund failed: {}", orderNo, e);
        }
        return true;
    }

    @Override
    public boolean markFinished(String orderNo) {
        Orders order = byNo(orderNo);
        if (order == null) {
            return false;
        }
        if (OrderStatus.FINISHED.name().equals(order.getStatus())) {
            return true; // 幂等
        }
        boolean ok = orderStatusService.tryTransit(orderNo, OrderStatus.SHIPPED, OrderStatus.FINISHED,
                "system", "signed");
        if (!ok) {
            log.warn("[order] markFinished failed, order not SHIPPED: {}", orderNo);
        }
        return ok;
    }

    @Override
    public List<Long> getReviewableProductIds(String orderNo, long userId) {
        Orders order = byNo(orderNo);
        if (order == null || !Long.valueOf(userId).equals(order.getUserId())
                || !OrderStatus.FINISHED.name().equals(order.getStatus())) {
            return List.of();
        }
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderNo, orderNo)).stream()
                .map(OrderItem::getProductId)
                .distinct()
                .toList();
    }

    private void addSalesQuietly(String orderNo) {
        try {
            productTradeFacade.addSales(stockOps(orderNo));
        } catch (Exception e) {
            log.warn("[order] addSales failed (non-blocking): {}", orderNo, e);
        }
    }

    private List<StockOpDTO> stockOps(String orderNo) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderNo, orderNo)).stream()
                .map(i -> new StockOpDTO(i.getSkuId(), i.getProductId(), i.getQuantity()))
                .toList();
    }

    private Orders byNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return null;
        }
        return ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery().eq(Orders::getOrderNo, orderNo));
    }
}
