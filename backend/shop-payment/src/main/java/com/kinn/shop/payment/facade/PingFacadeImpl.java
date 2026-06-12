package com.kinn.shop.payment.facade;

import com.kinn.shop.api.common.PingFacade;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(group = "payment")
public class PingFacadeImpl implements PingFacade {

    @Override
    public String ping() {
        return "pong from shop-payment";
    }
}