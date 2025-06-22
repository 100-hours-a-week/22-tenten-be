package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.comments.exception.CommentException;
import com.kakaobase.snsapp.domain.comments.repository.CommentLikeRepository;
import com.kakaobase.snsapp.domain.comments.repository.CommentRepository;
import com.kakaobase.snsapp.domain.comments.repository.RecommentRepository;
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
import com.kakaobase.snsapp.domain.posts.exception.YoutubeSummaryStatus;
import com.kakaobase.snsapp.domain.posts.repository.PostImageRepository;
import com.kakaobase.snsapp.domain.posts.repository.PostLikeRepository;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
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
@Transactional(readOnly = true)
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
    private final CommentRepository commentRepository;
    private final RecommentRepository recommentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostLikeRepository postLikeRepository;

    /**
     * 게시글을 생성합니다.
     *
     * @param postType 게시판 유형
     * @param requestDto 게시글 생성 요청 DTO
     * @param memberId 작성자 ID
     * @return 생성된 게시글 엔티티
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

        Post.BoardType boardType;

        try {
            boardType = Post.BoardType.valueOf(postType);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 게시판 타입: {}", postType);
            throw new PostException(GeneralErrorCode.INVALID_FORMAT, "postType");
        }

        String youtubeUrl = requestDto.youtube_url();

        // 게시판 타입 변환
        Member proxyMember = em.find(Member.class, memberId);
        // 게시글 엔티티 생성
        Post post = PostConverter.toPost(requestDto, proxyMember, boardType);

        // 게시글 저장
        postRepository.save(post);

        if (StringUtils.hasText(requestDto.image_url())) {
            PostImage postImage = PostConverter.toPostImage(post, 0, requestDto.image_url());
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
     *
     * @param postId 게시글 ID
     * @param memberId 현재 사용자 ID
     * @return 게시글 상세 정보
     */
    public PostResponseDto.PostDetails getPostDetail(Long postId, Long memberId) {

        PostResponseDto.PostDetails postDetails = postRepository.findPostDetailById(postId, memberId)
                .orElseThrow(() -> new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId"));

        CacheRecord.PostStatsCache cache = postCacheService.findBy(postId);

        return postConverter.updateSinglePostStats(postDetails, cache);
    }

    /**
     * 게시글 ID로 게시글을 조회합니다.
     *
     * @param postId 게시글 ID
     * @return 조회된 게시글 엔티티
     */
    public Post findById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId", "해당 게시글을 찾을 수 없습니다"));
    }

    /**
     * 게시글을 삭제합니다.
     *
     * @param postId 게시글 ID
     * @param memberId 삭제자 ID
     */
    @Transactional
    public void deletePost(Long postId, Long memberId) {
        // 게시글 조회 - AccessChecker에서 이미 권한 검증을 했으므로 간소화 가능
        Post post = findById(postId);

        //해당 게시글과 연관된 댓글, 대댓글과 좋아요 삭제
        recommentRepository.deleteByPostId(postId);
        recommentRepository.deleteByPostId(postId);
        commentRepository.deleteByPostId(postId);
        commentLikeRepository.deleteByPostId(postId);
        postLikeRepository.deleteByPostId(postId);
        // 소프트 삭제 처리
        postRepository.delete(post);
        //캐시에서 제거
        postCacheService.delete(post.getId());

        log.info("게시글 삭제 완료: 게시글 ID={}, 삭제자 ID={}", postId, memberId);
    }


    /**
     * 게시글 목록을 조회합니다.
     */
    public List<PostResponseDto.PostDetails> getPostList(String postType, int limit, Long cursor, Long currentMemberId) {
        // 1. 유효성 검증
        if (limit < 1) {
            throw new PostException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit", "limit는 1 이상이어야 합니다.");
        }

        Post.BoardType boardType = PostConverter.toBoardType(postType);

        // 2. 게시글 조회
        List<PostResponseDto.PostDetails> postDetails = postRepository.findByBoardTypeWithCursor(boardType, cursor, limit, currentMemberId);

        // 3. PostListItem으로 변환
        return postConverter.updateWithCachedStats(postDetails);
    }

    /**
     * 유저가 작성한 게시글 조회
     */
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