package com.kinn.shop.common.web;

import com.kinn.shop.common.context.LoginContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 从网关注入的请求头还原登录上下文。
 * 头只信任网关：网关已剥离外部伪造的 X-User-* 头。
 */
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(LoginContext.HEADER_USER_ID);
        String userType = request.getHeader(LoginContext.HEADER_USER_TYPE);
        if (userId != null && !userId.isBlank()) {
            try {
                LoginContext.set(Long.parseLong(userId), userType);
            } catch (NumberFormatException ignore) {
                LoginContext.clear();
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginContext.clear();
    }
}
