package com.kinn.shop.order.vo;

import com.kinn.shop.order.entity.OrderStatusLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "订单状态流转记录")
public class StatusLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fromStatus;

    private String toStatus;

    @Schema(description = "user:1 / admin:1 / system")
    private String operator;

    private String remark;

    private LocalDateTime createTime;

    public static StatusLogVO from(OrderStatusLog log) {
        StatusLogVO vo = new StatusLogVO();
        vo.setFromStatus(log.getFromStatus());
        vo.setToStatus(log.getToStatus());
        vo.setOperator(log.getOperator());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }
}
