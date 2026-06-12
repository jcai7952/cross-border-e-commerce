package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.api.product.dto.StockOpDTO;
import com.kinn.shop.product.entity.FlashSaleItem;
import com.kinn.shop.product.entity.ProductSku;
import com.kinn.shop.product.mapper.FlashSaleItemMapper;
import com.kinn.shop.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易链路库存扣减/回滚（DB 乐观锁 stock>=q）。
 * 事务逻辑独立成 Service 由 Dubbo facade 调用，避免同类自调用导致 @Transactional 失效。
 */
@Service
@RequiredArgsConstructor
public class TradeStockService {

    private final ProductSkuMapper productSkuMapper;
    private final FlashSaleItemMapper flashSaleItemMapper;
    private final FlashSaleService flashSaleService;

    /**
     * 逐条扣减，任一 SKU 库存不足（影响行数=0）抛运行时异常整体回滚。
     * 全部成功后对命中当前闪购的商品累计 flash_sale_item.sold。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deduct(List<StockOpDTO> ops) {
        for (StockOpDTO op : ops) {
            int q = requireValid(op);
            int affected = productSkuMapper.update(null, Wrappers.<ProductSku>lambdaUpdate()
                    .eq(ProductSku::getId, op.getSkuId())
                    .ge(ProductSku::getStock, q)
                    .setSql("stock = stock - {0}, version = version + 1", q));
            if (affected == 0) {
                throw new IllegalStateException("库存不足或 SKU 不存在, skuId=" + op.getSkuId());
            }
        }
        // 命中当前闪购的商品同步累计 sold。
        // 注意：不强制 quota 上限——quota 耗尽不阻断下单，sold 仅作活动统计展示。
        FlashSaleService.CurrentFlash flash = flashSaleService.current();
        if (flash.active()) {
            qtyByFlashProduct(ops, flash).forEach((productId, qty) ->
                    flashSaleItemMapper.update(null, Wrappers.<FlashSaleItem>lambdaUpdate()
                            .eq(FlashSaleItem::getSaleId, flash.sale().getId())
                            .eq(FlashSaleItem::getProductId, productId)
                            .setSql("sold = sold + {0}", qty)));
        }
    }

    /** 回滚库存（关单/取消），闪购 sold 同步回减且不小于 0。 */
    @Transactional(rollbackFor = Exception.class)
    public void restore(List<StockOpDTO> ops) {
        for (StockOpDTO op : ops) {
            int q = requireValid(op);
            productSkuMapper.update(null, Wrappers.<ProductSku>lambdaUpdate()
                    .eq(ProductSku::getId, op.getSkuId())
                    .setSql("stock = stock + {0}", q));
        }
        FlashSaleService.CurrentFlash flash = flashSaleService.current();
        if (flash.active()) {
            qtyByFlashProduct(ops, flash).forEach((productId, qty) ->
                    flashSaleItemMapper.update(null, Wrappers.<FlashSaleItem>lambdaUpdate()
                            .eq(FlashSaleItem::getSaleId, flash.sale().getId())
                            .eq(FlashSaleItem::getProductId, productId)
                            .setSql("sold = IF(sold >= {0}, sold - {0}, 0)", qty)));
        }
    }

    /** 按命中闪购的 productId 聚合数量（同一商品多 SKU 合并一次更新）。 */
    private Map<Long, Integer> qtyByFlashProduct(List<StockOpDTO> ops, FlashSaleService.CurrentFlash flash) {
        Map<Long, Integer> qty = new LinkedHashMap<>();
        for (StockOpDTO op : ops) {
            Long productId = op.getProductId();
            if (productId != null && flash.discountByProduct().containsKey(productId)) {
                qty.merge(productId, op.getQuantity(), Integer::sum);
            }
        }
        return qty;
    }

    private int requireValid(StockOpDTO op) {
        if (op == null || op.getSkuId() == null || op.getQuantity() <= 0) {
            throw new IllegalArgumentException("非法库存操作: " + op);
        }
        return op.getQuantity();
    }
}
