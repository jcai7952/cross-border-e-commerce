package com.kinn.shop.logistics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.logistics.entity.LogisticsOrder;
import com.kinn.shop.logistics.entity.LogisticsTrack;
import com.kinn.shop.logistics.mapper.LogisticsOrderMapper;
import com.kinn.shop.logistics.mapper.LogisticsTrackMapper;
import com.kinn.shop.logistics.vo.ShipmentTrackVO;
import com.kinn.shop.logistics.vo.TrackItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 买家端物流轨迹查询（只能看自己的物流单）。
 */
@Service
@RequiredArgsConstructor
public class TrackService {

    private final LogisticsOrderMapper logisticsOrderMapper;
    private final LogisticsTrackMapper logisticsTrackMapper;

    /** 按订单号查物流单+轨迹（时间倒序）；非本人订单按不存在处理。 */
    public ShipmentTrackVO track(String orderNo) {
        long userId = LoginContext.requireUserId();
        LogisticsOrder lo = logisticsOrderMapper.selectOne(Wrappers.<LogisticsOrder>lambdaQuery()
                .eq(LogisticsOrder::getOrderNo, orderNo));
        if (lo == null || !Long.valueOf(userId).equals(lo.getUserId())) {
            throw new BizException(ErrorCode.SHIPMENT_NOT_FOUND);
        }
        ShipmentTrackVO vo = new ShipmentTrackVO();
        vo.setShipmentNo(lo.getShipmentNo());
        vo.setCarrier(lo.getCarrier());
        vo.setCountry(lo.getCountry());
        vo.setStatus(lo.getStatus());
        vo.setCurrentNode(lo.getCurrentNode());
        vo.setTracks(loadTracksDesc(lo.getShipmentNo()));
        return vo;
    }

    /** 轨迹按时间倒序（同秒落库按 id 倒序兜底）。 */
    List<TrackItemVO> loadTracksDesc(String shipmentNo) {
        return logisticsTrackMapper.selectList(Wrappers.<LogisticsTrack>lambdaQuery()
                        .eq(LogisticsTrack::getShipmentNo, shipmentNo)
                        .orderByDesc(LogisticsTrack::getTrackTime)
                        .orderByDesc(LogisticsTrack::getId))
                .stream().map(TrackItemVO::from).toList();
    }
}
