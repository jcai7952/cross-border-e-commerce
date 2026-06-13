package com.kinn.shop.payment.channel;

/**
 * 发起支付后前端的处理方式：跳转渠道收银台 / 用 clientSecret 走渠道 JS SDK。
 */
public enum PayloadType {

    REDIRECT,

    CLIENT_SECRET
}
