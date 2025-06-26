// RecommentRepository.java
package com.kakaobase.snsapp.domain.comments.repository;

import com.kakaobase.snsapp.domain.comments.entity.Recomment;
import com.kakaobase.snsapp.domain.comments.repository.custom.RecommentCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 대댓글 엔티티에 대한 데이터 액세스 객체
 */
@Repository
public interface RecommentRepository extends JpaRepository<Recomment, Long>, RecommentCustomRepository {

    boolean existsByIdAndMember_Id(Long recommentId, Long memberId);

    /**
     * 특정 댓글과 연관된 모든 대댓글을 소프트 삭제 처리합니다.
     * 댓글이 삭제될 때 해당 댓글의 모든 대댓글을 함께 삭제합니다.
     *
     * @param commentId 삭제할 댓글 ID
     * @return 삭제 처리된 대댓글 수
     */
    @Modifying
    @Query("UPDATE Recomment r SET r.deletedAt = CURRENT_TIMESTAMP WHERE r.comment.id = :commentId AND r.deletedAt IS NULL")
    void deleteByCommentId(@Param("commentId") Long commentId);

    /**
     * 특정 댓글의 대댓글을 모두 조회합니다. (댓글 목록 조회 시 사용)
     * 삭제되지 않은 대댓글만 조회하며, 생성 시간 오름차순으로 정렬합니다.
     *
     * @param commentId 댓글 ID
     * @return 대댓글 목록
     */
    @Query("SELECT r FROM Recomment r WHERE r.comment.id = :commentId AND r.deletedAt IS NULL ORDER BY r.createdAt ASC")
    List<Recomment> findByCommentId(@Param("commentId") Long commentId);
}