package com.kakaobase.snsapp.domain.comments.entity;

import com.kakaobase.snsapp.domain.members.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 좋아요 정보를 담는 엔티티 (@EmbeddedId 객체 그래프 형태)
 * <p>
 * 댓글에 좋아요를 누른 회원 정보를 관리합니다.
 * @EmbeddedId를 사용한 복합 기본키를 사용하며, Member와 Comment 엔티티를 직접 참조합니다.
 * </p>
 */
@Entity
@Table(
        name = "comment_likes",
        indexes = {
                @Index(name = "idx_member_id", columnList = "member_id"),
                @Index(name = "idx_comment_id", columnList = "comment_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike {

    @EmbeddedId
    private CommentLikeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", insertable = false, updatable = false)
    private Comment comment;

    /**
     * 좋아요 정보 생성을 위한 생성자
     *
     * @param member 좋아요를 누른 회원
     * @param comment 좋아요가 눌린 댓글
     */
    public CommentLike(Member member, Comment comment) {
        this.id = new CommentLikeId(member.getId(), comment.getId());
        this.member = member;
        this.comment = comment;
    }

    /**
     * CommentLike 엔티티의 임베디드 복합 기본키 클래스
     */
    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CommentLikeId implements java.io.Serializable {

        @Column(name = "member_id")
        private Long memberId;

        @Column(name = "comment_id")
        private Long commentId;

        public CommentLikeId(Long memberId, Long commentId) {
            this.memberId = memberId;
            this.commentId = commentId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CommentLikeId that = (CommentLikeId) o;

            if (!memberId.equals(that.memberId)) return false;
            return commentId.equals(that.commentId);
        }

        @Override
        public int hashCode() {
            int result = memberId.hashCode();
            result = 31 * result + commentId.hashCode();
            return result;
        }
    }

}