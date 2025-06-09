package com.kakaobase.snsapp.domain.comments.event;

import com.kakaobase.snsapp.domain.comments.service.BotRecommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 댓글 이벤트 리스너
 *
 * <p>댓글 관련 이벤트를 처리하여 봇 대댓글 생성 등의 후속 작업을 수행합니다.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommentEventListener {

    private final BotRecommentService botRecommentService;

    /**
     * 댓글 생성 이벤트 처리
     *
     * <p>봇이 작성한 게시글에 일반 사용자가 댓글을 달았을 때 AI 대댓글을 생성합니다.</p>
     *
     * @param event 댓글 생성 이벤트

    @EventListener
    @Async("taskExecutor")  // AsyncConfig에서 정의한 빈 이름 명시
    public void handleCommentCreated(CommentCreatedEvent event) {
        log.debug("댓글 생성 이벤트 수신: {}", event);

        try {
            // 1. 봇 게시글에 달린 댓글인지 확인
            if (!event.isCommentOnBotPost()) {
                log.debug("봇 게시글이 아니므로 대댓글 생성 스킵 - postAuthorId: {}", event.getPostAuthorId());
                return;
            }

            // 2. 봇이 작성한 댓글 제외 (무한 루프 방지)
            if (event.isCommentByBot()) {
                log.debug("봇이 작성한 댓글이므로 대댓글 생성 스킵 - commentAuthorId: {}", event.getCommentAuthorId());
                return;
            }

            log.info("봇 대댓글 생성 시작 - commentId: {}, postId: {}",
                    event.getCommentId(), event.getPostId());

            // 3. 봇 대댓글 생성 서비스 호출
            botRecommentService.createBotRecomment(event);

            log.info("봇 대댓글 생성 이벤트 처리 완료 - commentId: {}", event.getCommentId());

        } catch (Exception e) {
            // 에러가 발생해도 메인 플로우에 영향 없도록 처리
            log.error("봇 대댓글 생성 실패 - commentId: {}, error: {}",
                    event.getCommentId(), e.getMessage(), e);

            // 모니터링을 위해 추가적인 처리 필요시 여기에 구현
            // 예: 메트릭 수집, 알림 발송 등
        }
    }*/

}