package com.kinn.shop.common.core;

import lombok.Getter;

/**
 * 业务异常：由 GlobalExceptionHandler 统一转为 Result。
 * custom=false（仅错误码）时响应文案按 Accept-Language i18n 解析；
 * custom=true（携带定制消息）时原样返回。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final boolean custom;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.custom = false;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.custom = true;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.custom = true;
    }

    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode);
    }
}
