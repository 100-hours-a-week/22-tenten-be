package com.kakaobase.snsapp.global.config;

import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

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
     * 외부 Redis 연결 팩토리 생성 (Connection Pool 포함)
     */
    private RedisConnectionFactory createExternalRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(externalRedisHost);
        config.setPort(externalRedisPort);

        if (externalRedisPassword != null && !externalRedisPassword.trim().isEmpty()) {
            config.setPassword(RedisPassword.of(externalRedisPassword));
        }

        // Connection Pool 설정
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);        // 최대 연결 수
        poolConfig.setMaxIdle(10);         // 최대 유휴 연결 수
        poolConfig.setMinIdle(5);          // 최소 유휴 연결 수
        poolConfig.setMaxWait(Duration.ofSeconds(1)); // 연결 대기 시간 1초

        LettucePoolingClientConfiguration clientConfig =
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(poolConfig)
                        .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Embedded Redis 연결 팩토리 생성 (Connection Pool 포함)
     */
    private RedisConnectionFactory createEmbeddedRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(embeddedRedisConfig.getPort());
        // Embedded Redis는 패스워드 없음

        // Embedded Redis도 Connection Pool 적용 (Spring Boot 3.4.5 + Java 21 호환)
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(10);        // 개발환경이므로 더 적게
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(2);
        poolConfig.setMaxWait(Duration.ofSeconds(2)); // 2초 - 새로운 방식

        LettucePoolingClientConfiguration clientConfig =
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(poolConfig)
                        .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
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
            log.info("  🏊 Connection Pool: MaxTotal=10, MaxIdle=5, MinIdle=2");
            log.info("  ⚠️  데이터 지속성: 없음 (재시작 시 데이터 손실)");
            log.info("  💡 사용 목적: 개발/테스트 전용");
        } else {
            log.info("  📍 Redis 유형: 외부 Redis 서버");
            log.info("  🌐 호스트: {}", externalRedisHost);
            log.info("  🔌 포트: {}", externalRedisPort);
            log.info("  🔐 인증: {}",
                    (externalRedisPassword != null && !externalRedisPassword.trim().isEmpty())
                            ? "활성화" : "비활성화");
            log.info("  🏊 Connection Pool: MaxTotal=20, MaxIdle=10, MinIdle=5");
        }

        log.info("  🔄 Fallback 활성화: {}", fallbackEnabled);
        log.info("===============================================");
    }
}