package com.kinn.shop.logistics.vo;

import lombok.Data;

import java.util.List;

/** 买家端物流轨迹视图。 */
@Data
public class ShipmentTrackVO {

    private String shipmentNo;
    private String carrier;
    private String country;
    /** CREATED / IN_TRANSIT / SIGNED */
    private String status;
    private String currentNode;
    /** 轨迹按时间倒序 */
    private List<TrackItemVO> tracks;
}
