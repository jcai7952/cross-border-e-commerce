package com.kinn.shop.payment.channel;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kinn.shop.payment.config.PayProperties;
import com.kinn.shop.payment.entity.PayNotifyLog;
import com.kinn.shop.payment.entity.PayOrder;
import com.kinn.shop.payment.entity.RefundOrder;
import com.kinn.shop.payment.mapper.PayNotifyLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 模拟渠道（无条件注册，默认渠道）：跳内置收银台页面，按钮回调走统一幂等入账。
 * 本地开发/演示用，生产配置真实渠道后前端不暴露该入口即可。
 */
@Component
@RequiredArgsConstructor
public class SimulatorChannel implements PayChannel {

    public static final String CODE = "SIMULATOR";

    private final PayProperties payProperties;
    private final PayNotifyLogMapper payNotifyLogMapper;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public ChannelCreateResult create(PayOrder po) {
        String redirectUrl = payProperties.getApiBase() + "/api/pay/simulator/" + po.getPayNo();
        return new ChannelCreateResult("SIM" + po.getPayNo(), PayloadType.REDIRECT, Map.of("redirectUrl", redirectUrl));
    }

    @Override
    public ChannelRefundResult refund(RefundOrder ro, PayOrder po) {
        return new ChannelRefundResult(ChannelStatus.SUCCESS, "SIMRF" + ro.getRefundNo());
    }

    @Override
    public ChannelQueryResult query(PayOrder po) {
        Long success = payNotifyLogMapper.selectCount(Wrappers.<PayNotifyLog>lambdaQuery()
                .eq(PayNotifyLog::getChannel, CODE)
                .eq(PayNotifyLog::getEventId, "SIMEVT-" + po.getPayNo() + "-SUCCESS"));
        return new ChannelQueryResult(success != null && success > 0 ? ChannelStatus.SUCCESS : ChannelStatus.PENDING,
                po.getChannelTradeNo());
    }
}
