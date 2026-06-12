package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.product.entity.Product;
import com.kinn.shop.product.entity.UserFavorite;
import com.kinn.shop.product.mapper.ProductMapper;
import com.kinn.shop.product.mapper.UserFavoriteMapper;
import com.kinn.shop.product.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收藏夹（需登录）。
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;
    private final ProductMapper productMapper;
    private final PriceService priceService;
    private final ProductAssembler assembler;

    /** 切换收藏，返回切换后的状态。 */
    public boolean toggle(Long productId) {
        long userId = LoginContext.requireUserId();
        if (productMapper.selectById(productId) == null) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        UserFavorite existing = userFavoriteMapper.selectOne(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getProductId, productId));
        if (existing != null) {
            userFavoriteMapper.deleteById(existing.getId());
            return false;
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        userFavoriteMapper.insert(favorite);
        return true;
    }

    public PageResult<ProductListVO> list(String locale, String currency, long pageNum, long pageSize) {
        long userId = LoginContext.requireUserId();
        long pn = Math.max(pageNum, 1);
        long ps = Math.min(Math.max(pageSize, 1), 100);
        PriceService.CurrencyCtx ctx = priceService.resolve(currency);
        String loc = Locales.normalize(locale);

        Page<UserFavorite> page = userFavoriteMapper.selectPage(new Page<>(pn, ps),
                Wrappers.<UserFavorite>lambdaQuery()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getCreateTime)
                        .orderByDesc(UserFavorite::getId));
        List<Long> productIds = page.getRecords().stream().map(UserFavorite::getProductId).toList();
        if (productIds.isEmpty()) {
            return PageResult.of(page.getTotal(), pn, ps, List.of());
        }
        Map<Long, Product> byId = productMapper.selectList(Wrappers.<Product>lambdaQuery()
                        .in(Product::getId, productIds))
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        // 按收藏时间顺序输出
        List<Product> ordered = productIds.stream().map(byId::get).filter(Objects::nonNull).toList();
        return PageResult.of(page.getTotal(), pn, ps, assembler.toListVOs(ordered, loc, ctx));
    }
}
