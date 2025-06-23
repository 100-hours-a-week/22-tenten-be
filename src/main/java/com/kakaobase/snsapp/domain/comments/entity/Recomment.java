package com.kakaobase.snsapp.domain.comments.entity;

import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.global.common.entity.BaseSoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

/**
 * 대댓글 정보를 담는 엔티티
 * <p>
 * 댓글에 달린 대댓글 정보를 관리합니다.
 * BaseSoftDeletableEntity를 상속받아 생성/수정/삭제 시간 정보를 관리합니다.
 * </p>
 */
@Entity
@Table(
        name = "recomments",
        indexes = {
                @Index(name = "idx_comment_created_not_deleted", columnList = "comment_id, created_at DESC"),
                @Index(name = "idx_member_created_not_deleted", columnList = "member_id, created_at DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Where(clause = "deleted_at IS NULL")
public class Recomment extends BaseSoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "content", nullable = false, length = 3000)
    private String content;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    /**
     * 대댓글 생성을 위한 생성자
     *
     * @param comment 대댓글이 작성될 댓글
     * @param member 대댓글 작성자
     * @param content 대댓글 내용
     */
    @Builder
    public Recomment(Comment comment, Member member, String content) {
        this.comment = comment;
        this.member = member;
        this.content = content;
    }

    /**
     * 대댓글 내용 수정
     *
     * @param content 수정할 대댓글 내용
     */
    public void updateContent(String content) {
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
}