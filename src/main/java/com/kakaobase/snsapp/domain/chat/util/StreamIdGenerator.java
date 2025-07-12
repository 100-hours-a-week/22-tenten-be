package com.kakaobase.snsapp.domain.chat.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * StreamId 생성 유틸리티
 * NanoTime + Random 조합으로 빠르고 유니크한 ID 생성
 */
@Slf4j
@Component
public class StreamIdGenerator {
    
    /**
     * NanoTime + ThreadLocalRandom 기반 StreamId 생성
     * 
     * @return 유니크한 StreamId (예: "12345678901234-a1b2c3d4")
     */
    public String generate() {
        long time = System.nanoTime();
        long rand = ThreadLocalRandom.current().nextLong();
        String streamId = time + "-" + Long.toHexString(rand);
        
        log.debug("새로운 StreamId 생성: {}", streamId);
        return streamId;
    }
    
    /**
     * StreamId 유효성 검사
     * 
     * @param streamId 검사할 StreamId
     * @return 유효한 형식이면 true
     */
    public boolean isValid(String streamId) {
        if (streamId == null || streamId.trim().isEmpty()) {
            return false;
        }
        
        try {
            String[] parts = streamId.split("-");
            if (parts.length != 2) {
                return false;
            }
            
            // nanoTime 부분 검증 (숫자)
            Long.parseLong(parts[0]);
            
            // random 부분 검증 (16진수)
            Long.parseLong(parts[1], 16);
            
            return true;
        } catch (NumberFormatException e) {
            log.warn("잘못된 StreamId 형식: {}", streamId);
            return false;
        }
    }
}