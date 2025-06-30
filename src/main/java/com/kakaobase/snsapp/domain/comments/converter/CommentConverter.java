package com.kakaobase.snsapp.domain.comments.converter;

import com.kakaobase.snsapp.domain.comments.dto.CommentRequestDto;
import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.CommentLike;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.comments.entity.RecommentLike;
import com.kakaobase.snsapp.domain.comments.exception.CommentErrorCode;
import com.kakaobase.snsapp.domain.comments.exception.CommentException;
import com.kakaobase.snsapp.domain.comments.service.cache.CommentCacheService;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
import com.kakaobase.snsapp.global.common.redis.error.CacheException;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 댓글과 대댓글 관련 엔티티와 DTO 간 변환을 담당하는 컨버터 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentConverter {

    private final CommentCacheService commentCacheService;
    private final EntityManager em;

    /**
     * 댓글 작성 요청 DTO를 댓글 엔티티로 변환
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
     */
    public Recomment toRecommentEntity(Comment parentComment,
                                       Member member,
                                       CommentRequestDto.CreateCommentRequest request) {

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

        Member commentOwner = comment.getMember();

        var userInfo = MemberResponseDto.UserInfo.builder()
                        .id(commentOwner.getId())
                        .name(commentOwner.getName())
                        .nickname(commentOwner.getNickname())
                        .imageUrl(commentOwner.getProfileImgUrl())
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

        Member recommentOwner = recomment.getMember();

        MemberResponseDto.UserInfo userInfo =
                MemberResponseDto.UserInfo.builder()
                        .id(recommentOwner.getId())
                        .name(recommentOwner.getName())
                        .nickname(recommentOwner.getNickname())
                        .imageUrl(recommentOwner.getProfileImgUrl())
                        .build();

        return new CommentResponseDto.CreateCommentResponse(
                recomment.getId(),
                userInfo,
                recomment.getContent(),
                recomment.getComment().getId()  // 부모 댓글 ID
        );
    }

    /**
     * 댓글 좋아요 엔티티 생성
     */
    public CommentLike toCommentLikeEntity(Long memberId, Long commentId) {

        Member proxyMember = em.getReference(Member.class, memberId);
        Comment proxyCommennt = em.getReference(Comment.class, commentId);

        return new CommentLike(proxyMember, proxyCommennt);
    }

    /**
     * 대댓글 좋아요 엔티티 생성
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
     * CommentInfo 리스트의 likeCount, recommentCount를 캐시 데이터로 업데이트
     */
    public List<CommentResponseDto.CommentInfo> updateWithCachedStats(
            List<CommentResponseDto.CommentInfo> commentInfos) {

        if (commentInfos == null || commentInfos.isEmpty()) {
            return commentInfos;
        }

        try{
            Map<Long, CacheRecord.CommentStatsCache> commentStatsCache = commentCacheService.findAllByItems(commentInfos);
            if (commentStatsCache == null || commentStatsCache.isEmpty()) {
                return commentInfos;
            }
            return commentInfos.stream()
                    .map(commentInfo -> updateSingleCommentStats(commentInfo, commentStatsCache.get(commentInfo.id())))
                    .toList();
        } catch (CacheException e) {
            log.error(e.getMessage());
            return commentInfos;
        }
    }

    /**
     * 단일 PostDetails의 통계 정보를 캐시 데이터로 업데이트
     */
    public CommentResponseDto.CommentInfo updateSingleCommentStats(
            CommentResponseDto.CommentInfo original,
            CacheRecord.CommentStatsCache cacheStats) {

        // 캐시 데이터가 없으면 원본 그대로 반환
        if (cacheStats == null) {
            return original;
        }
        return original.withStats(cacheStats.likeCount(), cacheStats.recommentCount());
    }

}