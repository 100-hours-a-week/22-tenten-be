package com.kakaobase.snsapp.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 채팅 타이핑 스케줄러 설정
 * 
 * <p>타이핑 세션의 1초 타이머 관리를 위한 스케줄러를 설정합니다.</p>
 */
@Configuration
public class ChatTypingSchedulerConfig {

    /**
     * 타이핑 세션 전용 스케줄러
     * 
     * @return ScheduledExecutorService 인스턴스
     */
    @Bean("typingScheduler")
    public ScheduledExecutorService typingScheduler() {
        return Executors.newScheduledThreadPool(
            10, // 동시 처리 가능한 타이머 수
            r -> {
                Thread t = new Thread(r, "typing-scheduler-");
                t.setDaemon(true); // 애플리케이션 종료 시 함께 종료
                return t;
            }
        );
    }
}