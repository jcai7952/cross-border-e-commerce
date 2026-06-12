package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.product.entity.ProductReview;
import com.kinn.shop.product.mapper.ProductReviewMapper;
import com.kinn.shop.product.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 商品评论（仅展示 status=1 已审核通过）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProductReviewMapper productReviewMapper;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    public PageResult<ReviewVO> page(Long productId, long pageNum, long pageSize) {
        long pn = Math.max(pageNum, 1);
        long ps = Math.min(Math.max(pageSize, 1), 100);
        Page<ProductReview> page = productReviewMapper.selectPage(new Page<>(pn, ps),
                Wrappers.<ProductReview>lambdaQuery()
                        .eq(ProductReview::getProductId, productId)
                        .eq(ProductReview::getStatus, 1)
                        .orderByDesc(ProductReview::getCreateTime)
                        .orderByDesc(ProductReview::getId));
        List<ReviewVO> list = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), pn, ps, list);
    }

    private ReviewVO toVO(ProductReview review) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setUserNickname(review.getUserNickname());
        vo.setSkuText(review.getSkuText());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setImages(parseImages(review.getImages()));
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }

    private List<String> parseImages(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> keys = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return keys.stream().map(fileService::url).filter(Objects::nonNull).toList();
        } catch (Exception e) {
            log.warn("[review] images json parse failed: {}", e.getMessage());
            return List.of();
        }
    }
}
