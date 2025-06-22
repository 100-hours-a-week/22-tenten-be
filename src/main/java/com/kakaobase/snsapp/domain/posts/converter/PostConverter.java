package com.kakaobase.snsapp.domain.posts.converter;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.posts.dto.PostRequestDto;
import com.kakaobase.snsapp.domain.posts.dto.PostResponseDto;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.entity.PostImage;
import com.kakaobase.snsapp.domain.posts.exception.PostException;
import com.kakaobase.snsapp.domain.posts.service.PostCacheService;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
/**
 * Post 도메인의 Entity와 DTO 간 변환을 담당하는 Converter 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostConverter {
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


    private MemberResponseDto.UserInfoWithFollowing convertToUserInfo(Member member, boolean isFollowed) {
        return MemberResponseDto.UserInfoWithFollowing.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .imageUrl(member.getProfileImgUrl())
                .isFollowed(isFollowed)
                .build();
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