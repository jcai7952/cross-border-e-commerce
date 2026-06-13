package com.kinn.shop.order.controller;

import com.kinn.shop.common.core.PageResult;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.order.dto.OrderCreateDTO;
import com.kinn.shop.order.service.OrderService;
import com.kinn.shop.order.vo.OrderCreateVO;
import com.kinn.shop.order.vo.OrderDetailVO;
import com.kinn.shop.order.vo.OrderPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "订单（需登录）")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final com.kinn.shop.order.service.OrderActionService orderActionService;

    @Operation(summary = "下单（服务端全量重算金额，锁定汇率，30 分钟支付时限）")
    @PostMapping("/create")
    public Result<OrderCreateVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.ok(orderService.create(dto));
    }

    @Operation(summary = "我的订单分页（含明细摘要、支付币金额、待支付倒计时）")
    @GetMapping("/page")
    public Result<PageResult<OrderPageVO>> page(@RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(defaultValue = "USD") String currency,
                                                @RequestParam(defaultValue = "en-US") String locale) {
        return Result.ok(orderService.page(status, pageNum, pageSize, currency, locale));
    }

    @Operation(summary = "订单详情（金额四件套 USD 分+支付币显示、快照、时间线）")
    @GetMapping("/{orderNo}")
    public Result<OrderDetailVO> detail(@PathVariable String orderNo,
                                        @RequestParam(defaultValue = "USD") String currency,
                                        @RequestParam(defaultValue = "en-US") String locale) {
        return Result.ok(orderService.detail(orderNo, currency, locale));
    }

    @Operation(summary = "取消订单（仅 WAIT_PAY；回滚库存、退券）")
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo) {
        orderService.cancel(orderNo);
        return Result.ok();
    }

    @Operation(summary = "确认收货（SHIPPED→FINISHED）")
    @PostMapping("/{orderNo}/confirm")
    public Result<Void> confirm(@PathVariable String orderNo) {
        orderActionService.confirm(orderNo);
        return Result.ok();
    }
}
