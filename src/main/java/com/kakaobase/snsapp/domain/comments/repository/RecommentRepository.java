// RecommentRepository.java
package com.kakaobase.snsapp.domain.comments.repository;

import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 대댓글 엔티티에 대한 데이터 액세스 객체
 */
@Repository
public interface RecommentRepository extends JpaRepository<Recomment, Long> {

    /**
     * 특정 대댓글을 ID로 조회합니다. 삭제된 대댓글은 포함하지 않습니다.
     *
     * @param id 대댓글 ID
     * @return 대댓글 (Optional)
     */
    @Query("SELECT r FROM Recomment r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<Recomment> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * 특정 대댓글을 ID와 회원 ID로 조회합니다.
     * 대댓글의 소유자 확인에 사용됩니다.
     *
     * @param id 대댓글 ID
     * @param memberId 회원 ID
     * @return 대댓글 (Optional)
     */
    @Query("SELECT r FROM Recomment r WHERE r.id = :id AND r.member.id = :memberId AND r.deletedAt IS NULL")
    Optional<Recomment> findByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);

    /**
     * 특정 댓글의 대댓글을 모두 조회합니다. (댓글 목록 조회 시 사용)
     * 삭제되지 않은 대댓글만 조회하며, 생성 시간 오름차순으로 정렬합니다.
     *
     * @param commentId 댓글 ID
     * @return 대댓글 목록
     */
    @Query("SELECT r FROM Recomment r WHERE r.comment.id = :commentId AND r.deletedAt IS NULL ORDER BY r.createdAt ASC")
    List<Recomment> findByCommentId(@Param("commentId") Long commentId);

    /**
     * 특정 댓글의 모든 대댓글을 조회합니다. (삭제된 것 포함)
     * 댓글 삭제 시 연관된 대댓글 삭제를 위해 사용됩니다.
     *
     * @param commentId 댓글 ID
     * @return 대댓글 목록
     */
    @Query("SELECT r FROM Recomment r WHERE r.comment.id = :commentId")
    List<Recomment> findAllByCommentId(@Param("commentId") Long commentId);

    /**
     * 특정 댓글의 대댓글을 커서 기반으로 조회합니다. (대댓글 목록 API용)
     * 삭제되지 않은 대댓글만 조회합니다.
     *
     * @param commentId 댓글 ID
     * @param cursor 마지막으로 조회한 대댓글 ID (첫 페이지에서는 null)
     * @param limit 조회할 대댓글 수
     * @return 대댓글 목록
     */
    @Query(value = "SELECT r.* FROM recomments r " +
            "WHERE r.comment_id = :commentId " +
            "AND r.deleted_at IS NULL " +
            "AND (:cursor IS NULL OR r.id > :cursor) " +
            "ORDER BY r.id ASC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Recomment> findByRecommentIdWithCursor(
            @Param("commentId") Long commentId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);

    /**
     * 특정 댓글의 대댓글 수를 조회합니다.
     * 삭제되지 않은 대댓글만 계산합니다.
     *
     * @param commentId 댓글 ID
     * @return 대댓글 수
     */
    @Query("SELECT COUNT(r) FROM Recomment r WHERE r.comment.id = :commentId AND r.deletedAt IS NULL")
    long countByCommentIdAndDeletedAtIsNull(@Param("commentId") Long commentId);

    /**
     * 특정 대댓글들의 좋아요 상태를 한번에 조회합니다.
     * 대댓글 좋아요 테이블을 기반으로 좋아요 여부를 확인합니다.
     *
     * @param recommentIds 대댓글 ID 목록
     * @param memberId 회원 ID
     * @return 회원이 좋아요 한 대댓글 ID 목록
     */
    @Query(value = "SELECT rl.recomment_id FROM recomment_likes rl " +
            "WHERE rl.recomment_id IN :recommentIds " +
            "AND rl.member_id = :memberId",
            nativeQuery = true)
    List<Long> findLikedRecommentIds(
            @Param("recommentIds") List<Long> recommentIds,
            @Param("memberId") Long memberId);
}