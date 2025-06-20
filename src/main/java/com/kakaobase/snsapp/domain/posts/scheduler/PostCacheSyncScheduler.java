package com.kakaobase.snsapp.domain.posts.scheduler;


import com.kakaobase.snsapp.domain.posts.service.PostCacheSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 게시글 캐시데이터-DB 동기화 스케줄러
 * - 1분마다 실행
 * - Redisson 분산 락 사용
 * - 실제 동기화 로직은 PostCacheSyncService에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostCacheSyncScheduler {

    private final PostCacheSyncService postCacheSyncService;

    /**
     * 1분마다 실행되는 메인 동기화 스케줄러
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void syncPostCache() {
        postCacheSyncService.syncCacheToDB();
    }
}