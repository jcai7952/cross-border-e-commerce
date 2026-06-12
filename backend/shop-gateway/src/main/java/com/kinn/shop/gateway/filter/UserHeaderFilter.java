package com.kinn.shop.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 登录态透传：剥离外部伪造的 X-User-* 头；按 token 解析 loginId（u_/a_ 前缀），
 * 注入 X-User-Id / X-User-Type 供下游服务的 LoginContext 还原。
 */
@Component
public class UserHeaderFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_HEADER = "satoken";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Type");
                });

        String token = exchange.getRequest().getHeaders().getFirst(TOKEN_HEADER);
        if (token != null && !token.isBlank()) {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId != null) {
                String s = loginId.toString();
                String type = s.startsWith("u_") ? "user" : (s.startsWith("a_") ? "admin" : null);
                if (type != null) {
                    String id = s.substring(2);
                    builder.headers(h -> {
                        h.set("X-User-Id", id);
                        h.set("X-User-Type", type);
                    });
                }
            }
        }
        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
