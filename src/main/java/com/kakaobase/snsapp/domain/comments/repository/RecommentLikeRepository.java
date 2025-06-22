package com.kakaobase.snsapp.domain.comments.repository;

import com.kakaobase.snsapp.domain.comments.entity.RecommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 대댓글 좋아요 엔티티에 대한 데이터 액세스 객체
 *
 * <p>대댓글 좋아요에 대한 CRUD 및 다양한 조회 작업을 처리합니다.</p>
 */
@Repository
public interface RecommentLikeRepository extends JpaRepository<RecommentLike, RecommentLike.RecommentLikeId> {

    /**
     * 특정 회원이 특정 대댓글에 좋아요를 눌렀는지 확인합니다.
     *
     * @param memberId 회원 ID
     * @param recommentId 대댓글 ID
     * @return 좋아요 정보 (Optional)
     */
    @Query("SELECT rl FROM RecommentLike rl WHERE rl.member.id = :memberId AND rl.recomment.id = :recommentId")
    Optional<RecommentLike> findByMemberIdAndRecommentId(@Param("memberId") Long memberId, @Param("recommentId") Long recommentId);

    /**
     * 특정 회원이 특정 대댓글에 좋아요를 눌렀는지 여부를 확인합니다.
     *
     * @param memberId 회원 ID
     * @param recommentId 대댓글 ID
     * @return 좋아요를 눌렀으면 true, 아니면 false
     */
    @Query("SELECT COUNT(rl) > 0 FROM RecommentLike rl WHERE rl.member.id = :memberId AND rl.recomment.id = :recommentId")
    boolean existsByMemberIdAndRecommentId(@Param("memberId") Long memberId, @Param("recommentId") Long recommentId);

    /**
     * 특정 대댓글의 좋아요 수를 조회합니다.
     *
     * @param recommentId 대댓글 ID
     * @return 좋아요 수
     */
    @Query("SELECT COUNT(rl) FROM RecommentLike rl WHERE rl.recomment.id = :recommentId")
    long countByRecommentId(@Param("recommentId") Long recommentId);


    /**
     * 특정 대댓글의 모든 좋아요를 삭제합니다.
     * 대댓글 삭제 시 관련 좋아요도 함께 삭제하는 데 사용됩니다.
     *
     * @param recommentId 대댓글 ID
     * @return 삭제된 좋아요 수
     */
    @Modifying
    @Query("DELETE FROM RecommentLike rl WHERE rl.recomment.id = :recommentId")
    int deleteByRecommentId(@Param("recommentId") Long recommentId);

    /**
     * 특정 대댓글 목록의 모든 좋아요를 삭제합니다.
     * 여러 대댓글 삭제 시 관련 좋아요도 함께 삭제하는 데 사용됩니다.
     *
     * @param recommentIds 대댓글 ID 목록
     * @return 삭제된 좋아요 수
     */
    @Modifying
    @Query("DELETE FROM RecommentLike rl WHERE rl.recomment.id IN :recommentIds")
    int deleteByRecommentIdIn(@Param("recommentIds") List<Long> recommentIds);

    /**
     * 특정 회원의 모든 좋아요를 삭제합니다.
     * 회원 탈퇴 시 관련 좋아요도 함께 삭제하는 데 사용됩니다.
     *
     * @param memberId 회원 ID
     */
    @Query("DELETE FROM RecommentLike rl WHERE rl.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);


    /**
     * 특정 댓글의 모든 대댓글에 대한 좋아요를 삭제합니다.
     * 댓글 삭제 시 해당 댓글의 모든 대댓글 좋아요도 함께 삭제하는 데 사용됩니다.
     *
     * @param commentId 댓글 ID
     * @return 삭제된 좋아요 수
     */
    @Query("DELETE FROM RecommentLike rl WHERE rl.recomment.comment.id = :commentId")
    int deleteByCommentId(@Param("commentId") Long commentId);
}