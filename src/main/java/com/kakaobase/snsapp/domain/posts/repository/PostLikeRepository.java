package com.kakaobase.snsapp.domain.posts.repository;

import com.kakaobase.snsapp.domain.posts.entity.PostLike;
import com.kakaobase.snsapp.domain.posts.repository.custom.PostLikeCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 게시글 좋아요 엔티티에 대한 기본 데이터 액세스 객체
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.PostLikeId>, PostLikeCustomRepository {

    /**
     * 특정 회원이 특정 게시글에 좋아요를 눌렀는지 확인 - JPA 메서드명
     */
    Optional<PostLike> findByMemberIdAndPostId(Long memberId, Long postId);

    /**
     * 특정 회원이 특정 게시글에 좋아요를 눌렀는지 여부 확인 - JPA 메서드명
     */
    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    /**
     * 특정 회원의 모든 좋아요 삭제 - JPA 메서드명
     */
    @Modifying
    void deleteByMemberId(Long memberId);
}