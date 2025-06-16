// ControllerTest.java
package com.kakaobase.snsapp.annotation;

import com.kakaobase.snsapp.config.TestSecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 계층 테스트용 커스텀 어노테이션
 *
 * 포함 기능:
 * - @WebMvcTest: Web 계층만 로딩
 * - TestSecurityConfig 적용으로 인증 우회
 * - MockMvc 자동 설정
 * - 테스트 프로파일 활성화
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public @interface ControllerTest {
    /**
     * 테스트할 Controller 클래스들 지정
     * 빈 배열인 경우 모든 Controller 로딩
     */
    Class<?>[] controllers() default {};
}