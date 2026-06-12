package com.kinn.shop.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 区域计费（首重+续重；表无 create_time/update_time，不继承 BaseEntity）。
 */
@Data
@TableName("shipping_zone")
public class ShippingZone implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String zoneName;

    /** 逗号分隔 ISO2，如 US,CA */
    private String countries;

    /** 首重克数 */
    private Integer firstWeightG;

    /** 首重运费 USD 分 */
    private Long firstFeeCents;

    /** 续重克数（每满一档加一次续重费） */
    private Integer addWeightG;

    /** 续重运费 USD 分 */
    private Long addFeeCents;

    private Integer estDaysMin;

    private Integer estDaysMax;
}
