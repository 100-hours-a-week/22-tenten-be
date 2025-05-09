package com.kakaobase.snsapp.domain.comments.converter;

import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;
import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.entity.CommentLike;
import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.comments.entity.RecommentLike;
import com.kakaobase.snsapp.domain.comments.exception.CommentException;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import org.springframework.stereotype.Component;

/**
 * 댓글 및 대댓글 좋아요 관련 변환을 담당하는 컨버터 클래스
 */
@Component
public class LikeConverter {

    /**
     * 댓글 좋아요 엔티티 생성
     *
     * @param memberId 회원 ID
     * @param commentId 댓글 ID
     * @return 댓글 좋아요 엔티티
     */
    public CommentLike toCommentLikeEntity(Long memberId, Long commentId) {
        return new CommentLike(memberId, commentId);
    }

    /**
     * 대댓글 좋아요 엔티티 생성
     *
     * @param memberId 회원 ID
     * @param recommentId 대댓글 ID
     * @return 대댓글 좋아요 엔티티
     */
    public RecommentLike toRecommentLikeEntity(Long memberId, Long recommentId) {
        return new RecommentLike(memberId, recommentId);
    }

    /**
     * 댓글 좋아요 토글 응답 DTO 생성
     *
     * @param comment 댓글 엔티티
     * @param isLiked 좋아요 상태
     * @return 댓글 좋아요 토글 응답 DTO
     */
    public CommentResponseDto.CommentLikeResponse toCommentLikeResponse(Comment comment, boolean isLiked) {
        if (comment == null) {
            throw new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "commentId", "존재하지 않는 댓글입니다.");
        }
        return new CommentResponseDto.CommentLikeResponse(isLiked, comment.getLikeCount());
    }

    /**
     * 대댓글 좋아요 토글 응답 DTO 생성
     *
     * @param recomment 대댓글 엔티티
     * @param isLiked 좋아요 상태
     * @return 대댓글 좋아요 토글 응답 DTO
     */
    public CommentResponseDto.RecommentLikeResponse toRecommentLikeResponse(Recomment recomment, boolean isLiked) {
        if (recomment == null) {
            throw new CommentException(GeneralErrorCode.RESOURCE_NOT_FOUND, "recommentId", "존재하지 않는 대댓글입니다.");
        }
        return new CommentResponseDto.RecommentLikeResponse(isLiked, recomment.getLikeCount());
    }
}