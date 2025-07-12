package com.kakaobase.snsapp.config;

import com.kakaobase.snsapp.global.config.EmbeddedRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import jakarta.annotation.PreDestroy;

/**
 * 테스트 전용 Redis 설정
 * 
 * 특징:
 * - Embedded Redis 자동 시작/종료
 * - 포트 충돌 방지 (자동 포트 할당)
 * - 테스트 격리 보장
 * - Spring Context 라이프사이클과 연동
 * 
 * 사용법:
 * - 통합 테스트: @SpringBootTest + @ActiveProfiles("test")
 * - 단위 테스트에서는 사용하지 않음 (Mock 활용)
 */
@Slf4j
@TestConfiguration
@Profile("test")
public class TestRedisConfig {
    
    private EmbeddedRedisConfig embeddedRedisConfig;
    private boolean redisStarted = false;
    
    /**
     * 테스트용 Embedded Redis 설정 생성
     */
    @Bean
    public EmbeddedRedisConfig testEmbeddedRedisConfig() {
        if (embeddedRedisConfig == null) {
            embeddedRedisConfig = new EmbeddedRedisConfig();
        }
        return embeddedRedisConfig;
    }
    
    /**
     * 테스트용 Redis 연결 팩토리
     * - Embedded Redis 사용
     * - 자동 포트 할당으로 충돌 방지
     */
    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory() {
        // Embedded Redis가 시작되지 않았다면 시작
        if (!redisStarted) {
            startEmbeddedRedisForTest();
        }
        
        // Embedded Redis 연결 설정
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(embeddedRedisConfig.getPort());
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        
        log.info("✅ 테스트용 Redis 연결 팩토리 생성 완료: localhost:{}", embeddedRedisConfig.getPort());
        return factory;
    }
    
    /**
     * 테스트용 StringRedisTemplate
     */
    @Bean
    @Primary
    public StringRedisTemplate testStringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate(redisConnectionFactory);
        log.info("✅ 테스트용 StringRedisTemplate 생성 완료");
        return template;
    }
    
    /**
     * 테스트용 RedisTemplate (JSON 직렬화)
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> testRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // Key 직렬화 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value 직렬화 설정
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        log.info("✅ 테스트용 RedisTemplate 생성 완료");
        return template;
    }
    
    /**
     * Spring Context 시작 시 Embedded Redis 시작
     */
    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        if (!redisStarted) {
            startEmbeddedRedisForTest();
        }
    }
    
    /**
     * Spring Context 종료 시 Embedded Redis 종료
     */
    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        stopEmbeddedRedisForTest();
    }
    
    /**
     * @PreDestroy로 안전한 종료 보장
     */
    @PreDestroy
    public void cleanup() {
        stopEmbeddedRedisForTest();
    }
    
    /**
     * 테스트용 Embedded Redis 시작
     */
    private synchronized void startEmbeddedRedisForTest() {
        if (redisStarted) {
            return;
        }
        
        try {
            if (embeddedRedisConfig == null) {
                embeddedRedisConfig = new EmbeddedRedisConfig();
            }
            
            embeddedRedisConfig.startEmbeddedRedis();
            redisStarted = true;
            
            log.info("🚀 테스트용 Embedded Redis 시작 완료: localhost:{}", embeddedRedisConfig.getPort());
            
        } catch (Exception e) {
            log.error("❌ 테스트용 Embedded Redis 시작 실패", e);
            throw new RuntimeException("Embedded Redis 시작 실패", e);
        }
    }
    
    /**
     * 테스트용 Embedded Redis 종료
     */
    private synchronized void stopEmbeddedRedisForTest() {
        if (!redisStarted || embeddedRedisConfig == null) {
            return;
        }
        
        try {
            embeddedRedisConfig.stopEmbeddedRedis();
            redisStarted = false;
            log.info("⏹️ 테스트용 Embedded Redis 종료 완료");
            
        } catch (Exception e) {
            log.error("❌ 테스트용 Embedded Redis 종료 중 오류", e);
        }
    }
    
    /**
     * 현재 Redis 상태 확인 (디버깅용)
     */
    public boolean isRedisRunning() {
        return redisStarted && embeddedRedisConfig != null && embeddedRedisConfig.isRunning();
    }
    
    /**
     * 현재 Redis 포트 반환 (디버깅용)
     */
    public int getCurrentRedisPort() {
        if (embeddedRedisConfig != null && redisStarted) {
            return embeddedRedisConfig.getPort();
        }
        return -1;
    }
}