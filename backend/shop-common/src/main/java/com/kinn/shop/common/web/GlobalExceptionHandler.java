package com.kinn.shop.common.web;

import com.kinn.shop.common.core.BizException;
import com.kinn.shop.common.core.ErrorCode;
import com.kinn.shop.common.core.Result;
import com.kinn.shop.common.i18n.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：各服务 scanBasePackages 包含 com.kinn.shop 即生效。
 * 错误文案按 Accept-Language i18n（资源 i18n/messages*.properties）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("[biz] {} - {}", e.getCode(), e.getMessage());
        String msg = e.isCustom() ? e.getMessage() : MessageUtils.errorMessage(e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), msg);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null
                ? MessageUtils.errorMessage(ErrorCode.PARAM_ERROR.getCode(), ErrorCode.PARAM_ERROR.getMessage())
                : fe.getField() + " " + fe.getDefaultMessage();
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArg(IllegalArgumentException e) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.fail(ErrorCode.NOT_FOUND.getCode(),
                MessageUtils.errorMessage(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("[system] unexpected error", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(),
                MessageUtils.errorMessage(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage()));
    }
}
