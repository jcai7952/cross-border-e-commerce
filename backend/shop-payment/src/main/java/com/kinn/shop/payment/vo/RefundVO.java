package com.kinn.shop.payment.vo;

import com.kinn.shop.payment.entity.RefundOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "退款单")
public class RefundVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String refundNo;

    private String payNo;

    private String orderNo;

    private Long userId;

    @Schema(description = "退款金额（支付币最小单位）")
    private Long amountCents;

    private String currency;

    @Schema(description = "PROCESSING/SUCCESS/FAILED")
    private String status;

    private String channelRefundNo;

    private String reason;

    private LocalDateTime refundedAt;

    private LocalDateTime createTime;

    public static RefundVO from(RefundOrder ro) {
        RefundVO vo = new RefundVO();
        vo.setRefundNo(ro.getRefundNo());
        vo.setPayNo(ro.getPayNo());
        vo.setOrderNo(ro.getOrderNo());
        vo.setUserId(ro.getUserId());
        vo.setAmountCents(ro.getAmountCents());
        vo.setCurrency(ro.getCurrency());
        vo.setStatus(ro.getStatus());
        vo.setChannelRefundNo(ro.getChannelRefundNo());
        vo.setReason(ro.getReason());
        vo.setRefundedAt(ro.getRefundedAt());
        vo.setCreateTime(ro.getCreateTime());
        return vo;
    }
}
