package com.kakaobase.snsapp.domain.comments.entity;

import com.kakaobase.snsapp.domain.members.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대댓글 좋아요 정보를 담는 엔티티 (@EmbeddedId 객체 그래프 형태)
 * <p>
 * 대댓글에 좋아요를 누른 회원 정보를 관리합니다.
 * @EmbeddedId를 사용한 복합 기본키를 사용하며, Member와 Recomment 엔티티를 직접 참조합니다.
 * </p>
 */
@Entity
@Table(
        name = "recomment_likes",
        indexes = {
                @Index(name = "idx_member_id", columnList = "member_id"),
                @Index(name = "idx_recomment_id", columnList = "recomment_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommentLike {

    @EmbeddedId
    private RecommentLikeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recomment_id", insertable = false, updatable = false)
    private Recomment recomment;

    /**
     * 좋아요 정보 생성을 위한 생성자
     *
     * @param member 좋아요를 누른 회원
     * @param recomment 좋아요가 눌린 대댓글
     */
    public RecommentLike(Member member, Recomment recomment) {
        this.id = new RecommentLikeId(member.getId(), recomment.getId());
        this.member = member;
        this.recomment = recomment;
    }

    /**
     * RecommentLike 엔티티의 임베디드 복합 기본키 클래스
     */
    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class RecommentLikeId implements java.io.Serializable {

        @Column(name = "member_id")
        private Long memberId;

        @Column(name = "recomment_id")
        private Long recommentId;

        public RecommentLikeId(Long memberId, Long recommentId) {
            this.memberId = memberId;
            this.recommentId = recommentId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RecommentLikeId that = (RecommentLikeId) o;

            if (!memberId.equals(that.memberId)) return false;
            return recommentId.equals(that.recommentId);
        }

        @Override
        public int hashCode() {
            int result = memberId.hashCode();
            result = 31 * result + recommentId.hashCode();
            return result;
        }
    }
}