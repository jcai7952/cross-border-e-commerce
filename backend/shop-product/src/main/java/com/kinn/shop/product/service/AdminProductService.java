package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.product.dto.ProductSaveDTO;
import com.kinn.shop.product.entity.Product;
import com.kinn.shop.product.entity.ProductI18n;
import com.kinn.shop.product.entity.ProductImage;
import com.kinn.shop.product.entity.ProductSku;
import com.kinn.shop.product.mapper.ProductI18nMapper;
import com.kinn.shop.product.mapper.ProductImageMapper;
import com.kinn.shop.product.mapper.ProductMapper;
import com.kinn.shop.product.mapper.ProductSkuMapper;
import com.kinn.shop.product.vo.AdminProductDetailVO;
import com.kinn.shop.product.vo.AdminProductPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端商品维护：分页 / 编辑视图 / 创建 / 全量更新 / 上下架 / SKU 库存。
 */
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductMapper productMapper;
    private final ProductI18nMapper productI18nMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CategoryService categoryService;
    private final FileService fileService;

    /** keyword 匹配 en-US 名称或 spu_code。 */
    public PageResult<AdminProductPageVO> page(Long categoryId, String keyword, Integer status,
                                               long pageNum, long pageSize) {
        long pn = Math.max(pageNum, 1);
        long ps = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<Product> qw = Wrappers.lambdaQuery();
        if (categoryId != null) {
            qw.in(Product::getCategoryId, categoryService.expandCategoryIds(categoryId));
        }
        if (status != null) {
            qw.eq(Product::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> matched = productI18nMapper.selectList(Wrappers.<ProductI18n>lambdaQuery()
                            .eq(ProductI18n::getLocale, Locales.EN)
                            .like(ProductI18n::getName, kw))
                    .stream().map(ProductI18n::getProductId).distinct().toList();
            qw.and(w -> {
                w.like(Product::getSpuCode, kw);
                if (!matched.isEmpty()) {
                    w.or().in(Product::getId, matched);
                }
            });
        }
        qw.orderByDesc(Product::getId);

        Page<Product> page = productMapper.selectPage(new Page<>(pn, ps), qw);
        List<Product> records = page.getRecords();
        Map<Long, Map<String, String>> names = new HashMap<>();
        if (!records.isEmpty()) {
            productI18nMapper.selectList(Wrappers.<ProductI18n>lambdaQuery()
                            .in(ProductI18n::getProductId, records.stream().map(Product::getId).toList()))
                    .forEach(row -> names.computeIfAbsent(row.getProductId(), k -> new HashMap<>())
                            .put(row.getLocale(), row.getName()));
        }
        List<AdminProductPageVO> list = records.stream().map(p -> {
            AdminProductPageVO vo = new AdminProductPageVO();
            vo.setId(p.getId());
            vo.setCategoryId(p.getCategoryId());
            vo.setSpuCode(p.getSpuCode());
            vo.setBrand(p.getBrand());
            vo.setTradeMode(p.getTradeMode());
            Map<String, String> n = names.getOrDefault(p.getId(), Map.of());
            vo.setNameZh(n.get(Locales.ZH));
            vo.setNameEn(n.get(Locales.EN));
            vo.setMainImage(p.getMainImage());
            vo.setMainImageUrl(fileService.url(p.getMainImage()));
            vo.setMinPriceCents(p.getMinPriceCents());
            vo.setSalesCount(p.getSalesCount());
            vo.setRatingAvg(p.getRatingAvg());
            vo.setRatingCount(p.getRatingCount());
            vo.setStatus(p.getStatus());
            vo.setCreateTime(p.getCreateTime());
            return vo;
        }).toList();
        return PageResult.of(page.getTotal(), pn, ps, list);
    }

    /** 完整编辑视图（i18n 两条 + 图册 + SKU 全量真实库存）。 */
    public AdminProductDetailVO detail(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        AdminProductDetailVO vo = new AdminProductDetailVO();
        vo.setId(p.getId());
        vo.setCategoryId(p.getCategoryId());
        vo.setSpuCode(p.getSpuCode());
        vo.setBrand(p.getBrand());
        vo.setTradeMode(p.getTradeMode());
        vo.setOriginCountry(p.getOriginCountry());
        vo.setMainImage(p.getMainImage());
        vo.setMainImageUrl(fileService.url(p.getMainImage()));
        vo.setMinPriceCents(p.getMinPriceCents());
        vo.setSalesCount(p.getSalesCount());
        vo.setRatingAvg(p.getRatingAvg());
        vo.setRatingCount(p.getRatingCount());
        vo.setStatus(p.getStatus());
        vo.setCreateTime(p.getCreateTime());
        vo.setUpdateTime(p.getUpdateTime());

        vo.setI18ns(productI18nMapper.selectList(Wrappers.<ProductI18n>lambdaQuery()
                        .eq(ProductI18n::getProductId, id))
                .stream()
                .sorted(Comparator.comparing(ProductI18n::getLocale))
                .map(row -> {
                    AdminProductDetailVO.I18nVO iv = new AdminProductDetailVO.I18nVO();
                    iv.setLocale(row.getLocale());
                    iv.setName(row.getName());
                    iv.setSubtitle(row.getSubtitle());
                    iv.setDetail(row.getDetail());
                    return iv;
                }).toList());

        vo.setImages(productImageMapper.selectList(Wrappers.<ProductImage>lambdaQuery()
                        .eq(ProductImage::getProductId, id)
                        .orderByAsc(ProductImage::getSort))
                .stream().map(img -> {
                    AdminProductDetailVO.ImageVO iv = new AdminProductDetailVO.ImageVO();
                    iv.setId(img.getId());
                    iv.setKey(img.getUrl());
                    iv.setUrl(fileService.url(img.getUrl()));
                    iv.setSort(img.getSort());
                    return iv;
                }).toList());

        vo.setSkus(productSkuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                        .eq(ProductSku::getProductId, id)
                        .orderByAsc(ProductSku::getId))
                .stream().map(sku -> {
                    AdminProductDetailVO.SkuVO sv = new AdminProductDetailVO.SkuVO();
                    sv.setId(sku.getId());
                    sv.setSkuCode(sku.getSkuCode());
                    sv.setColor(sku.getColor());
                    sv.setColorZh(sku.getColorZh());
                    sv.setSize(sku.getSize());
                    sv.setPriceCents(sku.getPriceCents());
                    sv.setStock(sku.getStock());
                    sv.setImage(sku.getImage());
                    sv.setImageUrl(fileService.url(sku.getImage()));
                    sv.setWeightGrams(sku.getWeightGrams());
                    sv.setStatus(sku.getStatus());
                    return sv;
                }).toList());
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductSaveDTO dto) {
        if (productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getSpuCode, dto.getSpuCode())) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "SPU 编码已存在");
        }
        Product p = new Product();
        applyProduct(dto, p);
        productMapper.insert(p);
        insertI18ns(p.getId(), dto);
        insertImages(p.getId(), dto);
        for (ProductSaveDTO.SkuDTO s : dto.getSkus()) {
            ProductSku sku = new ProductSku();
            applySku(s, sku, p.getId());
            productSkuMapper.insert(sku);
        }
        return p.getId();
    }

    /** 全量更新：i18n/images 删后插，skus 按 id 更新/新增/删除。 */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductSaveDTO dto) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getSpuCode, dto.getSpuCode())
                .ne(Product::getId, id)) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "SPU 编码已存在");
        }
        applyProduct(dto, p);
        productMapper.updateById(p);

        productI18nMapper.delete(Wrappers.<ProductI18n>lambdaQuery().eq(ProductI18n::getProductId, id));
        insertI18ns(id, dto);

        productImageMapper.delete(Wrappers.<ProductImage>lambdaQuery().eq(ProductImage::getProductId, id));
        insertImages(id, dto);

        Map<Long, ProductSku> existing = productSkuMapper.selectList(Wrappers.<ProductSku>lambdaQuery()
                        .eq(ProductSku::getProductId, id))
                .stream().collect(Collectors.toMap(ProductSku::getId, Function.identity()));
        Set<Long> kept = new HashSet<>();
        for (ProductSaveDTO.SkuDTO s : dto.getSkus()) {
            if (s.getId() != null) {
                ProductSku db = existing.get(s.getId());
                if (db == null) {
                    throw new BizException(ErrorCode.SKU_NOT_FOUND);
                }
                applySku(s, db, id);
                productSkuMapper.updateById(db);
                kept.add(s.getId());
            } else {
                ProductSku sku = new ProductSku();
                applySku(s, sku, id);
                productSkuMapper.insert(sku);
            }
        }
        existing.keySet().stream()
                .filter(skuId -> !kept.contains(skuId))
                .forEach(productSkuMapper::deleteById);
    }

    public void updateStatus(Long id, Integer status) {
        Product p = productMapper.selectById(id);
        if (p == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        p.setStatus(status);
        productMapper.updateById(p);
    }

    public void updateStock(Long skuId, Integer stock) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(ErrorCode.SKU_NOT_FOUND);
        }
        sku.setStock(stock);
        productSkuMapper.updateById(sku);
    }

    private void applyProduct(ProductSaveDTO dto, Product p) {
        p.setCategoryId(dto.getCategoryId());
        p.setSpuCode(dto.getSpuCode().trim());
        p.setBrand(dto.getBrand());
        p.setTradeMode(dto.getTradeMode() == null ? "BONDED" : dto.getTradeMode());
        p.setOriginCountry(dto.getOriginCountry() == null ? "CN" : dto.getOriginCountry());
        p.setMainImage(dto.getMainImage());
        p.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        // 冗余最低 SKU 价
        p.setMinPriceCents(dto.getSkus().stream()
                .map(ProductSaveDTO.SkuDTO::getPriceCents)
                .min(Long::compareTo)
                .orElse(0L));
    }

    private void insertI18ns(Long productId, ProductSaveDTO dto) {
        for (ProductSaveDTO.I18nDTO i : dto.getI18ns()) {
            ProductI18n row = new ProductI18n();
            row.setProductId(productId);
            row.setLocale(Locales.normalize(i.getLocale()));
            row.setName(i.getName());
            row.setSubtitle(i.getSubtitle());
            row.setDetail(i.getDetail());
            productI18nMapper.insert(row);
        }
    }

    private void insertImages(Long productId, ProductSaveDTO dto) {
        List<String> images = dto.getImages() == null ? List.of() : dto.getImages();
        int sort = 0;
        for (String key : images) {
            if (key == null || key.isBlank()) {
                continue;
            }
            ProductImage img = new ProductImage();
            img.setProductId(productId);
            img.setUrl(key);
            img.setSort(sort++);
            productImageMapper.insert(img);
        }
    }

    private void applySku(ProductSaveDTO.SkuDTO s, ProductSku sku, Long productId) {
        sku.setProductId(productId);
        sku.setSkuCode(s.getSkuCode().trim());
        sku.setColor(s.getColor());
        sku.setColorZh(s.getColorZh());
        sku.setSize(s.getSize());
        sku.setPriceCents(s.getPriceCents());
        sku.setStock(s.getStock());
        sku.setImage(s.getImage());
        sku.setWeightGrams(s.getWeightGrams() == null ? 300 : s.getWeightGrams());
        sku.setStatus(s.getStatus() == null ? 1 : s.getStatus());
    }
}
