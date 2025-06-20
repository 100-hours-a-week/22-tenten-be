// Comment.java
package com.kakaobase.snsapp.domain.comments.entity;

import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.posts.entity.Post;
import com.kakaobase.snsapp.global.common.entity.BaseSoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 댓글 정보를 담는 엔티티
 * <p>
 * 게시글에 달린 댓글 정보를 관리합니다.
 * 대댓글은 별도의 Recomment 엔티티에서 관리됩니다.
 * BaseSoftDeletableEntity를 상속받아 생성/수정/삭제 시간 정보를 관리합니다.
 * </p>
 */
@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_post_created_not_deleted", columnList = "post_id, created_at DESC"),
                @Index(name = "idx_member_created_not_deleted", columnList = "member_id, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseSoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "content", nullable = false, length = 3000)
    private String content;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "recomment_count", nullable = false)
    private Long recommentCount = 0L;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL)
    private List<Recomment> recomments = new ArrayList<>();

    /**
     * 댓글 생성을 위한 생성자
     *
     * @param post 댓글이 작성될 게시글
     * @param member 댓글 작성자
     * @param content 댓글 내용
     */
    @Builder
    public Comment(Post post, Member member, String content) {
        this.post = post;
        this.member = member;
        this.content = content;
    }

    /**
     * 좋아요 수 증가
     */
    public void increaseLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수 감소
     */
    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 대댓글 수 증가
     */
    public void increaseRecommentCount() {
        this.recommentCount++;
    }

    /**
     * 대댓글 수 감소
     */
    public void decreaseRecommentCount() {
        if (this.recommentCount > 0) {
            this.recommentCount--;
        }
    }
}