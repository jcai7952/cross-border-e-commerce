package com.kinn.shop.logistics.facade;

import com.kinn.shop.api.common.PingFacade;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(group = "logistics")
public class PingFacadeImpl implements PingFacade {

    @Override
    public String ping() {
        return "pong from shop-logistics";
    }
}