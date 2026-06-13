package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.product.entity.Product;
import com.kinn.shop.product.entity.ProductReview;
import com.kinn.shop.product.mapper.ProductMapper;
import com.kinn.shop.product.mapper.ProductReviewMapper;
import com.kinn.shop.product.vo.AdminReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 管理端评论审核：分页 + 通过/拒绝（首次通过累加商品评分，撤下反向扣减）。
 */
@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ProductReviewMapper productReviewMapper;
    private final ProductMapper productMapper;
    private final ReviewService reviewService;

    public PageResult<AdminReviewVO> page(Long productId, Integer status, long pageNum, long pageSize) {
        long pn = Math.max(pageNum, 1);
        long ps = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<ProductReview> qw = Wrappers.lambdaQuery();
        if (productId != null) {
            qw.eq(ProductReview::getProductId, productId);
        }
        if (status != null) {
            qw.eq(ProductReview::getStatus, status);
        }
        qw.orderByDesc(ProductReview::getId);
        Page<ProductReview> page = productReviewMapper.selectPage(new Page<>(pn, ps), qw);
        List<AdminReviewVO> list = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), pn, ps, list);
    }

    /**
     * 审核：status 0→1/2，已通过的可改 2（撤下）。
     * 置 1 时聚合累加商品评分，从 1 撤下时反向扣减；同状态幂等不重复计。
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, boolean approve) {
        ProductReview review = productReviewMapper.selectById(id);
        if (review == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "评论不存在");
        }
        int from = review.getStatus() == null ? 0 : review.getStatus();
        int to = approve ? 1 : 2;
        if (from == to) {
            return; // 幂等：状态未变不重复聚合
        }
        review.setStatus(to);
        productReviewMapper.updateById(review);
        if (to == 1) {
            applyRating(review.getProductId(), review.getRating(), true);
        } else if (from == 1) {
            applyRating(review.getProductId(), review.getRating(), false);
        }
    }

    /** 加权平均维护：add=true 累加一条评分，false 扣减（防除零）。 */
    private void applyRating(Long productId, Integer rating, boolean add) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return; // 商品已删，跳过聚合
        }
        int count = product.getRatingCount() == null ? 0 : product.getRatingCount();
        BigDecimal avg = product.getRatingAvg() == null ? BigDecimal.ZERO : product.getRatingAvg();
        BigDecimal r = BigDecimal.valueOf(rating == null ? 0 : rating);
        if (add) {
            BigDecimal newAvg = avg.multiply(BigDecimal.valueOf(count)).add(r)
                    .divide(BigDecimal.valueOf(count + 1L), 1, RoundingMode.HALF_UP);
            product.setRatingCount(count + 1);
            product.setRatingAvg(newAvg);
        } else {
            int newCount = Math.max(count - 1, 0);
            if (newCount == 0) {
                product.setRatingCount(0);
                product.setRatingAvg(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
            } else {
                BigDecimal newAvg = avg.multiply(BigDecimal.valueOf(count)).subtract(r)
                        .divide(BigDecimal.valueOf(newCount), 1, RoundingMode.HALF_UP);
                if (newAvg.signum() < 0) {
                    newAvg = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
                }
                product.setRatingCount(newCount);
                product.setRatingAvg(newAvg);
            }
        }
        productMapper.updateById(product);
    }

    private AdminReviewVO toVO(ProductReview review) {
        AdminReviewVO vo = new AdminReviewVO();
        vo.setId(review.getId());
        vo.setProductId(review.getProductId());
        vo.setOrderNo(review.getOrderNo());
        vo.setUserId(review.getUserId());
        vo.setUserNickname(review.getUserNickname());
        vo.setSkuText(review.getSkuText());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setImages(reviewService.imageUrls(review.getImages()));
        vo.setStatus(review.getStatus());
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }
}
