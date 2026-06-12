package com.kinn.shop.api.common;

/**
 * 连通性探活接口：每个服务以 group=服务名 暴露，用于验证 Dubbo/Nacos 链路。
 */
public interface PingFacade {

    String ping();
}
