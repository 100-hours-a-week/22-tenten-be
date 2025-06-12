package com.kakaobase.snsapp.global.config;

import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String externalRedisHost;

    @Value("${spring.data.redis.port:6379}")
    private int externalRedisPort;

    @Value("${spring.data.redis.password:}")
    private String externalRedisPassword;

    @Value("${redis.fallback.enabled:true}")
    private boolean fallbackEnabled;

    private final EmbeddedRedisConfig embeddedRedisConfig;
    private boolean isUsingEmbeddedRedis = false;

    public RedisConfig(EmbeddedRedisConfig embeddedRedisConfig) {
        this.embeddedRedisConfig = embeddedRedisConfig;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("🔍 Redis 연결 초기화 중...");

        // 1. 외부 Redis 연결 시도
        if (RedisChecker.checkRedisConnection(externalRedisHost, externalRedisPort, externalRedisPassword)) {
            log.info("🔗 외부 Redis 서버 사용: {}:{}", externalRedisHost, externalRedisPort);
            isUsingEmbeddedRedis = false;
            return createExternalRedisConnectionFactory();
        }

        // 2. Fallback 설정 확인
        if (!fallbackEnabled) {
            log.error("❌ 외부 Redis 연결 실패 및 Fallback 비활성화");
            throw new CustomException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 3. Embedded Redis로 fallback
        log.warn("🔄 외부 Redis 실패, 내장형 Redis로 전환");
        try {
            embeddedRedisConfig.startEmbeddedRedis();
            isUsingEmbeddedRedis = true;
            return createEmbeddedRedisConnectionFactory();
        } catch (Exception e) {
            log.error("❌ 내장형 Redis Fallback 시작 실패", e);
            throw new CustomException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 외부 Redis 연결 팩토리 생성
     */
    private RedisConnectionFactory createExternalRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(externalRedisHost);
        config.setPort(externalRedisPort);

        if (externalRedisPassword != null && !externalRedisPassword.trim().isEmpty()) {
            config.setPassword(RedisPassword.of(externalRedisPassword));
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Embedded Redis 연결 팩토리 생성
     */
    private RedisConnectionFactory createEmbeddedRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(embeddedRedisConfig.getPort());
        // Embedded Redis는 패스워드 없음

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean(name = "stringRedisTemplate")
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate(factory);
        logRedisConnectionInfo();
        return template;
    }

    @Bean(name = "jsonRedisTemplate")
    public RedisTemplate<String, Object> jsonRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * Redis 연결 정보 상세 로깅
     */
    private void logRedisConnectionInfo() {
        log.info("📋 ============= Redis 설정 정보 =============");

        if (isUsingEmbeddedRedis) {
            log.info("  📍 Redis 유형: Embedded Redis");
            log.info("  🌐 호스트: localhost");
            log.info("  🔌 포트: {}", embeddedRedisConfig.getPort());
            log.info("  🔐 인증: 비활성화");
            log.info("  ⚠️  데이터 지속성: 없음 (재시작 시 데이터 손실)");
            log.info("  💡 사용 목적: 개발/테스트 전용");
        } else {
            log.info("  📍 Redis 유형: 외부 Redis 서버");
            log.info("  🌐 호스트: {}", externalRedisHost);
            log.info("  🔌 포트: {}", externalRedisPort);
            log.info("  🔐 인증: {}",
                    (externalRedisPassword != null && !externalRedisPassword.trim().isEmpty())
                            ? "활성화" : "비활성화");
        }

        log.info("  🔄 Fallback 활성화: {}", fallbackEnabled);
        log.info("===============================================");
    }
}