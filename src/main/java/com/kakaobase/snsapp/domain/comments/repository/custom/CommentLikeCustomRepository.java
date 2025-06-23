package com.kakaobase.snsapp.domain.comments.repository.custom;

import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import java.util.List;

/**
 * 댓글 좋아요 복잡한 쿼리를 위한 Custom Repository
 */
public interface CommentLikeCustomRepository {

    /**
     * 특정 댓글에 좋아요를 누른 회원을 커서 기반으로 조회
     *
     * @param commentId 댓글 ID
     * @param lastMemberId 마지막으로 조회한 회원 ID (커서)
     * @param limit 조회할 회원 수
     * @return 좋아요를 누른 활성 회원 목록 (UserInfo DTO)
     */
    List<MemberResponseDto.UserInfo> findMembersByCommentIdWithCursor(
            Long commentId,
            Long lastMemberId,
            int limit
    );
}
