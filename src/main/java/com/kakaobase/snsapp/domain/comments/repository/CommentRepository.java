// CommentRepository.java
package com.kakaobase.snsapp.domain.comments.repository;

import com.kakaobase.snsapp.domain.comments.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 댓글 엔티티에 대한 데이터 액세스 객체
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 댓글을 ID로 조회합니다. 삭제된 댓글은 포함하지 않습니다.
     *
     * @param id 댓글 ID
     * @return 댓글 (Optional)
     */
    @Query("SELECT c FROM Comment c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Comment> findByIdAndDeletedAtIsNull(@Param("id") Long id);

    /**
     * 특정 댓글을 ID와 회원 ID로 조회합니다.
     * 댓글의 소유자 확인에 사용됩니다.
     *
     * @param id 댓글 ID
     * @param memberId 회원 ID
     * @return 댓글 (Optional)
     */
    @Query("SELECT c FROM Comment c WHERE c.id = :id AND c.member.id = :memberId AND c.deletedAt IS NULL")
    Optional<Comment> findByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);

    /**
     * 특정 게시글의 댓글을 커서 기반으로 조회합니다.
     * 삭제되지 않은 댓글을 작성순(오래된 순)으로 조회합니다.
     *
     * @param postId 게시글 ID
     * @param cursor 마지막으로 조회한 댓글 ID (첫 페이지에서는 null)
     * @param limit 조회할 댓글 수
     * @return 댓글 목록
     */
    @Query(value = "SELECT c.* FROM comments c " +
            "WHERE c.post_id = :postId " +
            "AND c.deleted_at IS NULL " +
            "AND (:cursor IS NULL OR c.id > :cursor) " + // 부등호 방향 변경 (< 에서 > 로)
            "ORDER BY c.id ASC " +                      // 정렬 방향 변경 (DESC에서 ASC로)
            "LIMIT :limit",
            nativeQuery = true)
    List<Comment> findByPostIdWithCursor(
            @Param("postId") Long postId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit);

    
    //특정 유저가 작성한 댓글 조회
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.member " +
            "JOIN FETCH c.post " +
            "WHERE c.member.id = :memberId " +
            "AND c.deletedAt IS NULL " +
            "AND (:cursor IS NULL OR c.id < :cursor) " +
            "ORDER BY c.createdAt DESC, c.id DESC")
    List<Comment> findByMemberIdWithCursor(
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            Pageable pageable);

    /**
     * 특정 게시글의 댓글 수를 조회합니다.
     * 삭제되지 않은 댓글만 계산합니다.
     *
     * @param postId 게시글 ID
     * @return 댓글 수
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.deletedAt IS NULL")
    long countByPostIdAndDeletedAtIsNull(@Param("postId") Long postId);

}