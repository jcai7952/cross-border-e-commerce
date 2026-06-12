package com.kinn.shop.gateway.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关统一鉴权（Sa-Token 与 shop-user 共享 Redis 会话）。
 * loginId 约定：买家 u_{userId}，管理员 a_{adminId}。
 */
@Configuration
public class SaTokenConfig {

    /** 无需登录即可访问的路径。 */
    private static final String[] WHITELIST = {
            "/api/auth/**",
            "/api/admin/auth/login",
            "/api/category/**",
            "/api/product/**",
            "/api/currency/list",
            "/api/flash-sale/**",
            "/api/review/list",
            "/api/pay/notify/**",
            "/favicon.ico"
    };

    @Bean
    public SaReactorFilter saReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                .setAuth(obj -> {
                    // 管理端：除登录外全部要求 a_ 身份
                    SaRouter.match("/api/admin/**")
                            .notMatch("/api/admin/auth/login")
                            .check(r -> {
                                StpUtil.checkLogin();
                                if (!StpUtil.getLoginIdAsString().startsWith("a_")) {
                                    throw NotLoginException.newInstance(StpUtil.getLoginType(),
                                            NotLoginException.INVALID_TOKEN, "非管理员身份", null);
                                }
                            });
                    // 买家端受保护路径：要求 u_ 身份
                    SaRouter.match(
                                    "/api/user/**", "/api/address/**", "/api/identity/**",
                                    "/api/favorite/**", "/api/cart/**", "/api/order/**",
                                    "/api/checkout/**", "/api/coupon/**", "/api/logistics/**")
                            .check(r -> {
                                StpUtil.checkLogin();
                                if (!StpUtil.getLoginIdAsString().startsWith("u_")) {
                                    throw NotLoginException.newInstance(StpUtil.getLoginType(),
                                            NotLoginException.INVALID_TOKEN, "非买家身份", null);
                                }
                            });
                    // 支付：回调走白名单，其余需买家登录
                    SaRouter.match("/api/pay/**")
                            .notMatch("/api/pay/notify/**")
                            .check(r -> StpUtil.checkLogin());
                })
                .setExcludeList(java.util.List.of(WHITELIST))
                .setError(e -> "{\"code\":401,\"message\":\"Please sign in first\",\"data\":null,\"success\":false}");
    }
}
