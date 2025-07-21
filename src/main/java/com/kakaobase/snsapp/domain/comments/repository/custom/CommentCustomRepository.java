package com.kakaobase.snsapp.domain.comments.repository.custom;

import com.kakaobase.snsapp.domain.comments.dto.CommentResponseDto;

import java.util.List;
import java.util.Optional;

/**
 * 댓글 복잡한 쿼리를 위한 Custom Repository
 */
public interface CommentCustomRepository {

    /**
     * 댓글 상세 정보 조회 (단건)
     *
     * @param commentId 댓글 ID
     * @param memberId 현재 로그인한 회원 ID (null 가능)
     * @return 댓글 상세 정보
     */
    Optional<CommentResponseDto.CommentInfo> findCommentInfoById(Long commentId, Long memberId);

    /**
     * 특정 게시글의 댓글 목록을 커서 기반으로 조회
     *
     * @param postId 게시글 ID
     * @param cursor 마지막으로 조회한 댓글 ID (커서)
     * @param limit 조회할 댓글 수
     * @param memberId 현재 로그인한 회원 ID (null 가능)
     * @return 댓글 목록
     */
    List<CommentResponseDto.CommentInfo> findCommentInfoListWithCursor(
            Long postId,
            Long cursor,
            int limit,
            Long memberId
    );

    /**
     * 특정 회원이 작성한 댓글 목록을 커서 기반으로 조회
     *
     * @param authorMemberId 댓글 작성자 ID
     * @param cursor 마지막으로 조회한 댓글 ID (커서)
     * @param limit 조회할 댓글 수
     * @param currentMemberId 현재 로그인한 회원 ID (null 가능)
     * @return 댓글 목록
     */
    List<CommentResponseDto.CommentInfo> findCommentInfoByMemberWithCursor(
            Long authorMemberId,
            Long cursor,
            int limit,
            Long currentMemberId
    );
}
