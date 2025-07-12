package com.kakaobase.snsapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Chat 도메인 테스트 전용 설정 클래스
 *
 * 제공 기능:
 * - 시간 제어를 위한 Clock 설정
 * - Chat 도메인 테스트에 필요한 유틸리티 Bean 설정
 *
 * 사용법:
 * - @Import(ChatTestConfig.class)를 테스트 클래스에 추가 (단위 테스트용)
 * - Redis 관련 설정은 TestRedisConfig.java 참조 (통합 테스트용)
 *
 * 참고:
 * - Redis 설정은 TestRedisConfig로 분리됨
 * - 단위 테스트에서는 Mock 사용, 통합 테스트에서는 TestRedisConfig 사용
 */
@TestConfiguration
@Profile("test")
public class ChatTestConfig {
    
    /**
     * 테스트용 Clock 설정
     * - 시간 제어 가능한 고정 Clock
     * - 테스트 시간 일관성 보장
     */
    @Bean
    @Primary
    public Clock testClock() {
        // 고정 시간으로 테스트 일관성 보장
        // 2024-01-01 12:00:00 UTC
        Instant fixedInstant = Instant.parse("2024-01-01T12:00:00Z");
        return Clock.fixed(fixedInstant, ZoneId.systemDefault());
    }
    
    /**
     * 테스트용 시간 제어 유틸리티
     * - 테스트에서 시간 조작 가능
     */
    @Bean
    public TestTimeController testTimeController() {
        return new TestTimeController();
    }
    
    /**
     * 테스트용 시간 제어 클래스
     */
    public static class TestTimeController {
        private Clock currentClock;
        
        public TestTimeController() {
            // 기본 고정 시간 설정
            this.currentClock = Clock.fixed(
                Instant.parse("2024-01-01T12:00:00Z"), 
                ZoneId.systemDefault()
            );
        }
        
        /**
         * 특정 시간으로 Clock 설정
         */
        public void setTime(Instant instant) {
            this.currentClock = Clock.fixed(instant, ZoneId.systemDefault());
        }
        
        /**
         * 현재 시간에서 지정된 시간만큼 앞으로 이동
         */
        public void advanceTime(java.time.Duration duration) {
            Instant newInstant = currentClock.instant().plus(duration);
            this.currentClock = Clock.fixed(newInstant, ZoneId.systemDefault());
        }
        
        /**
         * 현재 Clock 반환
         */
        public Clock getClock() {
            return currentClock;
        }
        
        /**
         * 현재 시간 반환
         */
        public Instant now() {
            return currentClock.instant();
        }
        
        /**
         * 기본 시간으로 리셋
         */
        public void reset() {
            this.currentClock = Clock.fixed(
                Instant.parse("2024-01-01T12:00:00Z"), 
                ZoneId.systemDefault()
            );
        }
    }
}