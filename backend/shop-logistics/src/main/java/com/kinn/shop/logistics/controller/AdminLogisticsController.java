package com.kinn.shop.logistics.controller;

import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.logistics.dto.TemplateCreateDTO;
import com.kinn.shop.logistics.dto.TemplateUpdateDTO;
import com.kinn.shop.logistics.dto.ZoneSaveDTO;
import com.kinn.shop.logistics.service.AdminLogisticsService;
import com.kinn.shop.logistics.vo.ShipmentDetailVO;
import com.kinn.shop.logistics.vo.ShipmentPageVO;
import com.kinn.shop.logistics.vo.TemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "物流（管理端）")
@RestController
@RequestMapping("/api/admin/logistics")
@RequiredArgsConstructor
public class AdminLogisticsController {

    private final AdminLogisticsService adminLogisticsService;

    @Operation(summary = "运费模板列表（含各自区域计费全量）")
    @GetMapping("/template/list")
    public Result<List<TemplateVO>> templateList() {
        return Result.ok(adminLogisticsService.templateList());
    }

    @Operation(summary = "创建运费模板（默认启用）")
    @PostMapping("/template")
    public Result<Long> createTemplate(@Valid @RequestBody TemplateCreateDTO dto) {
        return Result.ok(adminLogisticsService.createTemplate(dto));
    }

    @Operation(summary = "更新运费模板（名称/启停）")
    @PutMapping("/template/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateUpdateDTO dto) {
        adminLogisticsService.updateTemplate(id, dto);
        return Result.ok();
    }

    @Operation(summary = "新增区域计费（首重+续重）")
    @PostMapping("/zone")
    public Result<Long> createZone(@Valid @RequestBody ZoneSaveDTO dto) {
        return Result.ok(adminLogisticsService.createZone(dto));
    }

    @Operation(summary = "更新区域计费")
    @PutMapping("/zone/{id}")
    public Result<Void> updateZone(@PathVariable Long id, @Valid @RequestBody ZoneSaveDTO dto) {
        adminLogisticsService.updateZone(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除区域计费")
    @DeleteMapping("/zone/{id}")
    public Result<Void> deleteZone(@PathVariable Long id) {
        adminLogisticsService.deleteZone(id);
        return Result.ok();
    }

    @Operation(summary = "物流单分页（含当前轨迹节点）")
    @GetMapping("/shipment/page")
    public Result<PageResult<ShipmentPageVO>> shipmentPage(@RequestParam(required = false) String orderNo,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(defaultValue = "1") long pageNum,
                                                           @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(adminLogisticsService.shipmentPage(orderNo, status, pageNum, pageSize));
    }

    @Operation(summary = "物流单详情（含收件快照与全量轨迹）")
    @GetMapping("/shipment/{shipmentNo}")
    public Result<ShipmentDetailVO> shipmentDetail(@PathVariable String shipmentNo) {
        return Result.ok(adminLogisticsService.shipmentDetail(shipmentNo));
    }
}
