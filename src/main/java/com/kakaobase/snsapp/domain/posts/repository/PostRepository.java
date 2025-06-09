package com.kakaobase.snsapp.domain.posts.repository;

import com.kakaobase.snsapp.domain.posts.entity.Post;
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
 * 게시글 엔티티에 대한 데이터 액세스 객체
 *
 * <p>게시글에 대한 CRUD 및 다양한 조회 작업을 처리합니다.</p>
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {


    /**
     * 특정 게시글이 특정 사용자가 작성했는지 확인
     */
    boolean existsByIdAndMemberId(Long postId, Long memberId);


    /**
     * 특정 회원이 작성한 게시글 수를 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 회원이 작성한 게시글 수
     */
    long countByMemberId(Long memberId);


    /**
     * 특정 게시판의 최신 게시글을 생성일시와 ID 기준으로 내림차순 정렬하여 조회합니다.
     *
     * @param boardType 게시판 유형
     * @param limit 조회할 게시글 수
     * @return 최신 게시글 목록
     */
    @Query(value = "SELECT p FROM Post p WHERE p.boardType = :boardType AND p.deletedAt IS NULL ORDER BY p.createdAt DESC, p.id DESC LIMIT :limit")
    List<Post> findTopNByBoardTypeOrderByCreatedAtDescIdDesc(
            @Param("boardType") Post.BoardType boardType,
            @Param("limit") int limit);

    /**
     * 특정 게시판 타입에서 cursor 기반으로 게시글 목록을 조회합니다.
     * 최신 게시글부터 내림차순으로 정렬되며, cursor보다 작은 id를 가진 게시글을 조회합니다.
     */
    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.member " +  // JPA 연관관계 활용
            "WHERE p.boardType = :boardType " +
            "AND p.deletedAt IS NULL " +
            "AND (:cursor IS NULL OR p.id < :cursor) " +
            "ORDER BY p.createdAt DESC, p.id DESC")
    List<Post> findByBoardTypeWithCursor(
            @Param("boardType") Post.BoardType boardType,
            @Param("cursor") Long cursor,
            Pageable pageable);

    /**
     * 특정 회원의 게시글을 cursor 기반으로 조회합니다.
     * 최신 게시글부터 내림차순으로 정렬되며, cursor보다 작은 id를 가진 게시글을 조회합니다.
     */
    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.member " +
            "WHERE p.member.id = :memberId " +
            "AND p.deletedAt IS NULL " +
            "AND (:cursor IS NULL OR p.id < :cursor) " +
            "ORDER BY p.createdAt DESC, p.id DESC")
    List<Post> findByMemberIdWithCursor(
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            Pageable pageable);

    /**
     * 특정 유저가 좋아요한 게시글을 조회
     */
    @Query("SELECT p FROM Post p " +
            "WHERE EXISTS (SELECT 1 FROM PostLike pl WHERE pl.id.memberId = :memberId AND pl.id.postId = p.id) " +
            "AND p.deletedAt IS NULL " +
            "AND (:cursor IS NULL OR p.id < :cursor) " +
            "ORDER BY p.createdAt DESC, p.id DESC")
    List<Post> findLikedPostsByMemberIdWithCursor(
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            Pageable pageable);
}