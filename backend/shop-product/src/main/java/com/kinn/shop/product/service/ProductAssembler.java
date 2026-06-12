package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.product.entity.Product;
import com.kinn.shop.product.entity.ProductI18n;
import com.kinn.shop.product.mapper.ProductI18nMapper;
import com.kinn.shop.product.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品列表 VO 装配：多语言名称（缺失回退 en-US）+ 闪购价 + 多币种换算。
 */
@Component
@RequiredArgsConstructor
public class ProductAssembler {

    private final ProductI18nMapper productI18nMapper;
    private final FlashSaleService flashSaleService;
    private final PriceService priceService;
    private final FileService fileService;

    public List<ProductListVO> toListVOs(List<Product> products, String locale, PriceService.CurrencyCtx ctx) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        String loc = Locales.normalize(locale);
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, String> names = resolveNames(ids, loc);
        FlashSaleService.CurrentFlash flash = flashSaleService.current();
        return products.stream()
                .map(p -> toListVO(p, names.get(p.getId()), flash.discountByProduct().get(p.getId()), ctx))
                .toList();
    }

    /** 当前 locale 名称，缺失回退 en-US。 */
    public Map<Long, String> resolveNames(List<Long> productIds, String locale) {
        List<String> locales = Locales.EN.equals(locale) ? List.of(Locales.EN) : List.of(locale, Locales.EN);
        List<ProductI18n> rows = productI18nMapper.selectList(Wrappers.<ProductI18n>lambdaQuery()
                .in(ProductI18n::getProductId, productIds)
                .in(ProductI18n::getLocale, locales));
        Map<Long, String> target = new HashMap<>();
        Map<Long, String> fallback = new HashMap<>();
        for (ProductI18n row : rows) {
            if (locale.equals(row.getLocale())) {
                target.put(row.getProductId(), row.getName());
            } else if (Locales.EN.equals(row.getLocale())) {
                fallback.put(row.getProductId(), row.getName());
            }
        }
        fallback.forEach(target::putIfAbsent);
        return target;
    }

    private ProductListVO toListVO(Product p, String name, Integer discountPercent, PriceService.CurrencyCtx ctx) {
        ProductListVO vo = new ProductListVO();
        vo.setId(p.getId());
        vo.setName(name);
        vo.setMainImage(fileService.url(p.getMainImage()));
        long usdCents = p.getMinPriceCents() == null ? 0 : p.getMinPriceCents();
        vo.setPrice(priceService.build(usdCents, ctx));
        if (discountPercent != null) {
            vo.setFlashPrice(priceService.build(PriceService.flashCents(usdCents, discountPercent), ctx));
            vo.setDiscountPercent(discountPercent);
        }
        vo.setSalesCount(p.getSalesCount());
        vo.setRatingAvg(p.getRatingAvg());
        vo.setRatingCount(p.getRatingCount());
        return vo;
    }
}
