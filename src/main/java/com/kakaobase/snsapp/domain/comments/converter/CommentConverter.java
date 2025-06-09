package com.kakaobase.snsapp.domain.comments.converter;

import com.kakaobase.snsapp.domain.comments.dto.CommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.CommentLike;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.comments.entity.RecommentLike;
import com.kakaobase.snsapp.domain.comments.exception.CommentErrorCode;
import com.kakaobase.snsapp.domain.comments.exception.CommentException;
import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 댓글과 대댓글 관련 엔티티와 DTO 간 변환을 담당하는 컨버터 클래스
 */
@Component
@RequiredArgsConstructor
public class CommentConverter {

    private final EntityManager em;

    /**
     * 댓글 작성 요청 DTO를 댓글 엔티티로 변환
     *
     * @param post 댓글이 작성될 게시글
     * @param member 댓글 작성자
     * @param request 댓글 작성 요청 DTO
     * @return 생성된 댓글 엔티티
     */
    public Comment toCommentEntity(
            Post post,
            Member member,
            CommentRequestDto.CreateCommentRequest request) {
        validateContent(request.content());

        return Comment.builder()
                .post(post)
                .member(member)
                .content(request.content())
                .build();
    }

    /**
     * 대댓글 작성 요청 DTO를 대댓글 엔티티로 변환
     *
     * @param parentComment 대댓글이 작성될 부모 댓글
     * @param member 대댓글 작성자
     * @param request 대댓글 작성 요청 DTO
     * @return 생성된 대댓글 엔티티
     */
    public Recomment toRecommentEntity(Comment parentComment, Member member, CommentRequestDto.CreateCommentRequest request) {
        validateContent(request.content());

        return Recomment.builder()
                .comment(parentComment)
                .member(member)
                .content(request.content())
                .build();
    }

    /**
     * 댓글 엔티티를 댓글 생성 응답 DTO로 변환
     *
     * @param comment 댓글 엔티티
     * @return 댓글 생성 응답 DTO
     */
    public CommentResponseDto.CreateCommentResponse toCreateCommentResponse(Comment comment) {

        Member CommentOwner = comment.getMember();

        MemberResponseDto.UserInfo userInfo =
                MemberResponseDto.UserInfo.builder()
                        .id(CommentOwner.getId())
                        .name(CommentOwner.getName())
                        .nickname(CommentOwner.getNickname())
                        .imageUrl(CommentOwner.getProfileImgUrl())
                        .build();

        return new CommentResponseDto.CreateCommentResponse(
                comment.getId(),
                userInfo,
                comment.getContent(),
                null  // 일반 댓글이므로 parent_id는 null
        );
    }

    /**
     * 대댓글 엔티티를 대댓글 생성 응답 DTO로 변환
     *
     * @param recomment 대댓글 엔티티
     * @return 대댓글 생성 응답 DTO
     */
    public CommentResponseDto.CreateCommentResponse toCreateRecommentResponse(Recomment recomment) {

        Member RecommentOwner = recomment.getMember();

        MemberResponseDto.UserInfo userInfo =
                MemberResponseDto.UserInfo.builder()
                        .id(RecommentOwner.getId())
                        .name(RecommentOwner.getName())
                        .nickname(RecommentOwner.getNickname())
                        .imageUrl(RecommentOwner.getProfileImgUrl())
                        .build();

        return new CommentResponseDto.CreateCommentResponse(
                recomment.getId(),
                userInfo,
                recomment.getContent(),
                recomment.getComment().getId()  // 부모 댓글 ID
        );
    }

    /**
     * 댓글 엔티티를 댓글 상세 정보 DTO로 변환
     *
     * @param comment 댓글 엔티티
     * @return 댓글 상세 정보 DTO
     */
    public CommentResponseDto.CommentInfo toCommentInfo(
            Comment comment,
            Boolean isMine,
            Boolean isLiked,
            Boolean isFollowing
    ) {

        Member CommentOwner = comment.getMember();

        MemberResponseDto.UserInfoWithFollowing userInfo =
                MemberResponseDto.UserInfoWithFollowing.builder()
                        .id(CommentOwner.getId())
                        .nickname(CommentOwner.getNickname())
                        .imageUrl(CommentOwner.getProfileImgUrl())
                        .isFollowed(isFollowing)
                        .build();

        return new CommentResponseDto.CommentInfo(
                comment.getId(),
                userInfo,
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getLikeCount(),
                comment.getRecommentCount(),
                isMine,
                isLiked
        );
    }


