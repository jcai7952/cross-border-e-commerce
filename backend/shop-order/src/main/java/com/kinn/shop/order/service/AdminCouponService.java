package com.kinn.shop.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.order.dto.CouponSaveDTO;
import com.kinn.shop.order.entity.Coupon;
import com.kinn.shop.order.mapper.CouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 优惠券管理（管理端）。
 */
@Service
@RequiredArgsConstructor
public class AdminCouponService {

    private final CouponMapper couponMapper;

    public PageResult<Coupon> page(long pageNum, long pageSize) {
        LoginContext.requireAdminId();
        long pn = Math.max(pageNum, 1);
        long ps = Math.min(Math.max(pageSize, 1), 100);
        Page<Coupon> page = couponMapper.selectPage(new Page<>(pn, ps),
                Wrappers.<Coupon>lambdaQuery().orderByDesc(Coupon::getId));
        return PageResult.of(page.getTotal(), pn, ps, page.getRecords());
    }

    public Long create(CouponSaveDTO dto) {
        LoginContext.requireAdminId();
        if (!dto.getValidFrom().isBefore(dto.getValidTo())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "有效期起止时间不合法");
        }
        if ("PERCENT".equals(dto.getType()) && (dto.getValue() < 1 || dto.getValue() > 99)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "折扣百分比须在 1~99");
        }
        Coupon coupon = new Coupon();
        coupon.setTitle(dto.getTitle());
        coupon.setType(dto.getType());
        coupon.setValue(dto.getValue());
        coupon.setMinAmountCents(dto.getMinAmountCents());
        coupon.setTotalCount(dto.getTotalCount());
        coupon.setReceivedCount(0);
        coupon.setPerUserLimit(dto.getPerUserLimit());
        coupon.setValidFrom(dto.getValidFrom());
        coupon.setValidTo(dto.getValidTo());
        coupon.setStatus(1);
        couponMapper.insert(coupon);
        return coupon.getId();
    }

    public void updateStatus(Long id, Integer status) {
        LoginContext.requireAdminId();
        if (couponMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        Coupon update = new Coupon();
        update.setId(id);
        update.setStatus(status);
        couponMapper.updateById(update);
    }
}
