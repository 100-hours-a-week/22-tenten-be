package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.posts.dto.BotRequestDto;
import com.kakaobase.snsapp.domain.posts.dto.PostRequestDto;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.domain.posts.util.BoardType;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

/**
 * AI 봇의 게시글 관련 서비스
 *
 * <p>게시글이 5개 생성될 때마다 AI 서버에 요청하여 자동으로 봇 게시글을 생성합니다.</p>
 */
@Slf4j
@Service
public class BotPostService {

    private final PostService postService;
    private final PostRepository postRepository;
    private final WebClient webClient;

    @Value("${ai.server.url}")
    private String aiServerUrl;

    public BotPostService(@Qualifier("generalWebClient") WebClient webClient,
                          PostRepository postRepository,
                          PostService postService) {
        this.webClient = webClient;
        this.postRepository = postRepository;
        this.postService = postService;
    }

    /**
     * AI 봇 게시글 생성
     *
     * <p>최근 5개 게시글을 기반으로 AI 서버에 요청하여 봇 게시글을 생성합니다.</p>
     *
     * @param boardType 게시판 타입
     */
    @Transactional
    public void createBotPost(BoardType boardType) {
        try {
            log.info("봇 게시글 생성 시작 - boardType: {}", boardType);

            // 1. 최근 게시글 조회 (봇 게시글 필터링을 위해 여유있게 10개 조회)
            List<Post> recentPosts = postRepository.findTop10ByBoardTypeOrderByCreatedAtDescIdDesc(boardType);
            log.info("조회된 최근 게시글 수: {}", recentPosts.size());

            // 2. 봇이 작성하지 않은 게시글만 필터링하여 5개 선택
            List<Post> filteredPosts = filterNonBotPosts(recentPosts);

            if (filteredPosts.size() < 5) {
                log.warn("게시글이 5개 미만입니다. 봇 게시글 생성을 건너뜁니다. - count: {}", filteredPosts.size());
                return;
            }

            // 3. 오래된 순으로 정렬
            Collections.reverse(filteredPosts);
            logFilteredPosts(filteredPosts);

            // 4. AI 서버 요청 DTO 생성
            BotRequestDto.CreatePostRequest request = createBotRequest(boardType, filteredPosts);

            // 5. AI 서버 호출
            BotRequestDto.AiPostResponse aiResponse = callAiServer(request);

            // 6. 봇 게시글 저장
            saveBotPost(aiResponse);

            log.info("봇 게시글 생성 완료 - boardType: {}", boardType);

        } catch (Exception e) {
            log.error("봇 게시글 생성 실패 - boardType: {}", boardType, e);
        }
    }

    /**
     * 봇이 작성하지 않은 게시글만 필터링합니다.
     *
     * @param recentPosts 최근 게시글 목록
     * @return 필터링된 게시글 목록 (최대 5개)
     */
    private List<Post> filterNonBotPosts(List<Post> recentPosts) {
        List<Post> filteredPosts = new ArrayList<>();
        int botPostCount = 0;

        for (Post post : recentPosts) {
            if (!post.getMember().getId().equals(BotConstants.BOT_MEMBER_ID)) {
                filteredPosts.add(post);
                log.debug("일반 게시글 추가: id={}, content={}, createdAt={}",
                        post.getId(), post.getContent(), post.getCreatedAt());

                if (filteredPosts.size() == 5) {
                    log.info("5개의 일반 게시글 필터링 완료. 반복 중단");
                    break;
                }
            } else {
                botPostCount++;
                log.debug("봇 게시글 필터링 제외: id={}, content={}", post.getId(), post.getContent());
            }
        }

        log.info("필터링 결과 - 총 게시글: {}, 봇 게시글: {}, 일반 게시글: {}",
                recentPosts.size(), botPostCount, filteredPosts.size());

        return filteredPosts;
    }

    /**
     * 필터링된 게시글 목록을 로깅합니다.
     *
     * @param filteredPosts 필터링된 게시글 목록
     */
    private void logFilteredPosts(List<Post> filteredPosts) {
        log.debug("역순 정렬 후 게시글 순서(오래된순):");
        for (int i = 0; i < filteredPosts.size(); i++) {
            Post post = filteredPosts.get(i);
            log.debug("  {}. id={}, content={}, createdAt={}",
                    i + 1, post.getId(), post.getContent(), post.getCreatedAt());
        }
        log.info("AI에게 전송할 5개 게시글 준비 완료 (오래된순)");
    }

    /**
     * AI 서버 요청 DTO 생성
     *
     * @param boardType 게시판 타입
     * @return AI 서버 요청 DTO
     */
    private BotRequestDto.CreatePostRequest createBotRequest(BoardType boardType, List<Post> filteredPosts) {
        // 게시글 리스트를 PostDto 리스트로 변환
        List<BotRequestDto.PostDto> botPosts = filteredPosts.stream()
                .map(post -> BotRequestDto.PostDto.builder()
                        .user(BotRequestDto.UserDto.builder()
                                .nickname(post.getMember().getNickname())
                                .className(post.getMember().getClassName()) // enum을 문자열로 변환
                                .build())
                        .createdAt(formatUtc(post.getCreatedAt().atZone(ZoneOffset.UTC).toInstant())) // formatUtc 적용
                        .content(post.getContent())
                        .build())
                .toList();

        return new BotRequestDto.CreatePostRequest(boardType.name(), botPosts);
    }

    private String formatUtc(Instant instant) {
        // 마이크로초까지만 출력하고 Z 붙이기
        long seconds = instant.getEpochSecond();
        int micros = instant.getNano() / 1000;  // 나노초를 마이크로초로 변환

        // 포맷: yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'
        return String.format("%s.%06dZ",
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                        .withZone(ZoneOffset.UTC)
                        .format(instant),
                micros
        );
    }



    /**
     * AI 서버 호출
     *
     * @param request AI 서버 요청 DTO
     * @return AI 서버 응답 DTO
     */
    private BotRequestDto.AiPostResponse callAiServer(BotRequestDto.CreatePostRequest request) {
        try {
            return webClient.post()
                    .uri(aiServerUrl + "/posts/bot")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BotRequestDto.AiPostResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("AI 서버 요청 실패 - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI 서버 통신 오류", e);
        }
    }

    /**
     * 봇 게시글 저장
     *
     * @param aiResponse AI 서버 응답
     */
    private void saveBotPost(BotRequestDto.AiPostResponse aiResponse) {
        BotRequestDto.AiResponseData data = aiResponse.data();

        PostRequestDto.PostCreateRequestDto requestDto = new PostRequestDto.PostCreateRequestDto(
                data.content(),
                null,  // image_url
                null   // youtube_url
        );

        // AI 응답 데이터를 사용하여 게시글 생성
        String postType = data.boardType();

        // PostService를 통해 게시글 생성 (PostResponseDto.PostDetails 반환)
        postService.createPost(postType, requestDto, BotConstants.BOT_MEMBER_ID);
    }
}