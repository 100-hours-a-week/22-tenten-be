package com.kakaobase.snsapp.domain.comments.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대댓글 좋아요 정보를 담는 엔티티
 * <p>
 * 대댓글에 좋아요를 누른 회원 정보를 관리합니다.
 * 복합 기본키를 사용합니다 (회원 ID + 대댓글 ID).
 * </p>
 */
@Entity
@Table(
        name = "recomment_likes",
        indexes = {
                @Index(name = "idx_member_id", columnList = "member_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(RecommentLike.RecommentLikeId.class)
public class RecommentLike {

    @Id
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Id
    @Column(name = "recomment_id", nullable = false)
    private Long recommentId;

    /**
     * 좋아요 정보 생성을 위한 생성자
     *
     * @param memberId 좋아요를 누른 회원 ID
     * @param recommentId 좋아요가 눌린 대댓글 ID
     */
    public RecommentLike(Long memberId, Long recommentId) {
        this.memberId = memberId;
        this.recommentId = recommentId;
    }

    /**
     * RecommentLike 엔티티의 복합 기본키 클래스
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class RecommentLikeId implements java.io.Serializable {
        private Long memberId;
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