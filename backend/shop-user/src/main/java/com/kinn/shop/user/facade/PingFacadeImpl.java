package com.kinn.shop.user.facade;

import com.kinn.shop.api.common.PingFacade;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(group = "user")
public class PingFacadeImpl implements PingFacade {

    @Override
    public String ping() {
        return "pong from shop-user";
    }
}
