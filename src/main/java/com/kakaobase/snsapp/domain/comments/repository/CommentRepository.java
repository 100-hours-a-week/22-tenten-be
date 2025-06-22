// CommentRepository.java
package com.kakaobase.snsapp.domain.comments.repository;

import com.kakaobase.snsapp.domain.comments.entity.Comment;
import com.kakaobase.snsapp.domain.comments.repository.custom.CommentCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 댓글 엔티티에 대한 데이터 액세스 객체
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, CommentCustomRepository {

    //특정 회원이 해당 게시글을 작성했는 지 확인
    //삭제시 권한확인용
    boolean existsByIdAndMember_Id(Long id, Long memberId);

    /**
     * 특정 게시글과 연관된 모든 댓글을 소프트 삭제 처리합니다.
     * 게시글이 삭제될 때 해당 게시글의 모든 댓글을 함께 삭제합니다.
     *
     * @param postId 삭제할 게시글 ID
     */
    @Modifying
    @Query("UPDATE Comment c SET c.deletedAt = CURRENT_TIMESTAMP WHERE c.post.id = :postId AND c.deletedAt IS NULL")
    void deleteByPostId(@Param("postId") Long postId);

    /**
     * 특정 댓글의 RecommentCount수를 1증가시키는 벌크 업데이트 메서드
     */
    @Modifying
    @Query("UPDATE Comment c SET c.recommentCount = c.recommentCount + 1 WHERE c.id = :commentId")
    int incrementRecommentCount(@Param("commentId") Long commentId);
}