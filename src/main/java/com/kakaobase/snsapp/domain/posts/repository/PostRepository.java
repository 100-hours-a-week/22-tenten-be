package com.kakaobase.snsapp.domain.posts.repository;

import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.domain.posts.repository.custom.PostCustomRepository;
import com.kakaobase.snsapp.domain.posts.util.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 게시글 엔티티에 대한 데이터 액세스 객체
 *
 * <p>게시글에 대한 CRUD 및 다양한 조회 작업을 처리합니다.</p>
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostCustomRepository {


    /**
     * 특정 게시글이 특정 사용자가 작성했는지 확인
     */
    boolean existsByIdAndMemberId(Long postId, Long memberId);


    /**
     * 특정 회원이 작성한 게시글 수를 조회합니다.
     */
    long countByMemberId(Long memberId);

    List<Post> findTop10ByBoardTypeOrderByCreatedAtDescIdDesc(BoardType boardType);
}