package com.kinn.shop.payment.channel;

/**
 * 渠道退款结果。
 *
 * @param status SUCCESS/FAILED/PROCESSING
 */
public record ChannelRefundResult(ChannelStatus status, String channelRefundNo) {
}
