package com.kinn.shop.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收货地址。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_address")
public class UserAddress extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String receiverName;

    private String phone;

    /** ISO 3166-1 alpha-2 */
    private String countryCode;

    /** 州/省 */
    private String state;

    private String city;

    private String addressLine1;

    private String addressLine2;

    private String postcode;

    /** 1默认 0否 */
    private Integer isDefault;
}
