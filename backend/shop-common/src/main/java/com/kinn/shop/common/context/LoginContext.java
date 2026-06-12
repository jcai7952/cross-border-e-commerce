package com.kinn.shop.common.context;

import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;

/**
 * 登录上下文：网关鉴权后通过 X-User-Id / X-User-Type 头透传，
 * 由 UserContextInterceptor 写入 ThreadLocal。
 */
public final class LoginContext {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_TYPE = "X-User-Type";
    public static final String TYPE_USER = "user";
    public static final String TYPE_ADMIN = "admin";

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_TYPE = new ThreadLocal<>();

    private LoginContext() {
    }

    public static void set(Long userId, String userType) {
        USER_ID.set(userId);
        USER_TYPE.set(userType);
    }

    public static void clear() {
        USER_ID.remove();
        USER_TYPE.remove();
    }

    /** 已登录用户 id，未登录抛 401。 */
    public static long requireUserId() {
        Long id = USER_ID.get();
        if (id == null || !TYPE_USER.equals(USER_TYPE.get())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return id;
    }

    /** 已登录管理员 id，未登录抛 401。 */
    public static long requireAdminId() {
        Long id = USER_ID.get();
        if (id == null || !TYPE_ADMIN.equals(USER_TYPE.get())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return id;
    }

    /** 未登录返回 null（游客可访问的接口用）。 */
    public static Long userIdOrNull() {
        return TYPE_USER.equals(USER_TYPE.get()) ? USER_ID.get() : null;
    }
}
