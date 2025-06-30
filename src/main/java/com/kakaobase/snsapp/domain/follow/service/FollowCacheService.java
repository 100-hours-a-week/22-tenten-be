package com.kakaobase.snsapp.domain.follow.service;

import com.kakaobase.snsapp.domain.follow.util.FollowCacheUtil;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.error.CacheException;
import com.kakaobase.snsapp.global.common.redis.service.cacheService.AbstractCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class FollowCacheService extends AbstractCacheService<CacheRecord.FollowStatsCache, Member> {
    private static final String FOLLOW_CACHE_PREFIX = "follow:stats:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private final MemberRepository memberRepository;

    public FollowCacheService(RedisTemplate<String, Object> redisTemplate,
                              FollowCacheUtil cacheUtil,
                              FollowCacheSyncService cacheSyncService,
                              MemberRepository memberRepository) {
        super(redisTemplate, cacheSyncService, cacheUtil);
        this.memberRepository = memberRepository;
    }

    public void incrementFollowerCount(Long memberId) throws CacheException {
        incrementField(memberId, "followerCount");
    }

    public void decrementFollowerCount(Long memberId) throws CacheException {
        decrementField(memberId, "followerCount");
    }

    public void incrementFollowingCount(Long memberId) throws CacheException {
        incrementField(memberId, "followingCount");
    }

    public void decrementFollowingCount(Long memberId) throws CacheException {
        decrementField(memberId, "followingCount");
    }

    @Override
    protected String generateCacheKey(Long id) {
        return FOLLOW_CACHE_PREFIX + id;
    }

    @Override
    protected Long extractId(Member member) {
        return member.getId();
    }

    @Override
    protected void saveFromDB(Long id) {
        Member member = memberRepository.findById(id).orElse(null);
        if(member == null) {
            return;
        }

        var cacheData = CacheRecord.FollowStatsCache.builder()
                .memberId(member.getId())
                .followerCount(member.getFollowerCount())
                .followingCount(member.getFollowingCount())
                .build();

        cacheUtil.save(generateCacheKey(id), cacheData);
    }

    @Override
    protected void saveByEntity(Long id, Member member) {
        var cacheData = CacheRecord.FollowStatsCache.builder()
                .memberId(member.getId())
                .followerCount(member.getFollowerCount())
                .followingCount(member.getFollowingCount())
                .build();

        cacheUtil.save(generateCacheKey(id), cacheData);
    }

    @Override
    protected Duration getTTL() {
        return CACHE_TTL;
    }
}
