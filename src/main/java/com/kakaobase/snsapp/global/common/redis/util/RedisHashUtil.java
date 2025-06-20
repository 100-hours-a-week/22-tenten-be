package com.kakaobase.snsapp.global.common.redis.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

@RequiredArgsConstructor
public abstract class RedisHashUtil<T> {

    protected final RedisTemplate<String, Object> redisTemplate;
    protected final ObjectMapper objectMapper;

    protected void putAll(String key, T value) {
        Map<String, Object> map = objectMapper.convertValue(value, new TypeReference<>() {});
        redisTemplate.opsForHash().putAll(key, map);
    }

    protected T getAll(String key, Class<T> clazz) {
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        return objectMapper.convertValue(map, clazz);
    }

    // 추상 메서드로 강제 명시
    public abstract void save(String key, T value);

    public abstract T load(String key);
}