    /**
     * 대댓글 목록을 대댓글 목록 응답 DTO로 변환
     *
     * @param recomments 대댓글 목록
     * @param currentMemberId 현재 로그인한 회원 ID
     * @param likedRecommentIds 좋아요 누른 대댓글 ID 목록
     * @param nextCursor 다음 페이지 커서
     * @return 대댓글 목록 응답 DTO
     */
    public CommentResponseDto.RecommentListResponse toRecommentListResponse(
            List<Recomment> recomments,
            Long currentMemberId,
            Set<Long> likedRecommentIds,
            Set<Long> followingMemberIds,
            Long nextCursor
    ) {
        List<CommentResponseDto.RecommentInfo> recommentInfos = recomments.stream()
                .map(recomment -> toRecommentInfo(
                        recomment,
                        currentMemberId,
                        likedRecommentIds,
                        followingMemberIds
                ))
                .collect(Collectors.toList());

        return new CommentResponseDto.RecommentListResponse(
                recommentInfos,
                nextCursor != null,
                nextCursor
        );
    }

    /**
     * 대댓글 엔티티를 대댓글 상세 정보 DTO로 변환
     *
     * @param recomment 대댓글 엔티티
     * @param currentMemberId 현재 로그인한 회원 ID
     * @param likedRecommentIds 좋아요 누른 대댓글 ID 목록
     * @return 대댓글 상세 정보 DTO
     */
    public CommentResponseDto.RecommentInfo toRecommentInfo(
            Recomment recomment,
            Long currentMemberId,
            Set<Long> likedRecommentIds,
            Set<Long> followingMemberIds
    ) {

        Member CommentOwner = recomment.getMember();

        MemberResponseDto.UserInfoWithFollowing userInfo =
                MemberResponseDto.UserInfoWithFollowing.builder()
                        .id(CommentOwner.getId())
                        .imageUrl(CommentOwner.getProfileImgUrl())
                        .nickname(CommentOwner.getNickname())
                        .isFollowed(followingMemberIds != null && followingMemberIds.contains(CommentOwner.getId()))
                        .build();

        return new CommentResponseDto.RecommentInfo(
                recomment.getId(),
                userInfo,
                recomment.getContent(),
                recomment.getCreatedAt(),
                recomment.getLikeCount(),
                recomment.getMember().getId().equals(currentMemberId),
                likedRecommentIds != null && likedRecommentIds.contains(recomment.getId())
        );
    }

    /**
     * 댓글 좋아요 엔티티 생성
     *
     * @param memberId 회원 ID
     * @param commentId 댓글 ID
     * @return 댓글 좋아요 엔티티
     */
    public CommentLike toCommentLikeEntity(Long memberId, Long commentId) {

        Member proxyMember = em.getReference(Member.class, memberId);
        Comment proxyCommennt = em.getReference(Comment.class, commentId);

        return new CommentLike(proxyMember, proxyCommennt);
    }

    /**
     * 대댓글 좋아요 엔티티 생성
     *
     * @param memberId 회원 ID
     * @param recommentId 대댓글 ID
     * @return 대댓글 좋아요 엔티티
     */
    public RecommentLike toRecommentLikeEntity(Long memberId, Long recommentId) {

        Member proxyMember = em.getReference(Member.class, memberId);
        Recomment proxyRecomment = em.getReference(Recomment.class, recommentId);

        return new RecommentLike(proxyMember, proxyRecomment);
    }

    /**
     * 댓글/대댓글 내용 유효성 검증
     */
    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "content");
        }

        if (content.length() > 2000) {
            throw new CommentException(CommentErrorCode.CONTENT_LENGTH_EXCEEDED);
        }
    }

    /**
     * 소셜봇용 대댓글 응답 DTO 생성
     *
     * 로그인 사용자 정보 없이 기본 정보만 포함합니다.
     * 좋아요 수, is_mine, is_liked, is_followed 모두 false 또는 0으로 초기화됩니다.
     *
     * @param recomment 생성된 대댓글 엔티티
     * @param bot       소셜봇 사용자
     * @return 대댓글 응답 DTO
     */
    public CommentResponseDto.RecommentInfo toRecommentInfoForBot(Recomment recomment, Member bot) {
        MemberResponseDto.UserInfoWithFollowing userInfo =
                MemberResponseDto.UserInfoWithFollowing.builder()
                        .id(bot.getId())
                        .nickname(bot.getNickname())
                        .imageUrl(bot.getProfileImgUrl())
                        .isFollowed(false)
                        .build();

        return new CommentResponseDto.RecommentInfo(
                recomment.getId(),
                userInfo,
                recomment.getContent(),
                recomment.getCreatedAt(),
                0,       // like_count
                false,   // is_mine
                false    // is_liked
        );
    }

}