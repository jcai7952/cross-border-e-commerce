package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.product.entity.Product;
import com.kinn.shop.product.entity.ProductI18n;
import com.kinn.shop.product.entity.ProductImage;
import com.kinn.shop.product.entity.ProductSku;
import com.kinn.shop.product.entity.SizeChart;
import com.kinn.shop.product.entity.UserFavorite;
import com.kinn.shop.product.mapper.ProductI18nMapper;
import com.kinn.shop.product.mapper.ProductImageMapper;
import com.kinn.shop.product.mapper.ProductMapper;
import com.kinn.shop.product.mapper.ProductSkuMapper;
import com.kinn.shop.product.mapper.SizeChartMapper;
import com.kinn.shop.product.mapper.UserFavoriteMapper;
import com.kinn.shop.product.vo.FlashSaleCurrentVO;
import com.kinn.shop.product.vo.ProductDetailVO;
import com.kinn.shop.product.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 前台商品：分页搜索 / 详情 / 当前闪购。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductI18nMapper productI18nMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductSkuMapper productSkuMapper;
    private final SizeChartMapper sizeChartMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final CategoryService categoryService;
    private final FlashSaleService flashSaleService;
    private final PriceService priceService;
    private final ProductAssembler assembler;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    public PageResult<ProductListVO> page(Long categoryId, String keyword, String sort,
                                          long pageNum, long pageSize, String locale, String currency) {
        long pn = Math.max(pageNum, 1);
        long ps = Math.min(Math.max(pageSize, 1), 100);
        PriceService.CurrencyCtx ctx = priceService.resolve(currency);
        String loc = Locales.normalize(locale);

        LambdaQueryWrapper<Product> qw = Wrappers.<Product>lambdaQuery().eq(Product::getStatus, 1);
        if (categoryId != null) {
            qw.in(Product::getCategoryId, categoryService.expandCategoryIds(categoryId));
        }
        if (StringUtils.hasText(keyword)) {
            List<Long> matched = productI18nMapper.selectList(Wrappers.<ProductI18n>lambdaQuery()
                            .eq(ProductI18n::getLocale, loc)
                            .like(ProductI18n::getName, keyword.trim()))
                    .stream().map(ProductI18n::getProductId).distinct().toList();
            if (matched.isEmpty()) {
                return PageResult.of(0, pn, ps, List.of());
            }
            qw.in(Product::getId, matched);
        }
        applySort(qw, sort);

        Page<Product> page = productMapper.selectPage(new Page<>(pn, ps), qw);
        return PageResult.of(page.getTotal(), pn, ps, assembler.toListVOs(page.getRecords(), loc, ctx));
    }

    private void applySort(LambdaQueryWrapper<Product> qw, String sort) {
        switch (sort == null ? "new" : sort) {
            case "sales" -> qw.orderByDesc(Product::getSalesCount);
            case "price_asc" -> qw.orderByAsc(Product::getMinPriceCents);
            case "price_desc" -> qw.orderByDesc(Product::getMinPriceCents);
            default -> qw.orderByDesc(Product::getCreateTime);
        }
        qw.orderByDesc(Product::getId);
    }

    public ProductDetailVO detail(Long id, String locale, String currency) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        PriceService.CurrencyCtx ctx = priceService.resolve(currency);
        String loc = Locales.normalize(locale);

        ProductI18n i18n = findI18n(id, loc);
        FlashSaleService.CurrentFlash flash = flashSaleService.current();
        Integer discount = flash.discountByProduct().get(id);

        ProductDetailVO vo = new ProductDetailVO();
        vo.setId(product.getId());
        vo.setCategoryId(product.getCategoryId());
        if (i18n != null) {
            vo.setName(i18n.getName());
            vo.setSubtitle(i18n.getSubtitle());
            vo.setDetail(i18n.getDetail());
        }
        vo.setBrand(product.getBrand());
        vo.setTradeMode(product.getTradeMode());
        vo.setOriginCountry(product.getOriginCountry());
        vo.setImages(productImageMapper.selectList(Wrappers.<ProductImage>lambdaQuery()
                        .eq(ProductImage::getProductId, id)
                        .orderByAsc(ProductImage::getSort))
                .stream().map(img -> fileService.url(img.getUrl())).toList());
        vo.setSkus(productSkuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                        .eq(ProductSku::getProductId, id)
                        .eq(ProductSku::getStatus, 1)
                        .orderByAsc(ProductSku::getId))
                .stream().map(sku -> toSkuVO(sku, discount, ctx)).toList());
        vo.setSizeChart(loadSizeChart(product.getCategoryId(), loc));
        vo.setRatingAvg(product.getRatingAvg());
        vo.setRatingCount(product.getRatingCount());
        vo.setSalesCount(product.getSalesCount());

        Long userId = LoginContext.userIdOrNull();
        vo.setFavorite(userId != null && userFavoriteMapper.selectCount(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getProductId, id)) > 0);

        if (discount != null) {
            vo.setFlash(new ProductDetailVO.FlashVO(discount, flash.sale().getEndTime()));
        }
        return vo;
    }

    private ProductDetailVO.SkuVO toSkuVO(ProductSku sku, Integer discount, PriceService.CurrencyCtx ctx) {
        ProductDetailVO.SkuVO vo = new ProductDetailVO.SkuVO();
        vo.setId(sku.getId());
        vo.setColor(sku.getColor());
        vo.setColorZh(sku.getColorZh());
        vo.setSize(sku.getSize());
        long usdCents = sku.getPriceCents() == null ? 0 : sku.getPriceCents();
        vo.setPrice(priceService.build(usdCents, ctx));
        if (discount != null) {
            vo.setFlashPrice(priceService.build(PriceService.flashCents(usdCents, discount), ctx));
        }
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        vo.setStock(Math.min(Math.max(stock, 0), 99));
        vo.setImage(fileService.url(sku.getImage()));
        return vo;
    }

    private ProductI18n findI18n(Long productId, String locale) {
        ProductI18n i18n = productI18nMapper.selectOne(Wrappers.<ProductI18n>lambdaQuery()
                .eq(ProductI18n::getProductId, productId)
                .eq(ProductI18n::getLocale, locale));
        if (i18n == null && !Locales.EN.equals(locale)) {
            i18n = productI18nMapper.selectOne(Wrappers.<ProductI18n>lambdaQuery()
                    .eq(ProductI18n::getProductId, productId)
                    .eq(ProductI18n::getLocale, Locales.EN));
        }
        return i18n;
    }

    /** 尺码表：按 locale 取，缺失回退 en-US，再缺省 null（JSON 解析为数组对象）。 */
    private Object loadSizeChart(Long categoryId, String locale) {
        SizeChart chart = sizeChartMapper.selectOne(Wrappers.<SizeChart>lambdaQuery()
                .eq(SizeChart::getCategoryId, categoryId)
                .eq(SizeChart::getLocale, locale));
        if (chart == null && !Locales.EN.equals(locale)) {
            chart = sizeChartMapper.selectOne(Wrappers.<SizeChart>lambdaQuery()
                    .eq(SizeChart::getCategoryId, categoryId)
                    .eq(SizeChart::getLocale, Locales.EN));
        }
        if (chart == null || chart.getContentJson() == null || chart.getContentJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(chart.getContentJson(), Object.class);
        } catch (Exception e) {
            log.warn("[product] size chart json parse failed, categoryId={}", categoryId);
            return null;
        }
    }

    /** 进行中的闪购（无则 null）。 */
    public FlashSaleCurrentVO flashSaleCurrent(String locale, String currency) {
        FlashSaleService.CurrentFlash flash = flashSaleService.current();
        if (!flash.active() || flash.discountByProduct().isEmpty()) {
            return null;
        }
        PriceService.CurrencyCtx ctx = priceService.resolve(currency);
        String loc = Locales.normalize(locale);
        List<Product> products = productMapper.selectList(Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, 1)
                .in(Product::getId, flash.discountByProduct().keySet())
                .orderByDesc(Product::getSalesCount));
        FlashSaleCurrentVO vo = new FlashSaleCurrentVO();
        vo.setId(flash.sale().getId());
        vo.setTitle(flash.sale().getTitle());
        vo.setEndTime(flash.sale().getEndTime());
        vo.setItems(assembler.toListVOs(products, loc, ctx));
        return vo;
    }
}
