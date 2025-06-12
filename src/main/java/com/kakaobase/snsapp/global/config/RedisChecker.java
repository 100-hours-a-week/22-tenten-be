package com.kakaobase.snsapp.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.InetSocketAddress;
import java.net.Socket;

@Slf4j
public class RedisChecker {

    private RedisChecker() {}

    /**
     * 포트 연결 가능 여부 확인
     */
    public static boolean isPortAvailable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (Exception e) {
            log.debug("Port connection failed for {}:{}", host, port, e);
            return false;
        }
    }

    /**
     * Redis 서비스 레벨 연결 확인 (포트 체크 + 실제 Redis 명령어 실행)
     */
    public static boolean checkRedisConnection(String host, int port, String password) {
        log.info("Checking Redis connection to {}:{}", host, port);

        // 1단계: 포트 연결 확인
        if (!isPortAvailable(host, port)) {
            log.warn("❌ Redis port {}:{} is not available", host, port);
            return false;
        }

        // 2단계: Redis 서비스 레벨 확인
        LettuceConnectionFactory connectionFactory = null;
        try {
            // Redis 연결 설정
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(host);
            config.setPort(port);
            if (password != null && !password.trim().isEmpty()) {
                config.setPassword(RedisPassword.of(password));
            }

            connectionFactory = new LettuceConnectionFactory(config);
            connectionFactory.afterPropertiesSet();

            // Redis Template으로 실제 명령어 실행 테스트
            RedisTemplate<String, String> testTemplate = new RedisTemplate<>();
            testTemplate.setConnectionFactory(connectionFactory);
            testTemplate.setKeySerializer(new StringRedisSerializer());
            testTemplate.setValueSerializer(new StringRedisSerializer());
            testTemplate.afterPropertiesSet();

            // PING 명령어로 Redis 서버 응답 확인
            String pingResult = connectionFactory.getConnection().ping();

            // 간단한 SET/GET 테스트
            testTemplate.opsForValue().set("redis:health:check", "ok");
            String result = testTemplate.opsForValue().get("redis:health:check");
            testTemplate.delete("redis:health:check");

            if ("PONG".equals(pingResult) && "ok".equals(result)) {
                log.info("✅ Redis server is available and responding correctly");
                return true;
            } else {
                log.warn("❌ Redis server responded but with unexpected results");
                return false;
            }

        } catch (Exception e) {
            log.warn("❌ Redis service connection failed for {}:{}", host, port, e);
            return false;
        } finally {
            if (connectionFactory != null) {
                try {
                    connectionFactory.destroy();
                } catch (Exception e) {
                    log.debug("Error closing Redis test connection", e);
                }
            }
        }
    }
}