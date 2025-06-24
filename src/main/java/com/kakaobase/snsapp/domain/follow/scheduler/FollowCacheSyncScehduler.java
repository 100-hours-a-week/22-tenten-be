package com.kakaobase.snsapp.domain.follow.scheduler;

import com.kakaobase.snsapp.domain.follow.service.FollowCacheSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowCacheSyncScehduler {

    private final FollowCacheSyncService followCacheSyncService;

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void syncPostCache() {
        followCacheSyncService.syncCacheToDB();
    }
}
