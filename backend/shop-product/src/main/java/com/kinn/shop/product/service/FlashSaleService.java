package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.product.entity.FlashSale;
import com.kinn.shop.product.entity.FlashSaleItem;
import com.kinn.shop.product.mapper.FlashSaleItemMapper;
import com.kinn.shop.product.mapper.FlashSaleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 闪购：当前进行中的活动及其商品折扣表。
 */
@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final FlashSaleMapper flashSaleMapper;
    private final FlashSaleItemMapper flashSaleItemMapper;

    /** 当前活动快照：sale=null 表示无进行中的闪购。 */
    public record CurrentFlash(FlashSale sale, Map<Long, Integer> discountByProduct) {

        public static final CurrentFlash NONE = new CurrentFlash(null, Map.of());

        public boolean active() {
            return sale != null;
        }
    }

    public CurrentFlash current() {
        LocalDateTime now = LocalDateTime.now();
        FlashSale sale = flashSaleMapper.selectOne(Wrappers.<FlashSale>lambdaQuery()
                .eq(FlashSale::getStatus, 1)
                .le(FlashSale::getStartTime, now)
                .ge(FlashSale::getEndTime, now)
                .orderByAsc(FlashSale::getEndTime)
                .last("LIMIT 1"));
        if (sale == null) {
            return CurrentFlash.NONE;
        }
        Map<Long, Integer> discounts = flashSaleItemMapper.selectList(Wrappers.<FlashSaleItem>lambdaQuery()
                        .eq(FlashSaleItem::getSaleId, sale.getId()))
                .stream()
                .collect(Collectors.toMap(FlashSaleItem::getProductId, FlashSaleItem::getDiscountPercent,
                        (a, b) -> a, HashMap::new));
        return new CurrentFlash(sale, discounts);
    }
}
