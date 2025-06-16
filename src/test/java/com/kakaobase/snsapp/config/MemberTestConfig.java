package com.kakaobase.snsapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Member 도메인 테스트용 설정 클래스
 *
 * Member 관련 테스트에서만 필요한 설정:
 * - 기본 테스트 설정
 * - 이메일 관련 설정
 */
@TestConfiguration
@Import({
        BaseTestConfig.class,
        TestPropertiesConfig.class
})
public class MemberTestConfig {
}