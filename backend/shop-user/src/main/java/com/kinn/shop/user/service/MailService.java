package com.kinn.shop.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送：shop.mail.mock=true 或 JavaMailSender 不可用时，验证码打日志（dev 默认）；
 * 否则走 SMTP 真发（spring.mail.* 由 MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD 注入）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    /** ObjectProvider 延迟获取：无 spring.mail 配置时 JavaMailSender bean 可能不存在 */
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${shop.mail.mock:true}")
    private boolean mock;

    @Value("${spring.mail.username:}")
    private String from;

    public void sendEmailCode(String email, String scene, String code) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (mock || sender == null) {
            log.info("[mail-mock] scene={} email={} code={} (5 min valid)", scene, email, code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[KinnShop] Email Verification Code");
        message.setText("Your verification code is: " + code + "\nIt is valid for 5 minutes. "
                + "If you did not request this, please ignore this email.");
        sender.send(message);
        log.info("[mail] verification code sent, scene={} email={}", scene, email);
    }
}
