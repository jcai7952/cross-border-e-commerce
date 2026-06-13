package com.kinn.shop.payment.channel;

/**
 * 渠道侧状态查询结果。
 *
 * @param status SUCCESS/FAILED/PENDING
 */
public record ChannelQueryResult(ChannelStatus status, String channelTradeNo) {
}
