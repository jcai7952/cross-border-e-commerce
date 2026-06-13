package com.kinn.shop.logistics.vo;

import com.kinn.shop.logistics.entity.LogisticsTrack;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrackItemVO {

    private String nodeCode;
    private String nodeZh;
    private String nodeEn;
    private String location;
    private String remark;
    private LocalDateTime trackTime;

    public static TrackItemVO from(LogisticsTrack track) {
        TrackItemVO vo = new TrackItemVO();
        vo.setNodeCode(track.getNodeCode());
        vo.setNodeZh(track.getNodeZh());
        vo.setNodeEn(track.getNodeEn());
        vo.setLocation(track.getLocation());
        vo.setRemark(track.getRemark());
        vo.setTrackTime(track.getTrackTime());
        return vo;
    }
}
