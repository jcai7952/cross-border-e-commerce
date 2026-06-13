package com.kinn.shop.payment.vo;

import com.kinn.shop.payment.entity.PayOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "支付流水（管理端分页）")
public class AdminPayPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String payNo;

    private String orderNo;

    private Long userId;

    private String channel;

    @Schema(description = "支付币最小单位金额")
    private Long amountCents;

    private String currency;

    private String status;

    private String channelTradeNo;

    private LocalDateTime paidAt;

    private LocalDateTime createTime;

    public static AdminPayPageVO from(PayOrder po) {
        AdminPayPageVO vo = new AdminPayPageVO();
        vo.setPayNo(po.getPayNo());
        vo.setOrderNo(po.getOrderNo());
        vo.setUserId(po.getUserId());
        vo.setChannel(po.getChannel());
        vo.setAmountCents(po.getAmountCents());
        vo.setCurrency(po.getCurrency());
        vo.setStatus(po.getStatus());
        vo.setChannelTradeNo(po.getChannelTradeNo());
        vo.setPaidAt(po.getPaidAt());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
