package com.kakaobase.snsapp.domain.comments.scheduler;


import com.kakaobase.snsapp.domain.comments.service.cache.CommentCacheSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 댓글 캐시데이터-DB 동기화 스케줄러
 * - 1분마다 실행
 * - Redisson 분산 락 사용
 * - 실제 동기화 로직은 PostCacheSyncService에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentCacheSyncScheduler {

    private final CommentCacheSyncService commentCacheSyncService;

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void syncPostCache() {
        commentCacheSyncService.syncCacheToDB();
    }

}
