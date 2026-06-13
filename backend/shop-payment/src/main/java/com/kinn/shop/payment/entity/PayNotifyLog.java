package com.kinn.shop.payment.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 渠道回调日志：uk(channel, event_id) 作 DB 层幂等兜底。
 * 表无 update_time，不继承 BaseEntity。
 */
@Data
@TableName("pay_notify_log")
public class PayNotifyLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String channel;

    /** 渠道事件ID（Stripe evt_xxx），幂等键 */
    private String eventId;

    private String payload;

    private String result;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
}
