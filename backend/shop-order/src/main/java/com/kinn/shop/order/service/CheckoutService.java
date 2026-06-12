package com.kinn.shop.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.api.logistics.LogisticsFacade;
import com.kinn.shop.api.logistics.dto.ShippingQuoteDTO;
import com.kinn.shop.api.product.ProductTradeFacade;
import com.kinn.shop.api.product.dto.RateDTO;
import com.kinn.shop.api.product.dto.SkuTradeDTO;
import com.kinn.shop.api.user.UserFacade;
import com.kinn.shop.api.user.dto.AddressDTO;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.order.dto.CheckoutItemDTO;
import com.kinn.shop.order.dto.CheckoutPreviewDTO;
import com.kinn.shop.order.entity.CartItem;
import com.kinn.shop.order.entity.Coupon;
import com.kinn.shop.order.entity.UserCoupon;
import com.kinn.shop.order.mapper.CartItemMapper;
import com.kinn.shop.order.mapper.CouponMapper;
import com.kinn.shop.order.mapper.UserCouponMapper;
import com.kinn.shop.order.vo.CheckoutCouponVO;
import com.kinn.shop.order.vo.CheckoutLineVO;
import com.kinn.shop.order.vo.CheckoutPreviewVO;
import com.kinn.shop.order.vo.MoneyVO;
import com.kinn.shop.order.vo.RateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 结算计算核心：preview 与 create 共用同一条重算链路（金额永远不信任前端）。
 * 链路：地址 → 明细（直传/购物车勾选）→ SKU 校验 → 商品金额 → 券 → 运费 → 税 → 应付。
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final UserFacade userFacade;
    private final ProductTradeFacade productTradeFacade;
    private final LogisticsFacade logisticsFacade;
    private final CartItemMapper cartItemMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponMapper couponMapper;
    private final TaxService taxService;
    private final PriceService priceService;

    /** 明细行：SKU 快照 + 数量 + 小计（USD 分）。 */
    public record Line(SkuTradeDTO sku, int quantity, long lineTotal) {
    }

    /** 一次结算重算的全部产物。 */
    public record CheckoutCalc(AddressDTO address, List<Line> lines, long goodsCents, long discountCents,
                               UserCoupon userCoupon, ShippingQuoteDTO quote, TaxService.TaxResult tax,
                               long totalCents, String tradeMode) {
    }

    public CheckoutPreviewVO preview(CheckoutPreviewDTO dto) {
        long userId = LoginContext.requireUserId();
        CheckoutCalc calc = calculate(userId, dto.getAddressId(), dto.getItems(),
                Boolean.TRUE.equals(dto.getFromCart()), dto.getUserCouponId(), dto.getLocale());
        RateDTO rate = priceService.resolve(dto.getCurrency());

        CheckoutPreviewVO vo = new CheckoutPreviewVO();
        vo.setItems(calc.lines().stream().map(line -> {
            CheckoutLineVO lv = new CheckoutLineVO();
            lv.setSkuId(line.sku().getSkuId());
            lv.setProductId(line.sku().getProductId());
            lv.setName(line.sku().getProductName());
            lv.setSkuText(line.sku().getSkuText());
            lv.setImage(line.sku().getImage());
            lv.setTradeMode(line.sku().getTradeMode());
            lv.setQuantity(line.quantity());
            lv.setUnitPriceUsdCents(line.sku().getPriceCents());
            lv.setLineTotalUsdCents(line.lineTotal());
            lv.setUnitPrice(priceService.build(line.sku().getPriceCents(), rate));
            lv.setLineTotal(priceService.build(line.lineTotal(), rate));
            return lv;
        }).toList());
        vo.setGoods(money(calc.goodsCents(), rate));
        vo.setDiscount(money(calc.discountCents(), rate));
        vo.setShipping(money(calc.quote().getFeeCents(), rate));
        vo.setTax(money(calc.tax().taxCents(), rate));
        vo.setTotal(money(calc.totalCents(), rate));
        vo.setIdentityRequired(calc.tax().identityRequired());
        vo.setTaxNote(calc.tax().taxNote());
        vo.setEstDaysMin(calc.quote().getEstDaysMin());
        vo.setEstDaysMax(calc.quote().getEstDaysMax());
        vo.setRate(new RateVO(rate.getCurrency(), rate.getSymbol(), rate.getDecimalDigits(), rate.getRate()));
        vo.setAvailableCoupons(availableCoupons(userId, calc.goodsCents()));
        return vo;
    }

    /**
     * 全量重算（preview/create 共用）。
     *
     * @param itemsParam 直传明细（立即购买）；为空且 fromCart=true 时取购物车勾选项
     */
    public CheckoutCalc calculate(long userId, Long addressId, List<CheckoutItemDTO> itemsParam,
                                  boolean fromCart, Long userCouponId, String locale) {
        // 1) 地址
        AddressDTO address = userFacade.getAddress(userId, addressId);
        if (address == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "地址不存在");
        }
        // 2) 明细来源 + SKU 校验（全部上架且库存充足）
        Map<Long, Integer> skuQty = resolveSkuQuantities(userId, itemsParam, fromCart);
        Map<Long, SkuTradeDTO> skuMap = productTradeFacade
                .getSkusForTrade(new ArrayList<>(skuQty.keySet()), locale).stream()
                .collect(Collectors.toMap(SkuTradeDTO::getSkuId, Function.identity()));
        List<Line> lines = new ArrayList<>(skuQty.size());
        long goods = 0;
        long weight = 0;
        for (Map.Entry<Long, Integer> e : skuQty.entrySet()) {
            SkuTradeDTO sku = skuMap.get(e.getKey());
            if (sku == null || sku.getStatus() != 1) {
                throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (sku.getStock() < e.getValue()) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
            }
            long lineTotal = sku.getPriceCents() * e.getValue();
            lines.add(new Line(sku, e.getValue(), lineTotal));
            goods += lineTotal;
            weight += (long) sku.getWeightGrams() * e.getValue();
        }
        // 3) 券
        long discount = 0;
        UserCoupon userCoupon = null;
        if (userCouponId != null) {
            userCoupon = userCouponMapper.selectById(userCouponId);
            discount = validateCouponAndDiscount(userId, userCoupon, goods);
        }
        // 4) 运费
        ShippingQuoteDTO quote = logisticsFacade.quote(address.getCountryCode(), Math.toIntExact(weight));
        if (quote == null) {
            throw new BizException(ErrorCode.SHIPPING_ZONE_NOT_COVERED);
        }
        // 5) 税
        List<TaxService.TaxItem> taxItems = lines.stream()
                .map(l -> new TaxService.TaxItem(l.sku().getTradeMode(), l.sku().getPostalTaxRate(), l.lineTotal()))
                .toList();
        TaxService.TaxResult tax = taxService.calculate(address.getCountryCode(), taxItems,
                goods - discount, quote.getFeeCents());
        // 6) 应付
        long total = goods - discount + quote.getFeeCents() + tax.taxCents();
        return new CheckoutCalc(address, lines, goods, discount, userCoupon, quote, tax, total, tradeMode(lines));
    }

    /** 明细来源：直传优先；否则取购物车勾选项。重复 skuId 合并数量。 */
    private Map<Long, Integer> resolveSkuQuantities(long userId, List<CheckoutItemDTO> items, boolean fromCart) {
        Map<Long, Integer> skuQty = new LinkedHashMap<>();
        if (items != null && !items.isEmpty()) {
            for (CheckoutItemDTO item : items) {
                skuQty.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
            }
        } else if (fromCart) {
            List<CartItem> cartItems = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getChecked, 1)
                    .orderByAsc(CartItem::getId));
            for (CartItem item : cartItems) {
                skuQty.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
            }
        }
        if (skuQty.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "无可结算商品");
        }
        return skuQty;
    }

    /** 券校验：归属/未用/券启用/有效期内/满足门槛；返回抵扣金额（USD 分）。 */
    private long validateCouponAndDiscount(long userId, UserCoupon userCoupon, long goodsCents) {
        if (userCoupon == null || !userCoupon.getUserId().equals(userId) || userCoupon.getStatus() != 0) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        LocalDateTime now = LocalDateTime.now();
        if (coupon == null || coupon.getStatus() != 1
                || now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())
                || coupon.getMinAmountCents() > goodsCents) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        return discountOf(coupon, goodsCents);
    }

    /** FIXED 取 min(value, goods)；PERCENT 取 goods × value / 100（整除取整）。 */
    public static long discountOf(Coupon coupon, long goodsCents) {
        if ("FIXED".equals(coupon.getType())) {
            return Math.min(coupon.getValue(), goodsCents);
        }
        return goodsCents * coupon.getValue() / 100;
    }

    /** 明细含 DIRECT 且含 BONDED → MIXED；全 DIRECT → DIRECT；其余 → BONDED。 */
    private String tradeMode(List<Line> lines) {
        boolean hasDirect = lines.stream().anyMatch(l -> "DIRECT".equals(l.sku().getTradeMode()));
        boolean hasBonded = lines.stream().anyMatch(l -> !"DIRECT".equals(l.sku().getTradeMode()));
        if (hasDirect && hasBonded) {
            return "MIXED";
        }
        return hasDirect ? "DIRECT" : "BONDED";
    }

    /** 我的未用券中满足本单门槛且在有效期内的，附带可抵扣金额。 */
    private List<CheckoutCouponVO> availableCoupons(long userId, long goodsCents) {
        List<UserCoupon> mine = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 0)
                .orderByDesc(UserCoupon::getId));
        if (mine.isEmpty()) {
            return List.of();
        }
        Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(
                        mine.stream().map(UserCoupon::getCouponId).distinct().toList()).stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));
        LocalDateTime now = LocalDateTime.now();
        return mine.stream()
                .map(uc -> {
                    Coupon c = couponMap.get(uc.getCouponId());
                    if (c == null || c.getStatus() != 1
                            || now.isBefore(c.getValidFrom()) || now.isAfter(c.getValidTo())
                            || c.getMinAmountCents() > goodsCents) {
                        return null;
                    }
                    CheckoutCouponVO vo = new CheckoutCouponVO();
                    vo.setUserCouponId(uc.getId());
                    vo.setCouponId(c.getId());
                    vo.setTitle(c.getTitle());
                    vo.setType(c.getType());
                    vo.setValue(c.getValue());
                    vo.setMinAmountCents(c.getMinAmountCents());
                    vo.setValidTo(c.getValidTo());
                    vo.setDiscountCents(discountOf(c, goodsCents));
                    return vo;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private MoneyVO money(long usdCents, RateDTO rate) {
        return new MoneyVO(usdCents, priceService.build(usdCents, rate));
    }
}
