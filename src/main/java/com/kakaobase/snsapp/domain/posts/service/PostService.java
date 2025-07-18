package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.comments.exception.CommentException;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.domain.posts.converter.PostConverter;
import com.kakaobase.snsapp.domain.posts.dto.PostRequestDto;
import com.kakaobase.snsapp.domain.posts.dto.PostResponseDto;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.entity.PostImage;
import com.kakaobase.snsapp.domain.posts.event.PostCreatedEvent;
import com.kakaobase.snsapp.domain.posts.exception.PostErrorCode;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostImageRepository;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.domain.posts.service.async.YouTubeSummaryService;
import com.kakaobase.snsapp.domain.posts.service.cache.PostCacheService;
import com.kakaobase.snsapp.domain.posts.util.BoardType;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.error.CacheException;
import com.kakaobase.snsapp.global.common.s3.service.S3Service;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 게시글 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final S3Service s3Service;
    private final YouTubeSummaryService youtubeSummaryService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final EntityManager em;
    private final PostConverter postConverter;
    private final MemberRepository memberRepository;
    private final PostCacheService postCacheService;

    /**
     * 게시글을 생성합니다.
     */
    @Transactional
    public PostResponseDto.PostDetails createPost(String postType, PostRequestDto.PostCreateRequestDto requestDto, Long memberId) {
        // 게시글 내용 유효성 검증
        if (requestDto.isEmpty()) {
            throw new PostException(PostErrorCode.EMPTY_POST_CONTENT);
        }

        // 유튜브 URL 유효성 검증
        if (!requestDto.isValidYoutubeUrl()) {
            throw new PostException(PostErrorCode.INVALID_YOUTUBE_URL);
        }

        // 이미지 URL 유효성 검증
        if (StringUtils.hasText(requestDto.image_url()) && !s3Service.isValidImageUrl(requestDto.image_url())) {
            throw new PostException(PostErrorCode.INVALID_IMAGE_URL);
        }

        BoardType boardType = postConverter.toBoardType(postType);

        String youtubeUrl = requestDto.youtube_url();

        // 게시판 타입 변환
        Member proxyMember = em.find(Member.class, memberId);
        // 게시글 엔티티 생성
        Post post = postConverter.toPost(requestDto, proxyMember, boardType);

        // 게시글 저장
        postRepository.save(post);

        if (StringUtils.hasText(requestDto.image_url())) {
            PostImage postImage = postConverter.toPostImage(post, 0, requestDto.image_url());
            postImageRepository.save(postImage);
        }

        // 트랜잭션 커밋 후 비동기 요약 실행 예약
        if (StringUtils.hasText(youtubeUrl)) {
            final Long postId = post.getId();  // final로 캡처
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info(" 트랜잭션 커밋 완료 후 유튜브 요약 시작: postId={}", postId);
                    youtubeSummaryService.processYoutubeSummary(postId);
                }
            });
        }

        // 게시글 생성 이벤트 발행
        applicationEventPublisher.publishEvent(new PostCreatedEvent(post.getId(), boardType, memberId));

        return postConverter.convertToPostDetail(post, memberId, requestDto.image_url(), false, false);
    }

    /**
     * 게시글 상세 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public PostResponseDto.PostDetails getPostDetail(Long postId, Long memberId) {

        PostResponseDto.PostDetails postDetails = postRepository.findPostDetailById(postId, memberId)
                .orElseThrow(() -> new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId"));

        try{
            CacheRecord.PostStatsCache cache = postCacheService.findBy(postId);
            return postConverter.updateSinglePostStats(postDetails, cache);
        } catch (CacheException e){
            return postDetails;
        }
    }

    /**
     * 게시글을 삭제합니다.
     */
    @Transactional
    public void deletePost(Long postId) {

        if(!postRepository.existsById(postId)) {
            throw new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND);
        }

        postCacheService.delete(postId);
        postRepository.deletePost(postId);
    }

    /**
     * 게시글 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto.PostDetails> getPostList(String postType, int limit, Long cursor, Long currentMemberId) {
        // 1. 유효성 검증
        if (limit < 1) {
            throw new PostException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit", "limit는 1 이상이어야 합니다.");
        }

        BoardType boardType = postConverter.toBoardType(postType.toUpperCase());

        // 2. 게시글 조회
        List<PostResponseDto.PostDetails> postDetails = postRepository.findByBoardTypeWithCursor(boardType, cursor, limit, currentMemberId);

        // 3. PostListItem으로 변환
        return postConverter.updateWithCachedStats(postDetails);
    }

    /**
     * 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto.PostDetails> getUserPostList(int limit, Long cursor, Long memberId, Long currentMemberId) {
        // 1. 유효성 검증
        if (limit < 1) {
            throw new PostException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit", "limit는 1 이상이어야 합니다.");
        }

        // 2. 게시글 조회
        List<PostResponseDto.PostDetails> postDetails = postRepository.findByMemberWithCursor(memberId, cursor, limit, currentMemberId);

        // 3. PostListItem으로 변환
        return postConverter.updateWithCachedStats(postDetails);

    }

    /**
     * 유저가 좋아요한 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto.PostDetails> getLikedPostList(int limit, Long cursor, Long memberId, Long currentMemberId) {

        if(!memberRepository.existsById(memberId)){
            throw new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId");
        }

        if (limit < 1) {
            throw new PostException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit");
        }

        // 3. 게시글 조회
        List<PostResponseDto.PostDetails> postDetails = postRepository.findLikedPostsWithCursor(memberId, cursor, limit, currentMemberId);

        // 4. 캐싱데이터로 최신화후 반환
        return postConverter.updateWithCachedStats(postDetails);
    }
}