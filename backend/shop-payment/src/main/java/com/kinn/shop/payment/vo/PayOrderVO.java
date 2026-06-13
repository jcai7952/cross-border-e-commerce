package com.kinn.shop.payment.vo;

import com.kinn.shop.payment.entity.PayOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "支付单（用户视角）")
public class PayOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String payNo;

    private String orderNo;

    private String channel;

    @Schema(description = "CREATED/PENDING/SUCCESS/FAILED/CLOSED")
    private String status;

    @Schema(description = "支付币最小单位金额")
    private Long amountCents;

    private String currency;

    private LocalDateTime paidAt;

    private LocalDateTime createTime;

    public static PayOrderVO from(PayOrder po) {
        PayOrderVO vo = new PayOrderVO();
        vo.setPayNo(po.getPayNo());
        vo.setOrderNo(po.getOrderNo());
        vo.setChannel(po.getChannel());
        vo.setStatus(po.getStatus());
        vo.setAmountCents(po.getAmountCents());
        vo.setCurrency(po.getCurrency());
        vo.setPaidAt(po.getPaidAt());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
