package com.kinn.shop.payment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@Schema(description = "发起支付结果")
public class PayCreateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String payNo;

    private String channel;

    @Schema(description = "REDIRECT 跳 payload.redirectUrl；CLIENT_SECRET 用 payload.clientSecret 走渠道 JS SDK")
    private String payloadType;

    @Schema(description = "前端载荷：redirectUrl 或 clientSecret/publishableKey")
    private Map<String, Object> payload;
}
