package com.kinn.shop.logistics.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 物流单详情 + 轨迹（管理端）。 */
@Data
public class ShipmentDetailVO {

    private String shipmentNo;
    private String orderNo;
    private Long userId;
    private String carrier;
    private String country;
    /** 收件人快照（receiver_json 解析） */
    private Map<String, Object> receiver;
    private Integer weightG;
    /** 运费 USD 分 */
    private Long feeCents;
    /** CREATED / IN_TRANSIT / SIGNED */
    private String status;
    private String currentNode;
    private LocalDateTime signedAt;
    private LocalDateTime createTime;
    /** 轨迹按时间倒序 */
    private List<TrackItemVO> tracks;
}
