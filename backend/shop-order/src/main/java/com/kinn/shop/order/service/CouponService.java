package com.kinn.shop.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.order.entity.Coupon;
import com.kinn.shop.order.entity.UserCoupon;
import com.kinn.shop.order.mapper.CouponMapper;
import com.kinn.shop.order.mapper.UserCouponMapper;
import com.kinn.shop.order.vo.CouponVO;
import com.kinn.shop.order.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 优惠券：领取走「条件 UPDATE 占名额」原子化，杜绝超发；
 * 每人限领在事务内先查后插（唯一竞态窗口极小，且超领只损平台不损用户，可接受）。
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    /** 可领券列表：启用且在有效期内 + 当前用户已领张数/是否可再领。 */
    public List<CouponVO> available() {
        long userId = LoginContext.requireUserId();
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                .eq(Coupon::getStatus, 1)
                .le(Coupon::getValidFrom, now)
                .ge(Coupon::getValidTo, now)
                .orderByDesc(Coupon::getId));
        if (coupons.isEmpty()) {
            return List.of();
        }
        List<Long> ids = coupons.stream().map(Coupon::getId).toList();
        Map<Long, Long> claimedMap = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                        .eq(UserCoupon::getUserId, userId)
                        .in(UserCoupon::getCouponId, ids))
                .stream().collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        return coupons.stream().map(c -> {
            CouponVO vo = new CouponVO();
            vo.setId(c.getId());
            vo.setTitle(c.getTitle());
            vo.setType(c.getType());
            vo.setValue(c.getValue());
            vo.setMinAmountCents(c.getMinAmountCents());
            vo.setTotalCount(c.getTotalCount());
            vo.setReceivedCount(c.getReceivedCount());
            vo.setPerUserLimit(c.getPerUserLimit());
            vo.setValidFrom(c.getValidFrom());
            vo.setValidTo(c.getValidTo());
            long claimed = claimedMap.getOrDefault(c.getId(), 0L);
            vo.setClaimedCount(claimed);
            vo.setCanClaim(claimed < c.getPerUserLimit()
                    && (c.getTotalCount() == 0 || c.getReceivedCount() < c.getTotalCount()));
            return vo;
        }).toList();
    }

    /** 领取：限领校验 → 原子占名额（行数 0 即不可领）→ 插持券记录。 */
    @Transactional
    public void claim(Long couponId) {
        long userId = LoginContext.requireUserId();
        Coupon coupon = couponMapper.selectById(couponId);
        LocalDateTime now = LocalDateTime.now();
        if (coupon == null || coupon.getStatus() != 1
                || now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        long claimed = userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        if (claimed >= coupon.getPerUserLimit()) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        if (couponMapper.claimOne(couponId) == 0) {
            throw new BizException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        UserCoupon uc = new UserCoupon();
        uc.setCouponId(couponId);
        uc.setUserId(userId);
        uc.setStatus(0);
        userCouponMapper.insert(uc);
    }

    /** 我的券：先把过期未用的批量置 2，再按状态过滤（status 为空返回全部）。 */
    public List<UserCouponVO> mine(Integer status) {
        long userId = LoginContext.requireUserId();
        userCouponMapper.expireUnused(userId);
        List<UserCoupon> list = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(status != null, UserCoupon::getStatus, status)
                .orderByDesc(UserCoupon::getId));
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> couponIds = list.stream().map(UserCoupon::getCouponId).distinct().toList();
        Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));
        return list.stream().map(uc -> {
            UserCouponVO vo = new UserCouponVO();
            vo.setId(uc.getId());
            vo.setCouponId(uc.getCouponId());
            vo.setStatus(uc.getStatus());
            vo.setOrderNo(uc.getOrderNo());
            vo.setReceivedAt(uc.getReceivedAt());
            vo.setUsedAt(uc.getUsedAt());
            Coupon c = couponMap.get(uc.getCouponId());
            if (c != null) {
                vo.setTitle(c.getTitle());
                vo.setType(c.getType());
                vo.setValue(c.getValue());
                vo.setMinAmountCents(c.getMinAmountCents());
                vo.setValidFrom(c.getValidFrom());
                vo.setValidTo(c.getValidTo());
            }
            return vo;
        }).toList();
    }
}
