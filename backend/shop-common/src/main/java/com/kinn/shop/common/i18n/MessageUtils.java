package com.kinn.shop.common.i18n;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * i18n 消息工具：按请求 Accept-Language 解析（Spring MVC 自动填充 LocaleContextHolder）。
 * 资源文件 i18n/messages*.properties，key 约定 error.{code}。
 */
@Component
public class MessageUtils implements ApplicationContextAware {

    private static MessageSource messageSource;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext ctx) throws BeansException {
        messageSource = ctx;
    }

    /** 取 error.{code} 文案，缺失则回退 defaultMessage。 */
    public static String errorMessage(int code, String defaultMessage) {
        if (messageSource == null) {
            return defaultMessage;
        }
        return messageSource.getMessage("error." + code, null, defaultMessage, LocaleContextHolder.getLocale());
    }
}
