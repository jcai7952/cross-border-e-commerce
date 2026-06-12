package com.kinn.shop.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 币种（主键为 ISO 代码字符串，不继承 BaseEntity）。
 */
@Data
@TableName("currency")
public class Currency implements Serializable {

    @TableId(value = "code", type = IdType.INPUT)
    private String code;

    private String symbol;

    private String nameZh;

    private String nameEn;

    /** 小数位：USD/CNY/EUR=2，JPY=0 */
    private Integer decimalDigits;

    private Integer sort;

    private Integer enabled;
}
