package com.kinn.shop.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinn.shop.api.product.ProductTradeFacade;
import com.kinn.shop.api.product.dto.RateDTO;
import com.kinn.shop.api.product.dto.StockOpDTO;
import com.kinn.shop.api.user.UserFacade;
import com.kinn.shop.api.user.dto.IdentityDTO;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.util.CurrencyUtil;
import com.kinn.shop.order.dto.OrderCreateDTO;
import com.kinn.shop.order.entity.CartItem;
import com.kinn.shop.order.entity.OrderItem;
import com.kinn.shop.order.entity.OrderStatusLog;
import com.kinn.shop.order.entity.Orders;
import com.kinn.shop.order.entity.UserCoupon;
import com.kinn.shop.order.mapper.CartItemMapper;
import com.kinn.shop.order.mapper.OrderItemMapper;
import com.kinn.shop.order.mapper.OrderStatusLogMapper;
import com.kinn.shop.order.mapper.OrdersMapper;
import com.kinn.shop.order.mapper.UserCouponMapper;
import com.kinn.shop.order.vo.MoneyVO;
import com.kinn.shop.order.vo.OrderCreateVO;
import com.kinn.shop.order.vo.OrderDetailVO;
import com.kinn.shop.order.vo.OrderItemVO;
import com.kinn.shop.order.vo.OrderPageVO;
import com.kinn.shop.order.vo.StatusLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 下单/订单查询/取消/超时关单。
 * 下单顺序：全量重算 → 实名校验 → 锁汇率 → RPC 扣库存 → 本地事务（用券+落单+日志+清购物车），
 * 本地事务失败补偿回滚库存（RPC 扣库存不在本地事务内，避免长事务跨服务）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int PAY_TIMEOUT_MINUTES = 30;

    private final CheckoutService checkoutService;
    private final PriceService priceService;
    private final OrderStatusService orderStatusService;
    private final UserFacade userFacade;
    private final ProductTradeFacade productTradeFacade;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final UserCouponMapper userCouponMapper;
    private final CartItemMapper cartItemMapper;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    // ---------------- 下单 ----------------

    public OrderCreateVO create(OrderCreateDTO dto) {
        long userId = LoginContext.requireUserId();
        String locale = (dto.getLocale() == null || dto.getLocale().isBlank()) ? "en-US" : dto.getLocale();
        // 1) 与 preview 同链路全量重算（不信任前端金额）
        CheckoutService.CheckoutCalc calc = checkoutService.calculate(userId, dto.getAddressId(),
                dto.getItems(), Boolean.TRUE.equals(dto.getFromCart()), dto.getUserCouponId(), locale);
        // 2) 目的国要求实名时必须能取到实名快照
        IdentityDTO identity = null;
        if (calc.tax().identityRequired()) {
            identity = userFacade.getIdentity(userId, dto.getIdentityId());
            if (identity == null) {
                throw new BizException(ErrorCode.IDENTITY_REQUIRED);
            }
        }
        // 3) 锁定汇率并折算支付币应付
        RateDTO rate = priceService.resolve(dto.getPayCurrency());
        long payAmount = CurrencyUtil.convert(calc.totalCents(), rate.getRate(), rate.getDecimalDigits());

        String orderNo = generateOrderNo();
        LocalDateTime payDeadline = LocalDateTime.now().plusMinutes(PAY_TIMEOUT_MINUTES);
        List<StockOpDTO> ops = calc.lines().stream()
                .map(l -> new StockOpDTO(l.sku().getSkuId(), l.sku().getProductId(), l.quantity()))
                .toList();
        // 4) RPC 扣库存（DB 乐观锁，任一不足整体失败）
        if (!productTradeFacade.deductStock(ops)) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        // 5) 本地事务落单；失败补偿回滚库存
        try {
            IdentityDTO identitySnapshot = identity;
            transactionTemplate.executeWithoutResult(txStatus ->
                    persistOrder(userId, dto, locale, calc, rate, identitySnapshot, orderNo, payAmount, payDeadline));
        } catch (RuntimeException e) {
            log.warn("[order] create failed after stock deducted, restoring. orderNo={}", orderNo, e);
            try {
                productTradeFacade.restoreStock(ops);
            } catch (Exception re) {
                log.error("[order] restoreStock compensation failed, ops={}", ops, re);
            }
            throw e;
        }
        return new OrderCreateVO(orderNo, payAmount, rate.getCurrency(), payDeadline);
    }

    /** 本地事务体：用券（CAS）→ 插订单/明细/状态日志 → fromCart 清勾选项。 */
    private void persistOrder(long userId, OrderCreateDTO dto, String locale, CheckoutService.CheckoutCalc calc,
                              RateDTO rate, IdentityDTO identity, String orderNo,
                              long payAmount, LocalDateTime payDeadline) {
        // 用券：仅未用状态可核销，行数 0 即被并发用掉
        if (calc.userCoupon() != null) {
            int rows = userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                    .eq(UserCoupon::getId, calc.userCoupon().getId())
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getStatus, 0)
                    .set(UserCoupon::getStatus, 1)
                    .set(UserCoupon::getOrderNo, orderNo)
                    .set(UserCoupon::getUsedAt, LocalDateTime.now()));
            if (rows == 0) {
                throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE);
            }
        }
        Orders order = new Orders();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setStatus(OrderStatus.WAIT_PAY.name());
        order.setTradeMode(calc.tradeMode());
        order.setGoodsAmountCents(calc.goodsCents());
        order.setShippingAmountCents(calc.quote().getFeeCents());
        order.setTaxAmountCents(calc.tax().taxCents());
        order.setDiscountAmountCents(calc.discountCents());
        order.setTotalAmountCents(calc.totalCents());
        order.setPayCurrency(rate.getCurrency());
        order.setExchangeRate(rate.getRate());
        order.setPayAmountCents(payAmount);
        order.setUserCouponId(calc.userCoupon() == null ? null : calc.userCoupon().getId());
        order.setLocale(locale);
        order.setReceiverJson(toJson(calc.address()));
        order.setIdentityJson(identity == null ? null
                : toJson(Map.of("realName", identity.getRealName(), "idCardMask", identity.getIdCardMask())));
        order.setRemark(dto.getRemark());
        order.setPayDeadline(payDeadline);
        ordersMapper.insert(order);

        for (CheckoutService.Line line : calc.lines()) {
            OrderItem item = new OrderItem();
            item.setOrderNo(orderNo);
            item.setProductId(line.sku().getProductId());
            item.setSkuId(line.sku().getSkuId());
            item.setProductName(line.sku().getProductName());
            item.setSkuText(line.sku().getSkuText());
            item.setImage(line.sku().getImage());
            item.setPriceCents(line.sku().getPriceCents());
            item.setQuantity(line.quantity());
            item.setTotalCents(line.lineTotal());
            orderItemMapper.insert(item);
        }
        orderStatusService.log(orderNo, "CREATE", OrderStatus.WAIT_PAY.name(), "user:" + userId, null);

        if (Boolean.TRUE.equals(dto.getFromCart())) {
            cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getChecked, 1));
        }
    }

    // ---------------- 查询 ----------------

    public PageResult<OrderPageVO> page(String status, long pageNum, long pageSize, String currency, String locale) {
        long userId = LoginContext.requireUserId();
        long pn = Math.max(pageNum, 1);
        long ps = Math.min(Math.max(pageSize, 1), 100);
        Page<Orders> page = ordersMapper.selectPage(new Page<>(pn, ps), Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getUserId, userId)
                .eq(status != null && !status.isBlank(), Orders::getStatus, status)
                .orderByDesc(Orders::getId));
        if (page.getRecords().isEmpty()) {
            return PageResult.of(page.getTotal(), pn, ps, List.of());
        }
        Map<String, List<OrderItem>> itemMap = itemsByOrderNos(
                page.getRecords().stream().map(Orders::getOrderNo).toList());
        // 同币种的元数据（symbol/小数位）一次解析复用
        Map<String, RateDTO> metaCache = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        List<OrderPageVO> list = page.getRecords().stream().map(order -> {
            RateDTO meta = metaCache.computeIfAbsent(order.getPayCurrency(), priceService::metaOrFallback);
            OrderPageVO vo = new OrderPageVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setStatus(order.getStatus());
            vo.setTradeMode(order.getTradeMode());
            vo.setTotalUsdCents(order.getTotalAmountCents());
            // 支付币金额下单时已按锁定汇率落库，直接展示
            vo.setTotalDisplay(priceService.buildMinor(order.getPayAmountCents(), meta));
            vo.setPayCurrency(order.getPayCurrency());
            vo.setCountdownSeconds(countdown(order, now));
            vo.setPayDeadline(order.getPayDeadline());
            vo.setCreateTime(order.getCreateTime());
            vo.setItems(itemMap.getOrDefault(order.getOrderNo(), List.of())
                    .stream().map(OrderItemVO::from).toList());
            return vo;
        }).toList();
        return PageResult.of(page.getTotal(), pn, ps, list);
    }

    public OrderDetailVO detail(String orderNo, String currency, String locale) {
        long userId = LoginContext.requireUserId();
        Orders order = requireOwned(userId, orderNo);
        RateDTO meta = priceService.metaOrFallback(order.getPayCurrency());

        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setTradeMode(order.getTradeMode());
        vo.setLocale(order.getLocale());
        vo.setGoods(lockedMoney(order.getGoodsAmountCents(), order, meta));
        vo.setDiscount(lockedMoney(order.getDiscountAmountCents(), order, meta));
        vo.setShipping(lockedMoney(order.getShippingAmountCents(), order, meta));
        vo.setTax(lockedMoney(order.getTaxAmountCents(), order, meta));
        vo.setTotal(new MoneyVO(order.getTotalAmountCents(), priceService.buildMinor(order.getPayAmountCents(), meta)));
        vo.setPayCurrency(order.getPayCurrency());
        vo.setExchangeRate(order.getExchangeRate());
        vo.setPayAmountCents(order.getPayAmountCents());
        vo.setUserCouponId(order.getUserCouponId());
        vo.setReceiver(fromJson(order.getReceiverJson()));
        vo.setIdentity(fromJson(order.getIdentityJson()));
        vo.setRemark(order.getRemark());
        vo.setPayDeadline(order.getPayDeadline());
        vo.setCountdownSeconds(countdown(order, LocalDateTime.now()));
        vo.setPaidAt(order.getPaidAt());
        vo.setShippedAt(order.getShippedAt());
        vo.setFinishedAt(order.getFinishedAt());
        vo.setClosedAt(order.getClosedAt());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderNo, orderNo).orderByAsc(OrderItem::getId))
                .stream().map(OrderItemVO::from).toList());
        vo.setLogs(statusLogMapper.selectList(Wrappers.<OrderStatusLog>lambdaQuery()
                        .eq(OrderStatusLog::getOrderNo, orderNo).orderByAsc(OrderStatusLog::getId))
                .stream().map(StatusLogVO::from).toList());
        return vo;
    }

    // ---------------- 取消 / 超时关单 ----------------

    /** 用户取消：仅 WAIT_PAY；CAS 关单成功后回滚库存、退券。 */
    public void cancel(String orderNo) {
        long userId = LoginContext.requireUserId();
        Orders order = requireOwned(userId, orderNo);
        if (!OrderStatus.WAIT_PAY.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
        orderStatusService.transit(orderNo, OrderStatus.WAIT_PAY, OrderStatus.CLOSED,
                "user:" + userId, "用户取消");
        releaseResources(order);
    }

    /**
     * 超时关单（Job 调用，外层已持有分布式锁）：CAS 关单成功才回滚库存/退券；
     * 已被支付回调抢先转 PAID 的 CAS 失败直接跳过。
     */
    public void closeTimeoutOrder(String orderNo) {
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery().eq(Orders::getOrderNo, orderNo));
        if (order == null || !OrderStatus.WAIT_PAY.name().equals(order.getStatus())) {
            return;
        }
        boolean closed = orderStatusService.tryTransit(orderNo, OrderStatus.WAIT_PAY, OrderStatus.CLOSED,
                "system", "支付超时自动关单");
        if (closed) {
            releaseResources(order);
            log.info("[order] timeout closed, orderNo={}", orderNo);
        }
    }

    /** 关单后的资源释放：回滚库存 + 退券（status 1→0，清 order_no/used_at）。 */
    private void releaseResources(Orders order) {
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderNo, order.getOrderNo()));
        List<StockOpDTO> ops = items.stream()
                .map(i -> new StockOpDTO(i.getSkuId(), i.getProductId(), i.getQuantity()))
                .toList();
        try {
            productTradeFacade.restoreStock(ops);
        } catch (Exception e) {
            log.error("[order] restoreStock failed on close, orderNo={}", order.getOrderNo(), e);
        }
        if (order.getUserCouponId() != null) {
            userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                    .eq(UserCoupon::getId, order.getUserCouponId())
                    .eq(UserCoupon::getStatus, 1)
                    .set(UserCoupon::getStatus, 0)
                    .set(UserCoupon::getOrderNo, null)
                    .set(UserCoupon::getUsedAt, null));
        }
    }

    // ---------------- 工具 ----------------

    public Map<String, List<OrderItem>> itemsByOrderNos(List<String> orderNos) {
        if (orderNos.isEmpty()) {
            return Map.of();
        }
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .in(OrderItem::getOrderNo, orderNos).orderByAsc(OrderItem::getId))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderNo));
    }

    private Orders requireOwned(long userId, String orderNo) {
        Orders order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery().eq(Orders::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private MoneyVO lockedMoney(long usdCents, Orders order, RateDTO meta) {
        return new MoneyVO(usdCents, priceService.buildLocked(usdCents, order.getExchangeRate(), meta));
    }

    private Long countdown(Orders order, LocalDateTime now) {
        if (!OrderStatus.WAIT_PAY.name().equals(order.getStatus()) || order.getPayDeadline() == null) {
            return null;
        }
        long seconds = Duration.between(now, order.getPayDeadline()).getSeconds();
        return Math.max(seconds, 0);
    }

    /** O + 时间戳(17位) + 4位随机数字。 */
    private String generateOrderNo() {
        return "O" + LocalDateTime.now().format(ORDER_NO_FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("[order] snapshot json parse failed: {}", e.getMessage());
            return null;
        }
    }
}
