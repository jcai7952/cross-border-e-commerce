package com.kinn.shop.payment.channel;

import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 渠道注册表：收集所有 PayChannel Bean。
 * 未启用的渠道（@ConditionalOnProperty 不成立）不会成为 Bean，自然取不到。
 */
@Component
public class ChannelRegistry {

    private final Map<String, PayChannel> channels;

    public ChannelRegistry(List<PayChannel> channelList) {
        this.channels = channelList.stream()
                .collect(Collectors.toUnmodifiableMap(PayChannel::code, Function.identity()));
    }

    /** 按渠道码取适配器，不存在/未启用抛 PAY_CHANNEL_UNAVAILABLE。 */
    public PayChannel byCode(String code) {
        PayChannel channel = code == null ? null : channels.get(code.toUpperCase(Locale.ROOT));
        if (channel == null) {
            throw new BizException(ErrorCode.PAY_CHANNEL_UNAVAILABLE);
        }
        return channel;
    }
}
