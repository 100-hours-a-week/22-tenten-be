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
     * 특정 회원이 작성한 게시글을 최신순으로 조회합니다.
     *
     * @param memberId 회원 ID
     * @param pageable 페이지네이션 정보
     * @return 회원이 작성한 게시글 목록 (페이지네이션 적용)
     */
    Page<Post> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 특정 회원이 작성한 특정 게시판의 게시글을 최신순으로 조회합니다.
     *
     * @param memberId 회원 ID
     * @param boardType 게시판 타입
     * @param pageable 페이지네이션 정보
     * @return 회원이 작성한 특정 게시판의 게시글 목록 (페이지네이션 적용)
     */
    Page<Post> findByMemberIdAndBoardTypeOrderByCreatedAtDesc(
            Long memberId, Post.BoardType boardType, Pageable pageable);

    /**
     * 특정 게시판의 게시글을 최신순으로 조회합니다.
     *
     * @param boardType 게시판 타입
     * @param pageable 페이지네이션 정보
     * @return 특정 게시판의 게시글 목록 (페이지네이션 적용)
     */
    Page<Post> findByBoardTypeOrderByCreatedAtDesc(Post.BoardType boardType, Pageable pageable);


    /**
     * 특정 게시글이 특정 사용자가 작성했는지 확인
     */
    boolean existsByIdAndMemberId(Long postId, Long memberId);

    /**
     * 특정 회원이 좋아요를 누른 게시글 목록을 게시글 ID 기준으로 커서 기반 페이징으로 조회합니다.
     * 게시글 ID 내림차순으로 조회하므로 대략적인 최신순으로 볼 수 있습니다.
     *
     * @param memberId 회원 ID
     * @param lastPostId 마지막으로 조회한 게시글 ID (첫 페이지에서는 null 또는 Long.MAX_VALUE)
     * @param limit 조회할 게시글 수
     * @return 회원이 좋아요를 누른 게시글 목록
     */
    @Query("SELECT p FROM Post p " +
            "JOIN PostLike pl ON p.id = pl.postId " +
            "WHERE pl.memberId = :memberId " +
            "AND p.deletedAt IS NULL " +
            "AND (:lastPostId IS NULL OR p.id < :lastPostId) " +
            "ORDER BY p.id DESC")
    List<Post> findPostsLikedByMemberWithCursor(
            @Param("memberId") Long memberId,
            @Param("lastPostId") Long lastPostId,
            @Param("limit") int limit);
    /**
     * 게시글 좋아요 수를 증가시킵니다.
     *
     * @param postId 게시글 ID
     */
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void increaseLikeCount(@Param("postId") Long postId);

    /**
     * 게시글 좋아요 수를 감소시킵니다.
     *
     * @param postId 게시글 ID
     */
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :postId")
    void decreaseLikeCount(@Param("postId") Long postId);

    /**
     * 게시글 댓글 수를 증가시킵니다.
     *
     * @param postId 게시글 ID
     */
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void increaseCommentCount(@Param("postId") Long postId);

    /**
     * 게시글 댓글 수를 감소시킵니다.
     *
     * @param postId 게시글 ID
     */
    @Modifying
    @Query("UPDATE Post p SET p.commentCount = CASE WHEN p.commentCount > 0 THEN p.commentCount - 1 ELSE 0 END WHERE p.id = :postId")
    void decreaseCommentCount(@Param("postId") Long postId);

    /**
     * 제목이나 내용에 특정 키워드가 포함된 게시글을 검색합니다.
     *
     * @param keyword 검색 키워드
     * @param pageable 페이지네이션 정보
     * @return 검색 결과 게시글 목록 (페이지네이션 적용)
     */
    @Query("SELECT p FROM Post p WHERE p.content LIKE %:keyword%")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 특정 게시판에서 제목이나 내용에 특정 키워드가 포함된 게시글을 검색합니다.
     *
     * @param boardType 게시판 타입
     * @param keyword 검색 키워드
     * @param pageable 페이지네이션 정보
     * @return 검색 결과 게시글 목록 (페이지네이션 적용)
     */
    @Query("SELECT p FROM Post p WHERE p.boardType = :boardType AND p.content LIKE %:keyword%")
    Page<Post> searchByBoardTypeAndKeyword(
            @Param("boardType") Post.BoardType boardType,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 특정 게시판의 최신 게시글을 생성일시와 ID 기준으로 내림차순 정렬하여 조회합니다.
     *
     * @param boardType 게시판 유형
     * @param limit 조회할 게시글 수
     * @return 최신 게시글 목록
     */
    @Query(value = "SELECT p FROM Post p WHERE p.boardType = :boardType AND p.deletedAt IS NULL ORDER BY p.createdAt DESC, p.id DESC LIMIT :limit")
    List<Post> findTopNByBoardTypeOrderByCreatedAtDescIdDesc(@Param("boardType") Post.BoardType boardType, @Param("limit") int limit);

    /**
     * 특정 게시판에서 주어진 ID보다 작은 게시글을 ID 기준으로 내림차순 정렬하여 조회합니다.
     *
     * @param boardType 게시판 유형
     * @param cursor 마지막으로 조회한 게시글 ID
     * @param limit 조회할 게시글 수
     * @return 다음 페이지 게시글 목록
     */
    @Query(value = "SELECT p FROM Post p WHERE p.boardType = :boardType AND p.id < :cursor AND p.deletedAt IS NULL ORDER BY p.id DESC LIMIT :limit")
    List<Post> findByBoardTypeAndIdLessThanOrderByIdDesc(@Param("boardType") Post.BoardType boardType, @Param("cursor") Long cursor, @Param("limit") int limit);
}