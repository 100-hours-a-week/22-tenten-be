package com.kakaobase.snsapp.domain.posts.event;

import com.kakaobase.snsapp.domain.posts.service.BotPostService;
import com.kakaobase.snsapp.domain.posts.util.BoardType;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 게시글 생성 이벤트 리스너
 *
 * <p>모든 게시글(봇 포함) 생성 이벤트를 처리하여 5개마다 봇 게시글을 생성합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventListener {

    private final PostCounter postCounter;
    private final BotPostService botPostService;

    /**
     * 게시글 생성 이벤트 처리
     *
     * <p>모든 게시글(봇 포함)이 생성될 때마다 카운터를 증가시키고,
     * 5개가 되면 봇 게시글을 생성합니다.</p>
     *
     * @param event 게시글 생성 이벤트
     */
    @EventListener
    @Async
    public void handlePostCreated(PostCreatedEvent event) {
        String threadName = Thread.currentThread().getName();
        log.info("게시글 생성 이벤트 처리 시작 - postId: {}, boardType: {}, memberId: {}, thread: {}",
                event.getPostId(), event.getBoardType(), event.getMemberId(), threadName);

        try {
            BoardType boardType = event.getBoardType();

            // 모든 게시글에 대해 카운터 증가
            int currentCount = postCounter.increment(boardType);
            log.info("게시글 카운터 증가 - boardType: {}, currentCount: {}, thread: {}", 
                    boardType, currentCount, threadName);

            // 카운터가 5에 도달한 경우
            if (currentCount >= BotConstants.POST_COUNT_THRESHOLD) {
                log.info("게시글 5개 도달 - 봇 게시글 생성 시작. boardType: {}, thread: {}", 
                        boardType, threadName);

                // 카운터 먼저 리셋 (봇 게시글 생성 실패해도 리셋은 보장)
                postCounter.reset(boardType);
                log.info("카운터 리셋 완료 - boardType: {}, thread: {}", boardType, threadName);

                // 봇 게시글 생성을 별도 스레드에서 비동기 실행 (타임아웃 방지)
                CompletableFuture.runAsync(() -> {
                    try {
                        log.info("봇 게시글 생성 시작 - boardType: {}, asyncThread: {}", 
                                boardType, Thread.currentThread().getName());
                        botPostService.createBotPost(boardType);
                        log.info("봇 게시글 생성 완료 - boardType: {}, asyncThread: {}", 
                                boardType, Thread.currentThread().getName());
                    } catch (Exception e) {
                        log.error("봇 게시글 생성 실패 - boardType: {}, asyncThread: {}", 
                                boardType, Thread.currentThread().getName(), e);
                    }
                }).exceptionally(throwable -> {
                    log.error("봇 게시글 생성 CompletableFuture 실패 - boardType: {}", boardType, throwable);
                    return null;
                });
            }

        } catch (Exception e) {
            log.error("게시글 생성 이벤트 처리 중 오류 발생 - thread: {}", threadName, e);
            // 이벤트 처리 실패가 게시글 생성에 영향을 주지 않도록 예외 처리
        }
        
        log.info("게시글 생성 이벤트 처리 완료 - postId: {}, thread: {}", 
                event.getPostId(), threadName);
    }
}