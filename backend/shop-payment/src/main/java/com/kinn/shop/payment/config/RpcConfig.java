package com.kinn.shop.payment.config;

import com.kinn.shop.api.order.OrderFacade;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 三方服务 Dubbo 引用统一注册为 Spring Bean，业务层一律构造器注入。
 * check=false：启动不强依赖提供方在线（联调时各服务可乱序启动）。
 */
@Configuration
public class RpcConfig {

    @DubboReference(check = false)
    private OrderFacade orderFacade;

    @Bean
    public OrderFacade orderFacade() {
        return orderFacade;
    }
}
