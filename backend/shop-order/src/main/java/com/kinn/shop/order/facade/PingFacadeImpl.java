package com.kinn.shop.order.facade;

import com.kinn.shop.api.common.PingFacade;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(group = "order")
public class PingFacadeImpl implements PingFacade {

    @Override
    public String ping() {
        return "pong from shop-order";
    }
}