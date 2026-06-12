package com.kinn.shop.order.config;

import com.kinn.shop.api.logistics.LogisticsFacade;
import com.kinn.shop.api.product.ProductTradeFacade;
import com.kinn.shop.api.user.UserFacade;
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
    private UserFacade userFacade;

    @DubboReference(check = false)
    private ProductTradeFacade productTradeFacade;

    @DubboReference(check = false)
    private LogisticsFacade logisticsFacade;

    @Bean
    public UserFacade userFacade() {
        return userFacade;
    }

    @Bean
    public ProductTradeFacade productTradeFacade() {
        return productTradeFacade;
    }

    @Bean
    public LogisticsFacade logisticsFacade() {
        return logisticsFacade;
    }
}
