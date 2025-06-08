// BaseTestConfig.java - 핵심 테스트 설정을 통합하는 기본 설정
package com.kakaobase.snsapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 기본 테스트 설정을 통합하는 설정 클래스
 *
 * 모든 테스트에서 공통으로 필요한 설정만 포함:
 * - Security 설정 (인증 우회)
 * - Database 설정 (H2 인메모리)
 *
 * 도메인별 특화 설정은 필요한 테스트에서 개별적으로 Import
 */
@TestConfiguration
@Import({
        TestSecurityConfig.class,
        TestDatabaseConfig.class
})
public class BaseTestConfig {
}