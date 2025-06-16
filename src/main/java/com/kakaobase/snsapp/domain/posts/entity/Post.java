package com.kakaobase.snsapp.domain.posts.entity;

import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.global.common.entity.BaseSoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 정보를 담는 엔티티
 * <p>
 * 게시글의 내용, 좋아요 수, 댓글 수 등을 관리합니다.
 * BaseSoftDeletableEntity를 상속받아 생성 시간, 수정 시간, 삭제 시간 정보를 관리합니다.
 * </p>
 */
@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_member_board_deleted_created",
                        columnList = "member_id, board_type, deleted_at, created_at DESC"),
                @Index(name = "idx_board_deleted_created",
                        columnList = "board_type, deleted_at, created_at DESC, id DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE posts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Post extends BaseSoftDeletableEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "board_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BoardType boardType;

    @Column(length = 3000)
    private String content;

    @Column(name = "youtube_url", length = 512)
    private String youtubeUrl;

    @Column(name = "youtube_summary", length = 255)
    private String youtubeSummary;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;


    @Builder
    public Post(Member member, BoardType boardType, String content, String youtubeUrl) {
        this.member = member;
        this.boardType = boardType;
        this.content = content;
        this.youtubeUrl = youtubeUrl;
    }

    public enum BoardType {
        ALL,        // 전체 게시판
        PANGYO_1,   // 판교 1기 게시판
        PANGYO_2,   // 판교 2기 게시판
        JEJU_1,     // 제주 1기 게시판
        JEJU_2,     // 제주 2기 게시판
        JEJU_3      // 제주 3기 게시판
    }

    /**
     * 유튜브 요약 내용을 설정합니다.
     *
     * @param summary 유튜브 영상의 요약 내용
     */
    public void setYoutubeSummary(String summary) {
        this.youtubeSummary = summary;
    }


    /**
     * 좋아요 수를 증가시킵니다.
     */
    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void updateYoutubeSummary(String summary) {
        this.youtubeSummary = summary;
    }

    /**
     * 좋아요 수를 감소시킵니다.
     */
    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 댓글 수를 증가시킵니다.
     */
    public void increaseCommentCount() {
        this.commentCount++;
    }

    /**
     * 댓글 수를 감소시킵니다.
     */
    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }
}