package com.kakaobase.snsapp.domain.chat.util;

import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * StringRedisTemplate 기반 단순 채팅 버퍼 캐시 유틸리티
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatBufferCacheUtil {
    
    private static final String CACHE_PREFIX = "chatbuffer:";
    
    @Value("${chat.buffer.cache-ttl:60}")
    private long ttlSeconds;
    
    private final StringRedisTemplate stringRedisTemplate;
    
    /**
     * 사용자 ID로 캐시 키 생성
     */
    private String generateKey(Long userId) {
        return CACHE_PREFIX + userId;
    }
    
    /**
     * 메시지 추가 및 TTL 1분으로 초기화
     */
    public void appendMessage(Long userId, String message) {
        String key = generateKey(userId);
        
        try {
            // 기존 값 가져오기
            String existingValue = stringRedisTemplate.opsForValue().get(key);
            
            // 새로운 값 생성
            String newValue;
            if (existingValue == null || existingValue.isBlank()) {
                newValue = message.trim();
            } else {
                newValue = existingValue + " " + message.trim();
            }
            
            // Redis에 저장 및 TTL 설정
            stringRedisTemplate.opsForValue().set(key, newValue, ttlSeconds, TimeUnit.SECONDS);
            
            log.debug("메시지 추가 완료: userId={}, message={}, totalLength={}", 
                     userId, message, newValue.length());
            
        } catch (Exception e) {
            log.error("메시지 추가 실패: userId={}, message={}, error={}", 
                     userId, message, e.getMessage(), e);
            throw new ChatException(ChatErrorCode.CHAT_BUFFER_ADD_FAIL, userId);
        }
    }
    
    /**
     * TTL 1분으로 연장
     */
    public void extendTTL(Long userId) {
        String key = generateKey(userId);
        
        try {
            // 키가 존재하는 경우에만 TTL 연장
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
                log.debug("TTL 연장 완료: userId={}, ttl={}초", userId, ttlSeconds);
            } else {
                log.debug("연장할 캐시가 존재하지 않음: userId={}", userId);
            }
            
        } catch (Exception e) {
            log.error("TTL 연장 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 버퍼 내용 가져오기 및 삭제
     */
    public String getAndDeleteBuffer(Long userId) {
        String key = generateKey(userId);
        
        try {
            // 값 가져오기
            String value = stringRedisTemplate.opsForValue().get(key);
            
            if (value != null) {
                // 키 삭제
                stringRedisTemplate.delete(key);
                log.debug("버퍼 내용 가져오기 및 삭제 완료: userId={}, content={}", userId, value);
                return value.trim();
            } else {
                log.debug("버퍼가 존재하지 않음: userId={}", userId);
                return "";
            }
            
        } catch (Exception e) {
            log.error("버퍼 내용 가져오기 실패: userId={}, error={}", userId, e.getMessage(), e);
            return "";
        }
    }
    
    /**
     * 버퍼 내용 확인 (삭제하지 않음)
     */
    public String peekBuffer(Long userId) {
        String key = generateKey(userId);
        
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            return value != null ? value.trim() : "";
            
        } catch (Exception e) {
            log.warn("버퍼 내용 확인 실패: userId={}, error={}", userId, e.getMessage(), e);
            return "";
        }
    }
    
    /**
     * 버퍼 강제 삭제
     */
    public void deleteBuffer(Long userId) {
        String key = generateKey(userId);
        
        try {
            stringRedisTemplate.delete(key);
            log.debug("버퍼 강제 삭제 완료: userId={}", userId);
        } catch (Exception e) {
            log.error("버퍼 강제 삭제 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    
    /**
     * 캐시 키 존재 여부 확인
     */
    public boolean exists(Long userId) {
        String key = generateKey(userId);
        
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("키 존재 여부 확인 실패: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 버퍼 존재 여부 확인 (exists의 별칭)
     */
    public boolean hasBuffer(Long userId) {
        return exists(userId);
    }
}