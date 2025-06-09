package com.kakaobase.snsapp.domain.comments.repository;

import com.kakaobase.snsapp.domain.comments.entity.CommentLike;
import com.kakaobase.snsapp.domain.members.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 댓글 좋아요 엔티티에 대한 데이터 액세스 객체
 *
 * <p>댓글 좋아요에 대한 CRUD 및 다양한 조회 작업을 처리합니다.</p>
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLike.CommentLikeId> {

    /**
     * 특정 회원이 특정 댓글에 좋아요를 눌렀는지 확인합니다.
     *
     * @param memberId 회원 ID
     * @param commentId 댓글 ID
     * @return 좋아요 정보 (Optional)
     */
    @Query("SELECT cl FROM CommentLike cl WHERE cl.member.id = :memberId AND cl.comment.id = :commentId")
    Optional<CommentLike> findByMemberIdAndCommentId(@Param("memberId") Long memberId, @Param("commentId") Long commentId);

    /**
     * 특정 회원이 특정 댓글에 좋아요를 눌렀는지 여부를 확인합니다.
     *
     * @param memberId 회원 ID
     * @param commentId 댓글 ID
     * @return 좋아요를 눌렀으면 true, 아니면 false
     */
    @Query("SELECT COUNT(cl) > 0 FROM CommentLike cl WHERE cl.member.id = :memberId AND cl.comment.id = :commentId")
    boolean existsByMemberIdAndCommentId(@Param("memberId") Long memberId, @Param("commentId") Long commentId);

    /**
     * 특정 회원이 좋아요를 누른 댓글 ID 목록을 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 좋아요를 누른 댓글 ID 목록
     */
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.member.id = :memberId")
    List<Long> findCommentIdsByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 회원이 주어진 댓글 목록 중 좋아요를 누른 댓글 ID 목록을 조회합니다.
     * 댓글 목록 조회 시 좋아요 여부를 확인하는 데 사용됩니다.
     *
     * @param memberId 회원 ID
     * @param commentIds 댓글 목록
     * @return 좋아요를 누른 댓글 ID 목록
     */
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.member.id = :memberId AND cl.comment.id IN :commentIds")
    List<Long> findCommentIdsByMemberIdAndCommentIdIn(
            @Param("memberId") Long memberId,
            @Param("commentIds") List<Long> commentIds);

    /**
     * 특정 댓글의 좋아요 수를 조회합니다.
     *
     * @param commentId 댓글 ID
     * @return 좋아요 수
     */
    @Query("SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment.id = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);

    /**
     * 특정 회원이 좋아요를 누른 댓글 목록을 페이지네이션하여 조회합니다.
     *
     * @param memberId 회원 ID
     * @param pageable 페이지네이션 정보
     * @return 좋아요를 누른 댓글 목록 (페이지네이션 적용)
     */
    @Query("SELECT cl FROM CommentLike cl WHERE cl.member.id = :memberId")
    Page<CommentLike> findByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.id = :commentId")
    int deleteByCommentId(@Param("commentId") Long commentId);

    /**
     * 특정 회원의 모든 좋아요를 삭제합니다.
     * 회원 탈퇴 시 관련 좋아요도 함께 삭제하는 데 사용됩니다.
     *
     * @param memberId 회원 ID
     */
    @Query("DELETE FROM CommentLike cl WHERE cl.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 댓글에 좋아요를 누른 회원을 커서 기반으로 조회합니다.
     * 댓글에 좋아요를 누른 회원 중 활성 상태인 회원만 조회합니다.
     * Member Entity를 직접 반환하여 추가 조회 없이 회원 정보를 사용할 수 있습니다.
     *
     * @param commentId 댓글 ID
     * @param lastMemberId 마지막으로 조회한 회원 ID (첫 페이지에서는 null 또는 0)
     * @param limit 조회할 회원 수
     * @return 좋아요를 누른 활성 회원 목록
     */
    @Query(value = "SELECT m.* FROM comment_likes cl " +
            "JOIN members m ON cl.member_id = m.id " +
            "WHERE cl.comment_id = :commentId " +
            "AND m.deleted_at IS NULL " +
            "AND (:lastMemberId IS NULL OR m.id < :lastMemberId) " +
            "ORDER BY m.id DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Member> findMembersByCommentIdWithCursor(
            @Param("commentId") Long commentId,
            @Param("lastMemberId") Long lastMemberId,
            @Param("limit") int limit);

}