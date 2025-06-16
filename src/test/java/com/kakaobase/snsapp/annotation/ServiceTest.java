// ServiceTest.java
package com.kakaobase.snsapp.annotation;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Service 계층 테스트용 커스텀 어노테이션
 *
 * 포함 기능:
 * - Mockito 확장 활성화
 * - 테스트 프로파일 활성화
 * - Spring Context 로딩 없이 순수 단위 테스트
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public @interface ServiceTest {
}