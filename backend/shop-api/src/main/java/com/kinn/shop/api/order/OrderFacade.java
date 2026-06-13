package com.kinn.shop.api.order;

import com.kinn.shop.api.order.dto.OrderPayInfoDTO;

/**
 * 订单服务对内 RPC：支付服务创建支付单前校验、回调后驱动订单状态。
 */
public interface OrderFacade {

    /** 支付前取订单信息；查无返回 null。 */
    OrderPayInfoDTO getPayInfo(String orderNo);

    /**
     * 支付成功回调：WAIT_PAY→PAID（幂等：已是 PAID 直接返回 true）。
     * 返回 false 表示订单已被关闭等不可支付状态（调用方应走退款/人工流程）。
     */
    boolean markPaid(String orderNo, String payNo, String channel);

    /**
     * 全额退款后关闭订单：PAID→CLOSED（已发货订单不允许，返回 false）。
     * 成功后回滚库存（券不退还）。幂等：已 CLOSED 返回 true。
     */
    boolean markRefunded(String orderNo, String refundNo);

    /**
     * 物流签收驱动订单完成：SHIPPED→FINISHED。幂等：已 FINISHED 返回 true。
     */
    boolean markFinished(String orderNo);

    /**
     * 评论资格校验：订单属于该用户且已完成(FINISHED)时返回订单内商品 id 列表，
     * 否则返回空列表。
     */
    java.util.List<Long> getReviewableProductIds(String orderNo, long userId);
}
