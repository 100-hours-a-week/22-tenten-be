package com.kakaobase.snsapp.domain.comments.repository.custom;

import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;

import java.util.List;
import java.util.Optional;

/**
 * 대댓글 복잡한 쿼리를 위한 Custom Repository
 */
public interface RecommentCustomRepository {

    /**
     * 대댓글 상세 정보 조회 (단건)
     *
     * @param recommentId 대댓글 ID
     * @param memberId 현재 로그인한 회원 ID (null 가능)
     * @return 대댓글 상세 정보
     */
    Optional<CommentResponseDto.RecommentInfo> findRecommentInfoById(Long recommentId, Long memberId);

    /**
     * 특정 댓글의 대댓글 목록을 커서 기반으로 조회
     *
     * @param commentId 댓글 ID
     * @param cursor 마지막으로 조회한 대댓글 ID (커서)
     * @param limit 조회할 대댓글 수
     * @param memberId 현재 로그인한 회원 ID (null 가능)
     * @return 대댓글 목록
     */
    List<CommentResponseDto.RecommentInfo> findRecommentInfoListWithCursor(
            Long commentId,
            Long cursor,
            int limit,
            Long memberId
    );

    /**
     * 특정 회원이 작성한 대댓글 목록을 커서 기반으로 조회
     *
     * @param authorMemberId 대댓글 작성자 ID
     * @param cursor 마지막으로 조회한 대댓글 ID (커서)
     * @param limit 조회할 대댓글 수
     * @param currentMemberId 현재 로그인한 회원 ID (null 가능)
     * @return 대댓글 목록
     */
    List<CommentResponseDto.RecommentInfo> findRecommentInfoListByMemberWithCursor(
            Long authorMemberId,
            Long cursor,
            int limit,
            Long currentMemberId
    );

    /**
     * 특정 회원이 좋아요한 대댓글 목록을 커서 기반으로 조회
     *
     * @param likedByMemberId 좋아요한 회원 ID
     * @param cursor 마지막으로 조회한 대댓글 ID (커서)
     * @param limit 조회할 대댓글 수
     * @param currentMemberId 현재 로그인한 회원 ID (null 가능)
     * @return 대댓글 목록
     */
    List<CommentResponseDto.RecommentInfo> findReommentInfoListByLikedWithCursor(
            Long likedByMemberId,
            Long cursor,
            int limit,
            Long currentMemberId
    );
}
