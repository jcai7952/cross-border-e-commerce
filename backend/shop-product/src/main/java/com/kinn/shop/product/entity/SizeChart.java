package com.kinn.shop.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 尺码表（类目级，表无时间列，不继承 BaseEntity）。
 */
@Data
@TableName("size_chart")
public class SizeChart implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String locale;

    /** JSON 数组字符串 */
    private String contentJson;
}
