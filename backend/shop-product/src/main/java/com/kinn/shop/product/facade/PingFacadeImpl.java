package com.kinn.shop.product.facade;

import com.kinn.shop.api.common.PingFacade;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(group = "product")
public class PingFacadeImpl implements PingFacade {

    @Override
    public String ping() {
        return "pong from shop-product";
    }
}