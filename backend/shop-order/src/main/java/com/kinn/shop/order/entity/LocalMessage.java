package com.kinn.shop.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.kinn.shop.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 本地消息表（最终一致性，M3 支付/通知链路投递用）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("local_message")
public class LocalMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** ORDER_PAID_NOTIFY 等 */
    private String bizType;

    private String bizKey;

    private String payload;

    /** 0待投递 1成功 2死信 */
    private Integer status;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;
}
