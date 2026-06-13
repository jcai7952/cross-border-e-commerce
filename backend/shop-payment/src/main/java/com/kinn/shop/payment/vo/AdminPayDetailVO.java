package com.kinn.shop.payment.vo;

import com.kinn.shop.payment.entity.PayOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "支付单详情（管理端，含退款列表）")
public class AdminPayDetailVO implements Serializable {

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

    @Schema(description = "渠道响应快照")
    private String channelPayload;

    private LocalDateTime paidAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Schema(description = "已退款合计（SUCCESS，支付币最小单位）")
    private Long refundedCents;

    private List<RefundVO> refunds;

    public static AdminPayDetailVO from(PayOrder po) {
        AdminPayDetailVO vo = new AdminPayDetailVO();
        vo.setPayNo(po.getPayNo());
        vo.setOrderNo(po.getOrderNo());
        vo.setUserId(po.getUserId());
        vo.setChannel(po.getChannel());
        vo.setAmountCents(po.getAmountCents());
        vo.setCurrency(po.getCurrency());
        vo.setStatus(po.getStatus());
        vo.setChannelTradeNo(po.getChannelTradeNo());
        vo.setChannelPayload(po.getChannelPayload());
        vo.setPaidAt(po.getPaidAt());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }
}
