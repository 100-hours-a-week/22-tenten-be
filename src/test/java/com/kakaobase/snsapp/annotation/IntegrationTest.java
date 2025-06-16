// IntegrationTest.java
package com.kakaobase.snsapp.annotation;

import com.kakaobase.snsapp.config.TestDatabaseConfig;
import com.kakaobase.snsapp.config.TestSecurityConfig;
import com.kakaobase.snsapp.global.config.JpaConfig;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 통합 테스트용 커스텀 어노테이션
 *
 * 포함 기능:
 * - 전체 Spring Context 로딩
 * - H2 인메모리 데이터베이스 사용
 * - TestSecurityConfig 적용
 * - 트랜잭션 자동 롤백
 * - 랜덤 포트 사용
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = "com.kakaobase.snsapp.stub")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({TestDatabaseConfig.class, TestSecurityConfig.class, JpaConfig.class})
@Transactional
@ActiveProfiles("test")
@Rollback
public @interface IntegrationTest {
}