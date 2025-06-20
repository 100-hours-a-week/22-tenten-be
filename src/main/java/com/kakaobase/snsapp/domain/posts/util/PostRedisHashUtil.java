package com.kakaobase.snsapp.domain.posts.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.util.RedisHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PostRedisHashUtil extends RedisHashUtil<CacheRecord.PostStatsCache> {

    public PostRedisHashUtil(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        super(redisTemplate, objectMapper);
    }

    @Override
    public void save(String key, CacheRecord.PostStatsCache value) {
        putAll(key, value);
    }

    @Override
    public CacheRecord.PostStatsCache load(String key) {
        return getAll(key, CacheRecord.PostStatsCache.class);
    }

    /**
     * Pipeline으로 가져온 Hash 데이터가 유효한지 확인
     */
    public boolean isValidHashData(Object rawValue) {
        if (rawValue == null) {
            return false;
        }

        // Redis Hash 데이터는 보통 Map<String, String> 형태로 반환됨
        if (rawValue instanceof Map<?, ?> hashMap) {
            return !hashMap.isEmpty();
        }

        return false;
    }

    /**
     * Pipeline 결과에서 직접 PostStatsCache 파싱
     */
    public CacheRecord.PostStatsCache parseHashData(Object rawValue, Long postId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> hashData = (Map<String, Object>) rawValue;

            Long parsedPostId = (Long) hashData.get("postId");
            Long likeCount = (Long) hashData.get("likeCount");
            Long commentCount = (Long) hashData.get("commentCount");

            // 데이터 검증
            if (!parsedPostId.equals(postId)) {
                log.warn("PostId 불일치: expected={}, actual={}", postId, parsedPostId);
                return null;
            }

            if (parsedPostId == null || likeCount == null || commentCount == null) {
                log.warn("필수 필드가 null: postId={}, likeCount={}, commentCount={}",
                        parsedPostId, likeCount, commentCount);
                return null;
            }

            return CacheRecord.PostStatsCache.builder()
                    .postId(parsedPostId)
                    .likeCount(likeCount)
                    .commentCount(commentCount)
                    .build();

        } catch (ClassCastException e) {
            log.error("타입 캐스팅 실패: postId={}, rawValue={}", postId, rawValue, e);
            return null;
        } catch (Exception e) {
            log.error("캐시 데이터 파싱 실패: postId={}", postId, e);
            return null;
        }
    }
}