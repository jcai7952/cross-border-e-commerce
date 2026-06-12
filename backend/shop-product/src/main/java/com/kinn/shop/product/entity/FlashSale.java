package com.kinn.shop.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 限时闪购活动。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("flash_sale")
public class FlashSale extends BaseEntity {

    private String title;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 1 启用 0 停用 */
    private Integer status;
}
