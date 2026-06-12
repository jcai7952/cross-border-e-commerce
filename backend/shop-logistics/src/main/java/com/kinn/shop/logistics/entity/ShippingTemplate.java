package com.kinn.shop.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 运费模板。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shipping_template")
public class ShippingTemplate extends BaseEntity {

    private String name;

    /** 1 启用 0 停用 */
    private Integer status;
}
