package com.kinn.shop.logistics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinn.shop.common.context.LoginContext;
import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.logistics.dto.TemplateCreateDTO;
import com.kinn.shop.logistics.dto.TemplateUpdateDTO;
import com.kinn.shop.logistics.dto.ZoneSaveDTO;
import com.kinn.shop.logistics.entity.LogisticsOrder;
import com.kinn.shop.logistics.entity.ShippingTemplate;
import com.kinn.shop.logistics.entity.ShippingZone;
import com.kinn.shop.logistics.mapper.LogisticsOrderMapper;
import com.kinn.shop.logistics.mapper.ShippingTemplateMapper;
import com.kinn.shop.logistics.mapper.ShippingZoneMapper;
import com.kinn.shop.logistics.vo.ShipmentDetailVO;
import com.kinn.shop.logistics.vo.ShipmentPageVO;
import com.kinn.shop.logistics.vo.TemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 物流管理（管理端）：运费模板/区域计费维护、物流单查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogisticsService {

    private final ShippingTemplateMapper templateMapper;
    private final ShippingZoneMapper zoneMapper;
    private final LogisticsOrderMapper logisticsOrderMapper;
    private final TrackService trackService;
    private final ObjectMapper objectMapper;

    // ---------- 运费模板 ----------

    public List<TemplateVO> templateList() {
        LoginContext.requireAdminId();
        List<ShippingTemplate> templates = templateMapper.selectList(
                Wrappers.<ShippingTemplate>lambdaQuery().orderByAsc(ShippingTemplate::getId));
        if (templates.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ShippingZone>> zoneMap = zoneMapper.selectList(Wrappers.<ShippingZone>lambdaQuery()
                        .in(ShippingZone::getTemplateId, templates.stream().map(ShippingTemplate::getId).toList())
                        .orderByAsc(ShippingZone::getId))
                .stream().collect(Collectors.groupingBy(ShippingZone::getTemplateId));
        return templates.stream().map(t -> {
            TemplateVO vo = new TemplateVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setStatus(t.getStatus());
            vo.setCreateTime(t.getCreateTime());
            vo.setUpdateTime(t.getUpdateTime());
            vo.setZones(zoneMap.getOrDefault(t.getId(), List.of()));
            return vo;
        }).toList();
    }

    public Long createTemplate(TemplateCreateDTO dto) {
        LoginContext.requireAdminId();
        ShippingTemplate template = new ShippingTemplate();
        template.setName(dto.getName().trim());
        template.setStatus(1);
        templateMapper.insert(template);
        return template.getId();
    }

    public void updateTemplate(Long id, TemplateUpdateDTO dto) {
        LoginContext.requireAdminId();
        ShippingTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        template.setName(dto.getName().trim());
        template.setStatus(dto.getStatus());
        templateMapper.updateById(template);
    }

    // ---------- 区域计费 ----------

    public Long createZone(ZoneSaveDTO dto) {
        LoginContext.requireAdminId();
        requireTemplate(dto.getTemplateId());
        ShippingZone zone = new ShippingZone();
        applyZone(zone, dto);
        zoneMapper.insert(zone);
        return zone.getId();
    }

    public void updateZone(Long id, ZoneSaveDTO dto) {
        LoginContext.requireAdminId();
        ShippingZone zone = zoneMapper.selectById(id);
        if (zone == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        requireTemplate(dto.getTemplateId());
        applyZone(zone, dto);
        zoneMapper.updateById(zone);
    }

    public void deleteZone(Long id) {
        LoginContext.requireAdminId();
        if (zoneMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        zoneMapper.deleteById(id);
    }

    // ---------- 物流单 ----------

    public PageResult<ShipmentPageVO> shipmentPage(String orderNo, String status, long pageNum, long pageSize) {
        LoginContext.requireAdminId();
        long pn = Math.max(pageNum, 1);
        long ps = Math.min(Math.max(pageSize, 1), 100);
        Page<LogisticsOrder> page = logisticsOrderMapper.selectPage(new Page<>(pn, ps),
                Wrappers.<LogisticsOrder>lambdaQuery()
                        .eq(orderNo != null && !orderNo.isBlank(), LogisticsOrder::getOrderNo, orderNo)
                        .eq(status != null && !status.isBlank(), LogisticsOrder::getStatus, status)
                        .orderByDesc(LogisticsOrder::getId));
        List<ShipmentPageVO> list = page.getRecords().stream().map(ShipmentPageVO::from).toList();
        return PageResult.of(page.getTotal(), pn, ps, list);
    }

    public ShipmentDetailVO shipmentDetail(String shipmentNo) {
        LoginContext.requireAdminId();
        LogisticsOrder lo = logisticsOrderMapper.selectOne(Wrappers.<LogisticsOrder>lambdaQuery()
                .eq(LogisticsOrder::getShipmentNo, shipmentNo));
        if (lo == null) {
            throw new BizException(ErrorCode.SHIPMENT_NOT_FOUND);
        }
        ShipmentDetailVO vo = new ShipmentDetailVO();
        vo.setShipmentNo(lo.getShipmentNo());
        vo.setOrderNo(lo.getOrderNo());
        vo.setUserId(lo.getUserId());
        vo.setCarrier(lo.getCarrier());
        vo.setCountry(lo.getCountry());
        vo.setReceiver(fromJson(lo.getReceiverJson()));
        vo.setWeightG(lo.getWeightG());
        vo.setFeeCents(lo.getFeeCents());
        vo.setStatus(lo.getStatus());
        vo.setCurrentNode(lo.getCurrentNode());
        vo.setSignedAt(lo.getSignedAt());
        vo.setCreateTime(lo.getCreateTime());
        vo.setTracks(trackService.loadTracksDesc(lo.getShipmentNo()));
        return vo;
    }

    private void requireTemplate(Long templateId) {
        if (templateMapper.selectById(templateId) == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
    }

    private void applyZone(ShippingZone zone, ZoneSaveDTO dto) {
        zone.setTemplateId(dto.getTemplateId());
        zone.setZoneName(dto.getZoneName().trim());
        zone.setCountries(dto.getCountries().trim().toUpperCase());
        zone.setFirstWeightG(dto.getFirstWeightG());
        zone.setFirstFeeCents(dto.getFirstFeeCents());
        zone.setAddWeightG(dto.getAddWeightG());
        zone.setAddFeeCents(dto.getAddFeeCents());
        zone.setEstDaysMin(dto.getEstDaysMin());
        zone.setEstDaysMax(dto.getEstDaysMax());
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("[admin-logistics] receiver json parse failed: {}", e.getMessage());
            return null;
        }
    }
}
