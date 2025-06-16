package com.kakaobase.snsapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 테스트 환경용 외부 서비스 설정 클래스
 *
 * 실제 외부 서비스 호출을 방지하고 테스트용 구현체 제공:
 * - 메일 발송 서비스
 * - AWS S3 서비스 (필요 시)
 * - Redis 서비스 (필요 시)
 */
@TestConfiguration
@Profile("test")
public class TestPropertiesConfig {

    /**
     * 테스트용 JavaMailSender 설정
     *
     * - 실제 메일 발송하지 않음
     * - 메일 관련 로직 테스트 시 사용
     */
    @Bean
    @Primary
    @Profile("test")
    public JavaMailSender testMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(25);
        // 실제 메일 발송하지 않도록 설정
        return mailSender;
    }
}