package com.kinn.shop.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinn.shop.api.order.OrderFacade;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.product.dto.ReviewCreateDTO;
import com.kinn.shop.product.entity.ProductReview;
import com.kinn.shop.product.mapper.ProductReviewMapper;
import com.kinn.shop.product.vo.ReviewVO;
import com.kinn.shop.product.vo.UploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * 商品评论：前台展示（仅 status=1 已审核通过）+ 已购验证发布 + 晒图上传。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final ProductReviewMapper productReviewMapper;
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final OrderFacade orderFacade;

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

    /** 发布评论：订单完成且含该商品才可评，每单每商品一条，status=0 待审。 */
    public Long create(ReviewCreateDTO dto) {
        long userId = LoginContext.requireUserId();
        List<Long> reviewable = orderFacade.getReviewableProductIds(dto.getOrderNo(), userId);
        if (reviewable == null || !reviewable.contains(dto.getProductId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该订单不可评价此商品");
        }
        ProductReview review = new ProductReview();
        review.setProductId(dto.getProductId());
        review.setOrderNo(dto.getOrderNo().trim());
        review.setUserId(userId);
        // product 服务无用户表，昵称用兜底，不跨服务查
        review.setUserNickname("User" + userId);
        review.setSkuText(null);
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setImages(writeImages(dto.getImages()));
        review.setStatus(0);
        try {
            productReviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该商品已评价");
        }
        return review.getId();
    }

    /** 晒图上传：仅图片、最大 5MB，返回对象 key 与完整 URL。 */
    public UploadVO uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BizException(ErrorCode.PARAM_ERROR, "图片大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仅支持图片文件");
        }
        String key = fileService.upload(file, "reviews");
        return new UploadVO(key, fileService.url(key));
    }

    /** images JSON（对象 key 数组）→ 完整 URL 列表（管理端复用）。 */
    public List<String> imageUrls(String json) {
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

    private ReviewVO toVO(ProductReview review) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setUserNickname(review.getUserNickname());
        vo.setSkuText(review.getSkuText());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setImages(imageUrls(review.getImages()));
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }

    /** 对象 key 数组 → JSON 入库；空集合存 null。 */
    private String writeImages(List<String> keys) {
        List<String> cleaned = keys == null ? List.of()
                : keys.stream().filter(k -> k != null && !k.isBlank()).map(String::trim).toList();
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "images 格式错误");
        }
    }
}
