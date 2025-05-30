package com.kakaobase.snsapp.global.common.email.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * 이메일 발송을 담당하는 클래스입니다.
 * Spring Mail을 사용하여 SMTP를 통해 이메일을 전송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public void sendVerificationEmail(String recipientEmail, String verificationCode) {
        try {
            // 제목 고정 (필요 시 외부로 빼도 됨)
            String subject = "이메일 인증 코드입니다";

            // 템플릿에 전달할 값 설정
            Context context = new Context();
            context.setVariable("verificationCode", verificationCode);
            log.info("verificationCode: ", verificationCode);

            // 템플릿 처리 (resources/templates/verification-email.html)
            String htmlContent = templateEngine.process("verification-email", context);

            // 메일 생성 및 설정
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // 메일 전송
            javaMailSender.send(message);
            log.info("Email sent successfully to: {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", recipientEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String getSubjectByPurpose(String purpose) {
        return switch (purpose) {
            case "sign-up" -> "[KakaoBase] 회원가입 인증 코드";
            case "password-reset" -> "[KakaoBase] 비밀번호 재설정 인증 코드";
            default -> "[KakaoBase] 인증 코드";
        };
    }

    private String createEmailContent(String code, String purpose) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("purpose", purpose);
        return templateEngine.process("verification-email", context);
    }
}