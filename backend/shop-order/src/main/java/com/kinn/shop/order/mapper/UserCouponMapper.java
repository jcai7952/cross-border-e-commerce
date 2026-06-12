package com.kinn.shop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kinn.shop.order.entity.UserCoupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 把该用户过期未用的券批量置为过期（status 0 -> 2），查询「我的券」时顺手清理。
     */
    @Update("UPDATE user_coupon uc JOIN coupon c ON uc.coupon_id = c.id " +
            "SET uc.status = 2 " +
            "WHERE uc.user_id = #{userId} AND uc.status = 0 AND c.valid_to < NOW()")
    int expireUnused(@Param("userId") Long userId);
}
