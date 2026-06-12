package com.kinn.shop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 目的国税则（表无时间列，不继承 BaseEntity）。
 */
@Data
@TableName("tax_rule")
public class TaxRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目的国 ISO 3166-1 alpha-2 */
    private String countryCode;

    /** CN_CROSS_BORDER 中国跨境（保税9.1%/直邮行邮税）/ VAT / NONE */
    private String taxType;

    /** VAT 税率%（CN 模式忽略此列，按类目行邮税计算） */
    private BigDecimal ratePercent;

    /** 免税额度 USD 分（de minimis，如美国 800 美元；超额关税建模不在本期范围） */
    private Long thresholdCents;

    /** 清关是否需订购人实名：1需要 0不需要 */
    private Integer identityRequired;

    private String note;
}
