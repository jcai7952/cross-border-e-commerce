package com.kinn.shop.product.facade;

import com.kinn.shop.api.product.ProductTradeFacade;
import com.kinn.shop.api.product.dto.RateDTO;
import com.kinn.shop.api.product.dto.SkuTradeDTO;
import com.kinn.shop.api.product.dto.StockOpDTO;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.product.entity.Category;
import com.kinn.shop.product.entity.Currency;
import com.kinn.shop.product.entity.Product;
import com.kinn.shop.product.entity.ProductSku;
import com.kinn.shop.product.mapper.CategoryMapper;
import com.kinn.shop.product.mapper.ProductMapper;
import com.kinn.shop.product.mapper.ProductSkuMapper;
import com.kinn.shop.product.service.CurrencyService;
import com.kinn.shop.product.service.ExchangeRateService;
import com.kinn.shop.product.service.FileService;
import com.kinn.shop.product.service.FlashSaleService;
import com.kinn.shop.product.service.Locales;
import com.kinn.shop.product.service.ProductAssembler;
import com.kinn.shop.product.service.TradeStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品服务对内 RPC：交易链路取 SKU 快照、扣/回库存、取汇率。
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class ProductTradeFacadeImpl implements ProductTradeFacade {

    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductAssembler productAssembler;
    private final FlashSaleService flashSaleService;
    private final FileService fileService;
    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;
    private final TradeStockService tradeStockService;

    @Override
    public List<SkuTradeDTO> getSkusForTrade(List<Long> skuIds, String locale) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        Set<Long> distinctIds = new LinkedHashSet<>(skuIds);
        distinctIds.remove(null);
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        Map<Long, ProductSku> skuById = productSkuMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        if (skuById.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = skuById.values().stream().map(ProductSku::getProductId).distinct().toList();
        Map<Long, Product> productById = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<Long> categoryIds = productById.values().stream()
                .map(Product::getCategoryId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, Category> categoryById = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        Map<Long, String> nameByProduct = productAssembler.resolveNames(productIds, Locales.normalize(locale));
        FlashSaleService.CurrentFlash flash = flashSaleService.current();

        List<SkuTradeDTO> result = new ArrayList<>(distinctIds.size());
        for (Long skuId : distinctIds) {
            ProductSku sku = skuById.get(skuId);
            if (sku == null) {
                continue; // 查无的 skuId 不返回
            }
            Product product = productById.get(sku.getProductId());
            if (product == null) {
                continue; // 商品已被删，视同查无
            }
            result.add(toTradeDTO(sku, product, categoryById.get(product.getCategoryId()),
                    nameByProduct.get(product.getId()), flash));
        }
        return result;
    }

    private SkuTradeDTO toTradeDTO(ProductSku sku, Product product, Category category,
                                   String productName, FlashSaleService.CurrentFlash flash) {
        SkuTradeDTO dto = new SkuTradeDTO();
        dto.setSkuId(sku.getId());
        dto.setProductId(product.getId());
        dto.setProductName(productName);
        dto.setSkuText(skuText(sku));
        String imageKey = (sku.getImage() == null || sku.getImage().isBlank())
                ? product.getMainImage() : sku.getImage();
        dto.setImage(fileService.url(imageKey));

        long originalCents = sku.getPriceCents() == null ? 0 : sku.getPriceCents();
        dto.setOriginalPriceCents(originalCents);
        Integer discount = flash.discountByProduct().get(product.getId());
        if (discount != null) {
            dto.setDiscountPercent(discount);
            // 闪购折后价：USD 分上按 BigDecimal HALF_UP 取整
            dto.setPriceCents(BigDecimal.valueOf(originalCents)
                    .multiply(BigDecimal.valueOf(100L - discount))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .longValueExact());
        } else {
            dto.setPriceCents(originalCents);
        }

        dto.setStock(sku.getStock() == null ? 0 : sku.getStock());
        dto.setWeightGrams(sku.getWeightGrams() == null ? 0 : sku.getWeightGrams());
        dto.setTradeMode(product.getTradeMode());
        dto.setPostalTaxRate(category == null || category.getPostalTaxRate() == null
                ? 0 : category.getPostalTaxRate());
        boolean onSale = product.getStatus() != null && product.getStatus() == 1
                && sku.getStatus() != null && sku.getStatus() == 1;
        dto.setStatus(onSale ? 1 : 0);
        return dto;
    }

    /** 如 "Floral Blue / M"；color/size 缺失时只拼存在的部分。 */
    private String skuText(ProductSku sku) {
        StringBuilder sb = new StringBuilder();
        if (sku.getColor() != null && !sku.getColor().isBlank()) {
            sb.append(sku.getColor().trim());
        }
        if (sku.getSize() != null && !sku.getSize().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" / ");
            }
            sb.append(sku.getSize().trim());
        }
        return sb.toString();
    }

    @Override
    public boolean deductStock(List<StockOpDTO> ops) {
        if (ops == null || ops.isEmpty()) {
            return true;
        }
        try {
            tradeStockService.deduct(ops);
            return true;
        } catch (RuntimeException e) {
            log.warn("[trade] deductStock failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void restoreStock(List<StockOpDTO> ops) {
        if (ops == null || ops.isEmpty()) {
            return;
        }
        tradeStockService.restore(ops);
    }

    @Override
    public RateDTO getRate(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        String code = currency.trim().toUpperCase();
        Currency entity;
        try {
            entity = currencyService.getRequired(code);
        } catch (BizException e) {
            return null; // 不存在或 enabled=0
        }
        BigDecimal rate;
        if ("USD".equals(code)) {
            rate = BigDecimal.ONE;
        } else {
            try {
                rate = exchangeRateService.getRate(code);
            } catch (BizException e) {
                return null; // 无汇率记录
            }
        }
        RateDTO dto = new RateDTO();
        dto.setCurrency(entity.getCode());
        dto.setSymbol(entity.getSymbol());
        dto.setDecimalDigits(entity.getDecimalDigits() == null ? 2 : entity.getDecimalDigits());
        dto.setRate(rate);
        return dto;
    }
}
