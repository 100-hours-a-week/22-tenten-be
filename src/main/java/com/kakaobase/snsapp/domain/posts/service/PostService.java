package com.kakaobase.snsapp.domain.posts.service;

import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.service.MemberService;
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
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.global.common.s3.service.S3Service;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final MemberService memberService;
    private final YouTubeSummaryService youtubeSummaryService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PostLikeService postLikeService;
    private final FollowRepository followRepository;
    private final EntityManager em;
    private final PostConverter postConverter;

    /**
     * 게시글을 생성합니다.
     *
     * @param boardType 게시판 유형
     * @param requestDto 게시글 생성 요청 DTO
     * @param memberId 작성자 ID
     * @return 생성된 게시글 엔티티
     */
    @Transactional
    public PostResponseDto.PostDetails createPost(Post.BoardType boardType, PostRequestDto.PostCreateRequestDto requestDto, Long memberId) {
        // 이미지 URL 유효성 검증
        if (StringUtils.hasText(requestDto.image_url()) && !s3Service.isValidImageUrl(requestDto.image_url())) {
            throw new PostException(PostErrorCode.INVALID_IMAGE_URL);
        }

        String youtubeUrl = requestDto.youtube_url();

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
        // 게시글 조회
        Post post = findById(postId);

        // 좋아요 여부 확인
        boolean isLiked = memberId != null && postLikeService.isLikedByMember(postId, memberId);

        Member follower = em.getReference(Member.class, memberId);
        Member following = em.getReference(Member.class, post.getMember().getId());

        boolean isFollowing = followRepository.existsByFollowerUserAndFollowingUser(follower, following);

        // 이미지 조회
        String postImage = null;
        List<PostImage> postImages = postImageRepository.findByPostIdOrderBySortIndexAsc(post.getId());
        if(!postImages.isEmpty()){
            postImage = postImages.get(0).getImgUrl();
        }

        // 응답 DTO 생성 및 반환
        return postConverter.convertToPostDetail(post, memberId, postImage, isLiked, isFollowing);
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

        // 소프트 삭제 처리
        postRepository.delete(post);

        log.info("게시글 삭제 완료: 게시글 ID={}, 삭제자 ID={}", postId, memberId);
    }

    /**
     * 게시글 목록을 조회합니다.
     *
     * @param postType 게시판 유형
     * @param limit 페이지 크기
     * @param cursor 커서
     * @param memberId 현재 사용자 ID (nullable)
     * @return 게시글 목록 응답
     */
    /**
     * 게시글 목록을 조회합니다.
     */
    public List<PostResponseDto.PostDetails> getPostList(String postType, int limit, Long cursor, Long currentMemberId) {
        // 1. 유효성 검증
        if (limit < 1) {
            throw new PostException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit", "limit는 1 이상이어야 합니다.");
        }

        Post.BoardType boardType = PostConverter.toBoardType(postType);
        Pageable pageable = PageRequest.of(0, limit);

        // 3. 게시글 조회
        List<Post> posts = postRepository.findByBoardTypeWithCursor(boardType, cursor, pageable);

        // 3. PostListItem으로 변환
        return postConverter.convertToPostListItems(posts, currentMemberId);
    }

    /**
     * 유저가 작성한 게시글 조회
     */
    public List<PostResponseDto.PostDetails> getUserPostList(int limit, Long cursor, Long currentMemberId) {
        // 1. 유효성 검증
        if (limit < 1) {
            throw new PostException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit", "limit는 1 이상이어야 합니다.");
        }

        Pageable pageable = PageRequest.of(0, limit);

        // 3. 게시글 조회
        List<Post> posts = postRepository.findByMemberIdWithCursor(currentMemberId, cursor, pageable);

        // 3. PostListItem으로 변환
        return postConverter.convertToPostListItems(posts, currentMemberId);

    }

    public List<PostResponseDto.PostDetails> getLikedPostList(int limit, Long cursor, Long currentMemberId) {
        // 1. 유효성 검증
        if (limit < 1) {
            throw new PostException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "limit", "limit는 1 이상이어야 합니다.");
        }

        Pageable pageable = PageRequest.of(0, limit);

        // 3. 게시글 조회
        List<Post> posts = postRepository.findLikedPostsByMemberIdWithCursor(currentMemberId, cursor, pageable);

        // 3. PostListItem으로 변환
        return postConverter.convertToPostListItems(posts, currentMemberId);
    }

    /**
     * 회원 ID로 회원 정보를 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 회원 정보 (닉네임, 프로필 이미지)
     */
    public Map<String, String> getMemberInfo(Long memberId) {
        return memberService.getMemberInfo(memberId);
    }


    /**
     * YouTube 영상 요약
     *
     * <p>게시글에 포함된 YouTube URL의 영상을 요약하고 결과를 저장합니다.</p>
     *
     * @param postId 요약할 게시글의 ID
     * @param memberId 요청한 사용자의 ID
     * @return YouTube 요약 응답 DTO
     * @throws PostException 게시글을 찾을 수 없거나, 권한이 없거나, YouTube URL이 없는 경우
     */
    @Transactional
    public PostResponseDto.YouTubeSummaryResponse summarizeYoutube(Long postId, Long memberId) {
        log.info("YouTube 요약 요청 - postId: {}, memberId: {}", postId, memberId);

        // 1. 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("게시글을 찾을 수 없음 - postId: {}", postId);
                    return new PostException(GeneralErrorCode.RESOURCE_NOT_FOUND, "postId");
                });


        String summary = post.getYoutubeSummary();

        //summary의 상태값이 YoutubeSummaryStatus과 같다면 에러응답 반환
        for (YoutubeSummaryStatus status : YoutubeSummaryStatus.values()) {
            if (status.name().equals(summary)) {
                throw new PostException(status.getPostErrorCode());
            }
        }
        return PostResponseDto.YouTubeSummaryResponse.of(summary);
    }
}