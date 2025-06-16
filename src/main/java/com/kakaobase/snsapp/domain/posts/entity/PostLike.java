package com.kakaobase.snsapp.domain.posts.entity;

import com.kakaobase.snsapp.domain.members.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 좋아요 정보를 담는 엔티티 (@EmbeddedId 방식)
 * <p>
 * 게시글에 좋아요를 누른 회원 정보를 관리합니다.
 * BaseCreatedTimeEntity를 상속받아 생성 시간 정보를 관리합니다.
 * @EmbeddedId를 사용한 복합 기본키를 사용합니다.
 * </p>
 */
@Entity
@Table(
        name = "posts_likes",
        indexes = {
                @Index(name = "idx_member_id", columnList = "member_id"),
                @Index(name = "idx_post_id", columnList = "post_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @EmbeddedId
    private PostLikeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;

    /**
     * 좋아요 정보 생성을 위한 생성자
     *
     * @param member 좋아요를 누른 회원
     * @param post 좋아요가 눌린 게시글
     */
    public PostLike(Member member, Post post) {
        this.id = new PostLikeId(member.getId(), post.getId());
        this.member = member;
        this.post = post;
    }

    /**
     * PostLike 엔티티의 임베디드 복합 기본키 클래스
     */
    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class PostLikeId implements java.io.Serializable {

        @Column(name = "member_id")
        private Long memberId;

        @Column(name = "post_id")
        private Long postId;

        public PostLikeId(Long memberId, Long postId) {
            this.memberId = memberId;
            this.postId = postId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PostLikeId that = (PostLikeId) o;

            if (!memberId.equals(that.memberId)) return false;
            return postId.equals(that.postId);
        }

        @Override
        public int hashCode() {
            int result = memberId.hashCode();
            result = 31 * result + postId.hashCode();
            return result;
        }
    }
}