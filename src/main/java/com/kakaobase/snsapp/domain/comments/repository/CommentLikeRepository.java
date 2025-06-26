package com.kakaobase.snsapp.domain.comments.repository;

import com.kakaobase.snsapp.domain.comments.entity.CommentLike;
import com.kakaobase.snsapp.domain.comments.repository.custom.CommentLikeCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 댓글 좋아요 엔티티에 대한 데이터 액세스 객체
 *
 * <p>댓글 좋아요에 대한 CRUD 및 다양한 조회 작업을 처리합니다.</p>
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLike.CommentLikeId>, CommentLikeCustomRepository {

    /**
     * 특정 회원이 특정 댓글에 좋아요를 눌렀는지 확인합니다.
     *
     * @param memberId 회원 ID
     * @param commentId 댓글 ID
     * @return 좋아요 정보 (Optional)
     */
    Optional<CommentLike> findByMemberIdAndCommentId(Long memberId, Long commentId);

    /**
     * 특정 회원이 특정 댓글에 좋아요를 눌렀는지 여부를 확인합니다.
     * JPA 메소드 명으로 변경: existsByMemberIdAndCommentId
     *
     * @param memberId 회원 ID
     * @param commentId 댓글 ID
     * @return 좋아요를 눌렀으면 true, 아니면 false
     */
    boolean existsByMemberIdAndCommentId(Long memberId, Long commentId);

    /**
     * 특정 사용자가 좋아요를 누른 댓글 ID 목록을 일괄 조회
     *
     * @param memberId 사용자 ID
     * @param commentIds 확인할 댓글 ID 목록
     * @return 좋아요를 누른 댓글 ID Set
     */
    @Query("SELECT cl.id.commentId FROM CommentLike cl " +
            "WHERE cl.id.memberId = :memberId " +
            "AND cl.id.commentId IN :commentIds")
    Set<Long> findLikedCommentIdsByMemberAndComments(
            @Param("memberId") Long memberId,
            @Param("commentIds") List<Long> commentIds
    );

    /**
     * 특정 댓글의 모든 좋아요를 삭제
     *
     * @param commentId 댓글 ID
     */
    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);

    /**
     * 특정 댓글목록의 모든 좋아요를 삭제
     *
     * @param commentIds 댓글 ID의 List
     */
    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id IN :commentIds")
    void deleteByCommentIdIn(@Param("commentIds") List<Long> commentIds);
}