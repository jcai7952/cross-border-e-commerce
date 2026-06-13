package com.kinn.shop.logistics.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.logistics.entity.LogisticsOrder;
import com.kinn.shop.logistics.entity.LogisticsTrack;
import com.kinn.shop.logistics.mapper.LogisticsOrderMapper;
import com.kinn.shop.logistics.mapper.LogisticsTrackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 轨迹推进：每次把一张在途物流单向后推进一个节点。
 * 幂等：推进前回查当前节点 + CAS 条件更新（单实例场景简单处理）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackAdvanceService {

    private final LogisticsOrderMapper logisticsOrderMapper;
    private final LogisticsTrackMapper logisticsTrackMapper;

    /**
     * 推进一个节点：CAS 更新 current_node（到 SIGNED 同时置终态）+ 插入轨迹。
     * 返回本次推进到 SIGNED 的 orderNo（供调用方通知订单完成），其余情况返回 null。
     */
    @Transactional(rollbackFor = Exception.class)
    public String advanceOne(long logisticsOrderId) {
        // 幂等：推进前 SELECT 当前节点，防止拿批次快照重复推进
        LogisticsOrder lo = logisticsOrderMapper.selectById(logisticsOrderId);
        if (lo == null || !"IN_TRANSIT".equals(lo.getStatus())) {
            return null;
        }
        TrackNode current = TrackNode.of(lo.getCurrentNode());
        TrackNode next = current == null ? TrackNode.PICKED : current.next();
        if (next == null) {
            return null; // 已到终点
        }
        boolean signed = next == TrackNode.SIGNED;
        LambdaUpdateWrapper<LogisticsOrder> uw = Wrappers.<LogisticsOrder>lambdaUpdate()
                .eq(LogisticsOrder::getId, lo.getId())
                .eq(LogisticsOrder::getStatus, "IN_TRANSIT");
        if (lo.getCurrentNode() == null) {
            uw.isNull(LogisticsOrder::getCurrentNode);
        } else {
            uw.eq(LogisticsOrder::getCurrentNode, lo.getCurrentNode());
        }
        uw.set(LogisticsOrder::getCurrentNode, next.name())
                .set(signed, LogisticsOrder::getStatus, "SIGNED")
                .set(signed, LogisticsOrder::getSignedAt, LocalDateTime.now());
        if (logisticsOrderMapper.update(null, uw) == 0) {
            return null; // 已被并发推进，跳过
        }

        LogisticsTrack track = new LogisticsTrack();
        track.setShipmentNo(lo.getShipmentNo());
        track.setNodeCode(next.name());
        track.setNodeZh(next.getZh());
        track.setNodeEn(next.getEn());
        track.setLocation(next.locationOf(lo.getCountry()));
        if (next == TrackNode.IMPORT_CUSTOMS) {
            track.setRemark("关税核验通过 / Duty verified");
        }
        track.setTrackTime(LocalDateTime.now());
        logisticsTrackMapper.insert(track);
        log.info("[track] {} advanced to {}", lo.getShipmentNo(), next.name());
        return signed ? lo.getOrderNo() : null;
    }
}
