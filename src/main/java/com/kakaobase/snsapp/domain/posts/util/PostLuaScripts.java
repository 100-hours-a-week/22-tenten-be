package com.kakaobase.snsapp.domain.posts.util;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
public class PostLuaScripts {

    // 좋아요 수 증가 스크립트
    private static final String INCREMENT_LIKE_SCRIPT =
            "local key = KEYS[1] " +
                    "local increment = tonumber(ARGV[1]) " +
                    // 키가 존재하는지 확인
                    "if redis.call('EXISTS', key) == 0 then " +
                    "   return 0 " +  // 캐시가 초기화되지 않음
                    "end " +
                    // 좋아요 수 증가
                    "redis.call('HINCRBY', key, 'likeCount', increment) " +
                    "return 1";

    // 좋아요 수 감소 스크립트
    private static final String DECREMENT_LIKE_SCRIPT =
            "local key = KEYS[1] " +
                    "local decrement = tonumber(ARGV[1]) " +
                    // 키가 존재하는지 확인
                    "if redis.call('EXISTS', key) == 0 then " +
                    "   return 0 " +  // 캐시가 초기화되지 않음
                    "end " +
                    // 현재 좋아요 수 조회
                    "local currentLikeCount = tonumber(redis.call('HGET', key, 'likeCount') or 0) " +
                    // 좋아요 수 감소 (음수 방지)
                    "local newLikeCount = math.max(0, currentLikeCount - decrement) " +
                    // 새 값 설정
                    "redis.call('HSET', key, 'likeCount', newLikeCount) " +
                    "return 1";

    // 댓글 수 증가 스크립트
    private static final String INCREMENT_COMMENT_SCRIPT =
            "local key = KEYS[1] " +
                    "local increment = tonumber(ARGV[1]) " +
                    // 키가 존재하는지 확인
                    "if redis.call('EXISTS', key) == 0 then " +
                    "   return 0 " +  // 캐시가 초기화되지 않음
                    "end " +
                    // 댓글 수 증가
                    "redis.call('HINCRBY', key, 'commentCount', increment) " +
                    "return 1";

    // 댓글 수 감소 스크립트
    private static final String DECREMENT_COMMENT_SCRIPT =
            "local key = KEYS[1] " +
                    "local decrement = tonumber(ARGV[1]) " +
                    // 키가 존재하는지 확인
                    "if redis.call('EXISTS', key) == 0 then " +
                    "   return 0 " +  // 캐시가 초기화되지 않음
                    "end " +
                    // 현재 댓글 수 조회
                    "local currentCommentCount = tonumber(redis.call('HGET', key, 'commentCount') or 0) " +
                    // 댓글 수 감소 (음수 방지)
                    "local newCommentCount = math.max(0, currentCommentCount - decrement) " +
                    // 새 값 설정
                    "redis.call('HSET', key, 'commentCount', newCommentCount) " +
                    "return 1";

    // RedisScript 객체들 (SHA 캐싱용)
    private final RedisScript<Long> incrementLikeScript;
    private final RedisScript<Long> decrementLikeScript;
    private final RedisScript<Long> incrementCommentScript;
    private final RedisScript<Long> decrementCommentScript;

    public PostLuaScripts() {
        this.incrementLikeScript = new DefaultRedisScript<>(INCREMENT_LIKE_SCRIPT, Long.class);
        this.decrementLikeScript = new DefaultRedisScript<>(DECREMENT_LIKE_SCRIPT, Long.class);
        this.incrementCommentScript = new DefaultRedisScript<>(INCREMENT_COMMENT_SCRIPT, Long.class);
        this.decrementCommentScript = new DefaultRedisScript<>(DECREMENT_COMMENT_SCRIPT, Long.class);
    }

    /**
     * 좋아요 수 증가 스크립트
     */
    public RedisScript<Long> getIncrementLikeScript() {
        return incrementLikeScript;
    }

    /**
     * 좋아요 수 감소 스크립트
     */
    public RedisScript<Long> getDecrementLikeScript() {
        return decrementLikeScript;
    }

    /**
     * 댓글 수 증가 스크립트
     */
    public RedisScript<Long> getIncrementCommentScript() {
        return incrementCommentScript;
    }

    /**
     * 댓글 수 감소 스크립트
     */
    public RedisScript<Long> getDecrementCommentScript() {
        return decrementCommentScript;
    }
}