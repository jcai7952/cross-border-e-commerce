package com.kinn.shop.payment.channel;

import com.kinn.shop.payment.entity.PayOrder;
import com.kinn.shop.payment.entity.RefundOrder;

/**
 * 渠道适配器 SPI：注册为 Spring Bean 后由 {@link ChannelRegistry} 收集。
 */
public interface PayChannel {

    /** SIMULATOR / STRIPE / PAYPAL */
    String code();

    /** 发起支付，返回前端所需载荷。 */
    ChannelCreateResult create(PayOrder po);

    /** 渠道退款。 */
    ChannelRefundResult refund(RefundOrder ro, PayOrder po);

    /** 主动查询渠道侧状态（本地收不到 webhook 时的兜底）。 */
    ChannelQueryResult query(PayOrder po);
}
