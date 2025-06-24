package com.kakaobase.snsapp.domain.comments.service.async;

import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.service.BotRecommentService;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentAsyncService {

    private final BotRecommentService botRecommentService;

    @Async
    public void triggerAsync(Post post, Comment comment) {
        try {
            log.info("🚀 [BotTrigger] 비동기 트리거 시작 - postId={}, commentId={}", post.getId(), comment.getId());
            botRecommentService.handle(post, comment);
            log.info("✅ [BotTrigger] 성공적으로 처리됨");
        } catch (Exception e) {
            log.error("❌ [BotTrigger] 실패 - reason: {}", e.getMessage(), e);
        }
    }
}
