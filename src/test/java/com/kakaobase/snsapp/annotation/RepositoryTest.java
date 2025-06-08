// RepositoryTest.java
package com.kakaobase.snsapp.annotation;

import com.kakaobase.snsapp.config.TestDatabaseConfig;
import com.kakaobase.snsapp.global.config.JpaConfig;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Repository 계층 테스트용 커스텀 어노테이션
 *
 * 포함 기능:
 * - @DataJpaTest: JPA 관련 Bean만 로딩
 * - H2 인메모리 데이터베이스 사용
 * - 테스트 프로파일 활성화
 * - 트랜잭션 자동 롤백
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@ComponentScan(basePackages = "com.kakaobase.snsapp.stub")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import({TestDatabaseConfig.class, JpaConfig.class})
@ActiveProfiles("test")
@Rollback
public @interface RepositoryTest {
}