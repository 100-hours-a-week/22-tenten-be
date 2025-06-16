// SecurityTest.java
package com.kakaobase.snsapp.annotation;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Security 테스트용 커스텀 어노테이션
 *
 * 포함 기능:
 * - 전체 Spring Context 로딩 (실제 Security 설정 사용)
 * - MockMvc 자동 설정
 * - 실제 Security Filter Chain 적용
 * - JWT 토큰 테스트 가능
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public @interface SecurityTest {
}