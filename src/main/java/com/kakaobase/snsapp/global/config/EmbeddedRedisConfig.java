package com.kakaobase.snsapp.global.config;

import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.embedded.RedisServer;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;

@Slf4j
@Component
public class EmbeddedRedisConfig {

    @Value("${redis.embedded.port:16379}")
    private int defaultEmbeddedPort;

    private RedisServer redisServer;
    private int actualPort;
    private boolean isRunning = false;

    /**
     * Embedded Redis 서버 시작 (사용 가능한 포트 자동 할당)
     */
    public void startEmbeddedRedis() {
        if (isRunning) {
            log.warn("Embedded Redis is already running on port: {}", actualPort);
            return;
        }

        try {
            // 사용 가능한 포트 찾기
            actualPort = findAvailablePort();

            // Embedded Redis 서버 생성 및 시작
            redisServer = RedisServer.builder()
                    .port(actualPort)
                    .setting("maxmemory 128M")
                    .setting("maxmemory-policy allkeys-lru")
                    .build();

            redisServer.start();
            isRunning = true;

            log.info("🚀 Embedded Redis started successfully on port: {}", actualPort);

        } catch (Exception e) {
            log.error("❌ Embedded Redis 시작 실패", e);
            throw new CustomException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Embedded Redis 서버 종료
     */
    @PreDestroy
    public void stopEmbeddedRedis() {
        if (redisServer != null && isRunning) {
            try {
                redisServer.stop();
                isRunning = false;
                log.info("⏹️ Embedded Redis stopped (was running on port: {})", actualPort);
            } catch (Exception e) {
                log.error("Error stopping Embedded Redis", e);
            }
        }
    }

    /**
     * 사용 가능한 포트 찾기
     */
    private int findAvailablePort() {
        // 1. 기본 설정 포트 확인
        if (isPortAvailable(defaultEmbeddedPort)) {
            log.info("Using default Embedded Redis port: {}", defaultEmbeddedPort);
            return defaultEmbeddedPort;
        }

        // 2. 기본 포트가 사용 중이면 시스템에서 사용 가능한 포트 찾기
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            log.info("Embedded Redis Default port인 {}가 사용중, 사용가능 포트: {}",
                    defaultEmbeddedPort, port);
            return port;
        } catch (IOException e) {
            log.error("Embedded Redis를 사용할 수 있는 port를 찾지 못함", e);
            throw new CustomException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 포트 사용 가능 여부 확인
     */
    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 현재 실행 중인 Embedded Redis 포트 반환
     */
    public int getPort() {
        if (!isRunning) {
            throw new IllegalStateException("Embedded Redis is not running");
        }
        return actualPort;
    }

    /**
     * Embedded Redis 실행 상태 확인
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * 연결 정보 반환 (디버깅용)
     */
    public String getConnectionInfo() {
        if (isRunning) {
            return String.format("localhost:%d", actualPort);
        }
        return "Not running";
    }
}