package com.kakaobase.snsapp.domain.auth.service;

import com.kakaobase.snsapp.domain.auth.exception.AuthException;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.auth.util.AuthRedisHashUtil;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCacheService {

    private final AuthRedisHashUtil redisUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String AUTH_INFO_PREFIX = "refresh_token:";

    /**
     * CustomUserDetails를 Redis Hash로 캐싱
     *
     * @param refreshTokenHash 해싱 전 원본 리프레시 토큰 (키로 사용)
     * @param userDetails 캐싱할 사용자 정보
     * @param expiresAt 토큰 만료시간 (서울시간)
     */
    public void createRefreshCache(String refreshTokenHash, CustomUserDetails userDetails, LocalDateTime expiresAt) {
        try {
            String key = AUTH_INFO_PREFIX + refreshTokenHash;

            // 만료 시간 검증
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(expiresAt)) {
                log.warn("이미 만료된 토큰은 캐싱하지 않음: expiresAt={}, now={}", expiresAt, now);
                throw new AuthException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
            }

            // TTL 계산
            long ttlSeconds = ChronoUnit.SECONDS.between(now, expiresAt);

            // CustomUserDetails → UserAuthCache Record 변환
            CacheRecord.UserAuthCache cache = CacheRecord.UserAuthCache.builder()
                    .memberId(Long.valueOf(userDetails.getId()))
                    .role(userDetails.getRole())
                    .className(userDetails.getClassName())
                    .nickname(userDetails.getNickname())
                    .imageUrl(userDetails.getProfileImgUrl())
                    .isEnabled(userDetails.isEnabled())
                    .build();

            // Redis Hash로 저장
            redisUtil.save(key, cache);

            // TTL 설정
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);

            log.debug("Redis Hash 캐싱 완료: memberId={}, TTL={}초 ({}분)",
                    cache.memberId(), ttlSeconds, ttlSeconds / 60);

        } catch (NoSuchElementException e) {
            log.warn("Redis 연결 풀 고갈, 캐싱 건너뜀: {}", e.getMessage());
            throw new AuthException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 연결 실패, 캐싱 건너뜀: {}", e.getMessage());
            throw new AuthException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Redis 캐싱 실패: {}", e.getMessage());
            throw new AuthException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * rawRefreshToken을 키로 Redis Hash에서 사용자 정보 조회
     *
     * @param refreshTokenHash 해싱 전 원본 리프레시 토큰
     * @return UserAuthCache 또는 null (Cache Miss/실패 시)
     */
    public CacheRecord.UserAuthCache getRefreshCache(String refreshTokenHash) {
        try {
            String key = AUTH_INFO_PREFIX + refreshTokenHash;

            // Redis Hash에서 조회
            CacheRecord.UserAuthCache cache = redisUtil.load(key);

            if (cache == null || cache.memberId() == null) {
                log.debug("Cache Miss: {}", key);
                return null;
            }

            log.debug("Cache Hit: memberId={}", cache.memberId());
            return cache;

        } catch (NoSuchElementException e) {
            log.warn("Redis 연결 풀 고갈, Cache Miss로 처리: {}", e.getMessage());
            return null;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 연결 실패, Cache Miss로 처리: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Redis 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 특정 사용자의 프로필 이미지 URL 업데이트
     *
     * @param refreshTokenHash 해싱 전 원본 리프레시 토큰
     * @param newImageUrl 새로운 프로필 이미지 URL
     * @return 업데이트 성공 여부
     */
    public boolean updateRefreshCacheImage(String refreshTokenHash, String newImageUrl) {
        try {
            String key = AUTH_INFO_PREFIX + refreshTokenHash;

            // 해당 키가 존재하는지 확인
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                log.debug("Cache Miss - 업데이트할 데이터 없음: {}", key);
                return false;
            }

            // imageUrl 필드만 업데이트
            redisTemplate.opsForHash().put(key, "imageUrl", newImageUrl);

            log.debug("프로필 이미지 업데이트 완료: key={}, newImageUrl={}", key, newImageUrl);
            return true;

        } catch (NoSuchElementException e) {
            log.warn("Redis 연결 풀 고갈로 업데이트 실패: {}", e.getMessage());
            return false;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 연결 실패로 업데이트 실패: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("프로필 이미지 업데이트 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * rawRefreshToken을 키로 Redis Hash에서 데이터 삭제
     */
    public void deleteRefreshCache(String refreshTokenHash) throws AuthException {
        try {
            String key = AUTH_INFO_PREFIX + refreshTokenHash;
            Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Redis Hash 삭제 완료: {}", key);
            } else {
                log.debug("삭제할 데이터 없음: {}", key);
            }

        } catch (NoSuchElementException e) {
            log.warn("Redis 연결 풀 고갈로 삭제 실패: {}", e.getMessage());
            throw new AuthException(GeneralErrorCode.INTERNAL_SERVER_ERROR, null, "pool 고갈");
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 연결 실패로 삭제 실패: {}", e.getMessage());
            throw new AuthException(GeneralErrorCode.INTERNAL_SERVER_ERROR, null, "Redis 연결 실패");
        } catch (Exception e) {
            log.error("Redis 삭제 실패: {}", e.getMessage());
            throw new AuthException(GeneralErrorCode.INTERNAL_SERVER_ERROR, null, "Redis값 삭제 실패");
        }
    }
}