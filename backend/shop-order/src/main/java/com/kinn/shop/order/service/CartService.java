package com.kinn.shop.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.api.product.ProductTradeFacade;
import com.kinn.shop.api.product.dto.RateDTO;
import com.kinn.shop.api.product.dto.SkuTradeDTO;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.order.dto.CartAddDTO;
import com.kinn.shop.order.dto.CartUpdateDTO;
import com.kinn.shop.order.entity.CartItem;
import com.kinn.shop.order.mapper.CartItemMapper;
import com.kinn.shop.order.vo.CartItemVO;
import com.kinn.shop.order.vo.CartListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车（需登录）。所有操作校验记录归属，防越权。
 * 单条数量上限 99；价格/库存/上架状态实时取自商品服务，不在本地落价格快照。
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private static final int MAX_QUANTITY = 99;

    private final CartItemMapper cartItemMapper;
    private final ProductTradeFacade productTradeFacade;
    private final PriceService priceService;

    public CartListVO list(String locale, String currency) {
        long userId = LoginContext.requireUserId();
        RateDTO rate = priceService.resolve(currency);
        List<CartItem> rows = cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));
        if (rows.isEmpty()) {
            return new CartListVO(List.of(), 0, priceService.build(0, rate));
        }
        List<Long> skuIds = rows.stream().map(CartItem::getSkuId).toList();
        Map<Long, SkuTradeDTO> skuMap = productTradeFacade.getSkusForTrade(skuIds, locale).stream()
                .collect(Collectors.toMap(SkuTradeDTO::getSkuId, Function.identity()));

        int checkedCount = 0;
        long subtotalCents = 0;
        List<CartItemVO> items = new java.util.ArrayList<>(rows.size());
        for (CartItem row : rows) {
            SkuTradeDTO sku = skuMap.get(row.getSkuId());
            CartItemVO vo = new CartItemVO();
            vo.setId(row.getId());
            vo.setSkuId(row.getSkuId());
            vo.setProductId(row.getProductId());
            vo.setQuantity(row.getQuantity());
            vo.setChecked(row.getChecked() != null && row.getChecked() == 1);
            boolean invalid = sku == null || sku.getStatus() != 1;
            vo.setInvalid(invalid);
            if (sku != null) {
                vo.setName(sku.getProductName());
                vo.setSkuText(sku.getSkuText());
                vo.setImage(sku.getImage());
                vo.setPrice(priceService.build(sku.getPriceCents(), rate));
                vo.setOriginalPrice(priceService.build(sku.getOriginalPriceCents(), rate));
                vo.setDiscountPercent(sku.getDiscountPercent());
                vo.setStock(sku.getStock());
            } else {
                vo.setStock(0);
            }
            if (Boolean.TRUE.equals(vo.getChecked()) && !invalid) {
                checkedCount++;
                subtotalCents += sku.getPriceCents() * row.getQuantity();
            }
            items.add(vo);
        }
        return new CartListVO(items, checkedCount, priceService.build(subtotalCents, rate));
    }

    /** 加购：SKU 必须存在且上架；已存在则数量累加（上限 99）。返回购物车总件数。 */
    public long add(CartAddDTO dto) {
        long userId = LoginContext.requireUserId();
        List<SkuTradeDTO> skus = productTradeFacade.getSkusForTrade(List.of(dto.getSkuId()), null);
        if (skus.isEmpty() || skus.get(0).getStatus() != 1) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        SkuTradeDTO sku = skus.get(0);
        CartItem existing = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, dto.getSkuId()));
        if (existing != null) {
            existing.setQuantity(Math.min(MAX_QUANTITY, existing.getQuantity() + dto.getQuantity()));
            cartItemMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(sku.getProductId());
            item.setSkuId(sku.getSkuId());
            item.setQuantity(Math.min(MAX_QUANTITY, dto.getQuantity()));
            item.setChecked(1);
            cartItemMapper.insert(item);
        }
        return count();
    }

    /** 修改数量/勾选（归属校验）。 */
    public void update(Long id, CartUpdateDTO dto) {
        long userId = LoginContext.requireUserId();
        CartItem item = requireOwned(userId, id);
        if (dto.getQuantity() != null) {
            item.setQuantity(dto.getQuantity());
        }
        if (dto.getChecked() != null) {
            item.setChecked(dto.getChecked() ? 1 : 0);
        }
        cartItemMapper.updateById(item);
    }

    public void checkAll(boolean checked) {
        long userId = LoginContext.requireUserId();
        cartItemMapper.update(null, Wrappers.<CartItem>lambdaUpdate()
                .eq(CartItem::getUserId, userId)
                .set(CartItem::getChecked, checked ? 1 : 0));
    }

    public void delete(Long id) {
        long userId = LoginContext.requireUserId();
        requireOwned(userId, id);
        cartItemMapper.deleteById(id);
    }

    /** 删除当前用户全部勾选项。 */
    public void deleteChecked() {
        long userId = LoginContext.requireUserId();
        cartItemMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getChecked, 1));
    }

    /** 购物车总件数（Σ quantity，角标用）。 */
    public long count() {
        long userId = LoginContext.requireUserId();
        return cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                        .eq(CartItem::getUserId, userId)
                        .select(CartItem::getQuantity))
                .stream().mapToLong(CartItem::getQuantity).sum();
    }

    private CartItem requireOwned(long userId, Long id) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return item;
    }
}
