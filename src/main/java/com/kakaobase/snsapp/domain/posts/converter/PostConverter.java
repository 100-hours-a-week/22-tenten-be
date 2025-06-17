package com.kakaobase.snsapp.domain.posts.converter;

import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.domain.posts.dto.PostRequestDto;
import com.kakaobase.snsapp.domain.posts.dto.PostResponseDto;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.entity.PostImage;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.repository.PostImageRepository;
import com.kakaobase.snsapp.domain.posts.repository.PostLikeRepository;
import com.kakaobase.snsapp.domain.posts.service.PostCacheService;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Post 도메인의 Entity와 DTO 간 변환을 담당하는 Converter 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostConverter {

    private final MemberRepository memberRepository;
    private final PostImageRepository postImageRepository;
    private final FollowRepository followRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCacheService postCacheService;

    /**
     * 게시글 생성 요청 DTO를 Post 엔티티로 변환합니다.
     *
     * @param requestDto 게시글 생성 요청 DTO
     * @param member 작성자 객체
     * @param boardType 게시판 타입
     * @return 생성된 Post 엔티티
     */
    public static Post toPost(
            PostRequestDto.PostCreateRequestDto requestDto,
            Member member,
            Post.BoardType boardType) {

        return Post.builder()
                .member(member)
                .boardType(boardType)
                .content(requestDto.content())
                .youtubeUrl(requestDto.youtube_url())
                .build();
    }

    /**
     * 게시글 이미지 엔티티를 생성합니다
     *
     */
    public static PostImage toPostImage(
            Post post,
            Integer sortIndex,
            String imageUrl) {

        return PostImage.builder()
                .post(post)
                .sortIndex(sortIndex)
                .imgUrl(imageUrl)
                .build();
    }

    public List<PostResponseDto.PostDetails> convertToPostListItems(List<Post> posts, Long memberId) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. ID 추출
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        List<Long> memberIds = posts.stream()
                .map(post -> post.getMember().getId())
                .distinct()
                .toList();

        // 2. 배치로 추가 데이터 조회
        Map<Long, String> postImageMap = getFirstImagesByPostIds(postIds);
        Map<Long, CacheRecord.PostStatsCache> postStatsCache = postCacheService.getPostStatsBatch(posts);
        Set<Long> likedPostIds = memberId != null ?
                getLikedPostIds(memberId, postIds) : Collections.emptySet();
        Set<Long> followedMemberIds = memberId != null ?
                getFollowedMemberIds(memberId, memberIds) : Collections.emptySet();

        // 3. 각 Post를 PostDetails로 변환
        return posts.stream()
                .map(post -> {
                    // 각 게시글별 데이터 추출
                    String imageUrl = postImageMap.get(post.getId());

                    // 캐시에서 통계 정보 가져오기 (없으면 Post 엔티티 값 사용)
                    CacheRecord.PostStatsCache statsCache = postStatsCache.get(post.getId());
                    Long likeCount;
                    Long commentCount;

                    if (statsCache != null) {
                        likeCount = statsCache.likeCount();
                        commentCount = statsCache.commentCount();
                        log.debug("캐시에서 통계 사용: postId={}, like={}, comment={}",
                                post.getId(), likeCount, commentCount);
                    } else {
                        // 캐시에 없으면 Post 엔티티의 값을 fallback으로 사용
                        likeCount = post.getLikeCount();
                        commentCount = post.getCommentCount();
                        log.debug("Post 엔티티 값을 fallback으로 사용: postId={}, like={}, comment={}",
                                post.getId(), likeCount, commentCount);
                    }

                    // 새로운 시그니처로 convertToPostDetail 호출
                    return convertToPostDetail(
                            post,
                            memberId,
                            imageUrl,
                            likeCount,
                            commentCount,
                            likedPostIds,
                            followedMemberIds
                    );
                })
                .toList();
    }


    private PostResponseDto.PostDetails convertToPostDetail(Post post, Long currentMemberId,
                                                            String imageUrl,
                                                            Long likeCount,
                                                            Long commentCount,
                                                            Set<Long> likedPostIds,
                                                            Set<Long> followedMemberIds) {
        Member member = post.getMember();

        return PostResponseDto.PostDetails.builder()
                .id(post.getId())
                .user(convertToUserInfo(member, currentMemberId, followedMemberIds))
                .content(post.getContent())
                .imageUrl(imageUrl) // 첫 번째 이미지 URL
                .youtubeUrl(post.getYoutubeUrl())
                .youtubeSummary(post.getYoutubeSummary())
                .createdAt(post.getCreatedAt())
                .likeCount(likeCount)      // 캐시에서 가져온 좋아요 수
                .commentCount(commentCount)   // 캐시에서 가져온 댓글 수
                .isMine(currentMemberId != null && currentMemberId.equals(member.getId())) // isMine
                .isLiked(likedPostIds.contains(post.getId())) // isLiked
                .build();
    }

    public PostResponseDto.PostDetails convertToPostDetail(Post post, Long currentMemberId,
                                                            String imageUrl,
                                                            Boolean isLiked,
                                                            Boolean isFollowed) {
        Member member = post.getMember();

        return new PostResponseDto.PostDetails(
                post.getId(),
                convertToUserInfo(member, isFollowed),
                post.getContent(),
                imageUrl, // 첫 번째 이미지 URL
                post.getYoutubeUrl(),
                post.getYoutubeSummary(),
                post.getCreatedAt(),
                post.getLikeCount(),
                post.getCommentCount(),
                currentMemberId != null && currentMemberId.equals(member.getId()), // isMine
                isLiked
        );
    }

    /**
     * Member를 UserInfoWithFollowing으로 변환
     */
    private MemberResponseDto.UserInfoWithFollowing convertToUserInfo(Member member, Long currentMemberId, Set<Long> followedMemberIds) {
        return MemberResponseDto.UserInfoWithFollowing.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .imageUrl(member.getProfileImgUrl())
                .isFollowed(currentMemberId != null && followedMemberIds.contains(member.getId()))
                .build();
    }

    private MemberResponseDto.UserInfoWithFollowing convertToUserInfo(Member member, boolean isFollowed) {
        return MemberResponseDto.UserInfoWithFollowing.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .imageUrl(member.getProfileImgUrl())
                .isFollowed(isFollowed)
                .build();
    }

    /**
     * 게시글들의 첫 번째 이미지 조회 (기존 PostImageRepository 메서드 활용)
     */
    private Map<Long, String> getFirstImagesByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 기존 PostImageRepository.findFirstImagesByPostIds() 사용
        List<PostImage> firstImages = postImageRepository.findFirstImagesByPostIds(postIds);

        return firstImages.stream()
                .collect(Collectors.toMap(
                        postImage -> postImage.getPost().getId(),
                        PostImage::getImgUrl
                ));
    }


    /**
     * 현재 회원이 좋아요한 게시글 ID들 조회 (기존 PostLikeRepository 메서드 활용)
     */
    private Set<Long> getLikedPostIds(Long memberId, List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptySet();
        }

        // 기존 PostLikeRepository.findPostIdsByMemberIdAndPostIdIn() 사용
        List<Long> likedIds = postLikeRepository.findPostIdsByMemberIdAndPostIdIn(memberId, postIds);
        return new HashSet<>(likedIds);
    }

    /**
     * 현재 회원이 팔로우한 회원 ID들 조회 (기존 FollowRepository 메서드 활용)
     */
    private Set<Long> getFollowedMemberIds(Long currentMemberId, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Collections.emptySet();
        }

        // 현재 회원 Entity 조회
        Member currentMember = memberRepository.findById(currentMemberId)
                .orElse(null);

        if (currentMember == null) {
            return Collections.emptySet();
        }

        // 기존 FollowRepository.findFollowingUserIdsByFollowerUser() 사용
        Set<Long> allFollowedIds = followRepository.findFollowingUserIdsByFollowerUser(currentMember);

        // memberIds와 교집합만 반환 (성능 최적화)
        return allFollowedIds.stream()
                .filter(memberIds::contains)
                .collect(Collectors.toSet());
    }

    /**
     * 문자열 형태의 postType을 BoardType enum으로 변환합니다.
     *
     * @param postType 게시판 타입 문자열
     * @return BoardType enum 값
     * @throws IllegalArgumentException 유효하지 않은 postType인 경우
     */
    public static Post.BoardType toBoardType(String postType) {
        try {
            if ("all".equalsIgnoreCase(postType)) {
                return Post.BoardType.ALL;
            }

            // snake_case를 대문자와 underscore로 변환 (pangyo_1 -> PANGYO_1)
            String enumFormat = postType.toUpperCase();
            return Post.BoardType.valueOf(enumFormat);
        } catch (IllegalArgumentException e) {
            throw new PostException(GeneralErrorCode.INVALID_QUERY_PARAMETER, "postType");
        }
    }
}