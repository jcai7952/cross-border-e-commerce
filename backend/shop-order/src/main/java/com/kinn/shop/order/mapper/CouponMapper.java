package com.kinn.shop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kinn.shop.order.entity.Coupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 原子领取：仅在启用且未领罄时占用一个名额，返回受影响行数（0=不可领）。
     */
    @Update("UPDATE coupon SET received_count = received_count + 1 " +
            "WHERE id = #{id} AND status = 1 AND (total_count = 0 OR received_count < total_count)")
    int claimOne(@Param("id") Long id);
}
