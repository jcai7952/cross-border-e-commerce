package com.kinn.shop.product.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 汇率（base=USD；表无 create_time，不继承 BaseEntity；update_time 由 DB 维护）。
 */
@Data
@TableName("exchange_rate")
public class ExchangeRate implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String baseCurrency;

    private String quoteCurrency;

    /** 1 base = rate quote */
    private BigDecimal rate;

    /** API / MANUAL */
    private String source;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
