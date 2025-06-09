package com.kakaobase.snsapp.domain.comments.service;

import com.kakaobase.snsapp.domain.comments.converter.BotRecommentConverter;
import com.kakaobase.snsapp.domain.comments.converter.CommentConverter;
import com.kakaobase.snsapp.domain.comments.dto.BotRecommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.BotRecommentResponseDto;
import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.comments.repository.RecommentRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotRecommentService {

    private final RecommentRepository recommentRepository;
    private final MemberRepository memberRepository;
    private final CommentConverter commentConverter;
    private final WebClient webClient;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Transactional
    public CommentResponseDto.RecommentInfo handle(Post post, Comment comment) {
        log.info("👉 [BotHandle] 트리거 시작 - postId={}, commentId={}", post.getId(), comment.getId());

        Member bot = memberRepository.findFirstByRole(Member.Role.BOT)
                .orElseThrow(() -> new IllegalStateException("소셜봇 계정이 없습니다."));

        Member writer = memberRepository.findById(post.getMember().getId())
                .orElseThrow(() -> new IllegalStateException("작성자 조회 실패"));

        List<Recomment> recomments = recommentRepository.findByCommentId(comment.getId());

        BotRecommentRequestDto requestDto = BotRecommentConverter.toRequestDto(post, writer, comment, recomments);

        BotRecommentResponseDto response = webClient.post()
                .uri(aiServerUrl + "/recomments/bot")
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(BotRecommentResponseDto.class)
                .block();

        String generatedContent = Objects.requireNonNull(response).getData().getContent();
        log.info("📩 [BotHandle] AI 생성 대댓글: {}", generatedContent);

        Recomment newRecomment = Recomment.builder()
                .comment(comment)
                .member(bot)
                .content(generatedContent)
                .build();
        recommentRepository.save(newRecomment);

        comment.increaseRecommentCount();

        return commentConverter.toRecommentInfoForBot(newRecomment, bot);
    }

    @Async
    public void triggerAsync(Post post, Comment comment) {
        try {
            log.info("🚀 [BotTrigger] 비동기 트리거 시작 - postId={}, commentId={}", post.getId(), comment.getId());
            handle(post, comment);
            log.info("✅ [BotTrigger] 성공적으로 처리됨");
        } catch (Exception e) {
            log.error("❌ [BotTrigger] 실패 - reason: {}", e.getMessage(), e);
        }
    }

}
