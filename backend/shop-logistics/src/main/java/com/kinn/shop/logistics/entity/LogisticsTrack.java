package com.kinn.shop.logistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流轨迹（M4 履约链路使用；表无 create_time/update_time，不继承 BaseEntity）。
 */
@Data
@TableName("logistics_track")
public class LogisticsTrack implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String shipmentNo;

    /** PICKED/EXPORT_CUSTOMS/INTL_TRANSIT/IMPORT_CUSTOMS/DELIVERING/SIGNED */
    private String nodeCode;

    private String nodeZh;

    private String nodeEn;

    private String location;

    private String remark;

    private LocalDateTime trackTime;
}
