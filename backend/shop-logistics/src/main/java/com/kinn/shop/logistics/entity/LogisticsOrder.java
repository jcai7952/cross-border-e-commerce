package com.kinn.shop.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 物流单（M4 履约链路使用）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("logistics_order")
public class LogisticsOrder extends BaseEntity {

    private String shipmentNo;

    private String orderNo;

    private Long userId;

    private String carrier;

    /** 目的国 ISO2 */
    private String country;

    /** 收件人快照 JSON */
    private String receiverJson;

    private Integer weightG;

    /** 运费 USD 分 */
    private Long feeCents;

    /** CREATED / IN_TRANSIT / SIGNED */
    private String status;

    /** 当前轨迹节点 code */
    private String currentNode;

    private LocalDateTime signedAt;
}
