package com.kinn.shop.logistics.vo;

import com.kinn.shop.logistics.entity.LogisticsOrder;
import lombok.Data;

import java.time.LocalDateTime;

/** 物流单分页行（管理端）。 */
@Data
public class ShipmentPageVO {

    private String shipmentNo;
    private String orderNo;
    private Long userId;
    private String carrier;
    private String country;
    private Integer weightG;
    /** 运费 USD 分 */
    private Long feeCents;
    /** CREATED / IN_TRANSIT / SIGNED */
    private String status;
    private String currentNode;
    private LocalDateTime signedAt;
    private LocalDateTime createTime;

    public static ShipmentPageVO from(LogisticsOrder lo) {
        ShipmentPageVO vo = new ShipmentPageVO();
        vo.setShipmentNo(lo.getShipmentNo());
        vo.setOrderNo(lo.getOrderNo());
        vo.setUserId(lo.getUserId());
        vo.setCarrier(lo.getCarrier());
        vo.setCountry(lo.getCountry());
        vo.setWeightG(lo.getWeightG());
        vo.setFeeCents(lo.getFeeCents());
        vo.setStatus(lo.getStatus());
        vo.setCurrentNode(lo.getCurrentNode());
        vo.setSignedAt(lo.getSignedAt());
        vo.setCreateTime(lo.getCreateTime());
        return vo;
    }
}
