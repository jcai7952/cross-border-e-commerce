package com.kinn.shop.logistics.controller;

import com.kinn.shop.common.core.Result;
import com.kinn.shop.logistics.service.TrackService;
import com.kinn.shop.logistics.vo.ShipmentTrackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "物流（买家端，需登录）")
@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final TrackService trackService;

    @Operation(summary = "按订单号查物流轨迹（仅本人订单，轨迹时间倒序）")
    @GetMapping("/track/{orderNo}")
    public Result<ShipmentTrackVO> track(@PathVariable String orderNo) {
        return Result.ok(trackService.track(orderNo));
    }
}
