package com.kinn.shop.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinn.shop.api.logistics.LogisticsFacade;
import com.kinn.shop.api.logistics.dto.ShipmentCreateDTO;
import com.kinn.shop.api.product.ProductTradeFacade;
import com.kinn.shop.api.product.dto.SkuTradeDTO;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.order.entity.OrderItem;
import com.kinn.shop.order.entity.Orders;
import com.kinn.shop.order.mapper.OrderItemMapper;
import com.kinn.shop.order.mapper.OrdersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单动作：管理端发货（PAID→SHIPPED + 物流建单）、买家确认收货（SHIPPED→FINISHED）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderActionService {

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusService orderStatusService;
    private final ProductTradeFacade productTradeFacade;
    private final LogisticsFacade logisticsFacade;
    private final ObjectMapper objectMapper;

    /** 管理端发货：物流建单（幂等）成功后 CAS 流转。返回 shipmentNo。 */
    public String ship(String orderNo) {
        long adminId = LoginContext.requireAdminId();
        Orders order = requireOrder(orderNo);
        if (!OrderStatus.PAID.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderNo, orderNo));

        ShipmentCreateDTO dto = new ShipmentCreateDTO();
        dto.setOrderNo(orderNo);
        dto.setUserId(order.getUserId());
        dto.setCountryCode(countryOf(order));
        dto.setReceiverJson(order.getReceiverJson());
        dto.setWeightGrams(totalWeight(items));
        dto.setFeeCents(order.getShippingAmountCents());

        String shipmentNo = logisticsFacade.createShipment(dto);
        if (shipmentNo == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "物流建单失败，请稍后重试");
        }
        orderStatusService.transit(orderNo, OrderStatus.PAID, OrderStatus.SHIPPED,
                "admin:" + adminId, "ship:" + shipmentNo);
        return shipmentNo;
    }

    /** 买家确认收货。 */
    public void confirm(String orderNo) {
        long userId = LoginContext.requireUserId();
        Orders order = requireOrder(orderNo);
        if (!Long.valueOf(userId).equals(order.getUserId())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        orderStatusService.transit(orderNo, OrderStatus.SHIPPED, OrderStatus.FINISHED,
                "user:" + userId, "confirm-receipt");
    }

    /** 实时取 SKU 重量合计；商品服务不可达时按 300g/件 兜底（仅影响物流单记录值）。 */
    private int totalWeight(List<OrderItem> items) {
        try {
            Map<Long, SkuTradeDTO> skuMap = productTradeFacade
                    .getSkusForTrade(items.stream().map(OrderItem::getSkuId).toList(), "en-US")
                    .stream().collect(Collectors.toMap(SkuTradeDTO::getSkuId, Function.identity()));
            return items.stream().mapToInt(i -> {
                SkuTradeDTO sku = skuMap.get(i.getSkuId());
                int unit = (sku == null || sku.getWeightGrams() <= 0) ? 300 : sku.getWeightGrams();
                return unit * i.getQuantity();
            }).sum();
        } catch (Exception e) {
            log.warn("[order] weight lookup failed, fallback 300g/item: {}", e.getMessage());
            return items.stream().mapToInt(i -> 300 * i.getQuantity()).sum();
        }
    }

    private String countryOf(Orders order) {
        try {
            JsonNode node = objectMapper.readTree(order.getReceiverJson());
            JsonNode cc = node.get("countryCode");
            if (cc != null && !cc.asText().isBlank()) {
                return cc.asText();
            }
        } catch (Exception ignore) {
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR, "订单收件快照缺少国家信息");
    }

    private Orders requireOrder(String orderNo) {
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery().eq(Orders::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }
}
