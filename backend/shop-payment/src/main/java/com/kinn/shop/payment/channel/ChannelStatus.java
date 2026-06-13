package com.kinn.shop.payment.channel;

/**
 * 渠道侧状态归一：查询用 SUCCESS/FAILED/PENDING；退款用 SUCCESS/FAILED/PROCESSING。
 */
public enum ChannelStatus {

    SUCCESS,

    FAILED,

    PENDING,

    PROCESSING
}
