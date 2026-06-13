package com.kinn.shop.payment.channel;

import java.util.Map;

/**
 * 发起支付结果：渠道单号 + 前端载荷。
 *
 * @param channelTradeNo 渠道单号（pi_xxx / PayPal order id / SIM+payNo）
 * @param payloadType    前端处理方式
 * @param payload        前端载荷（redirectUrl 或 clientSecret/publishableKey）
 */
public record ChannelCreateResult(String channelTradeNo, PayloadType payloadType, Map<String, Object> payload) {
}
