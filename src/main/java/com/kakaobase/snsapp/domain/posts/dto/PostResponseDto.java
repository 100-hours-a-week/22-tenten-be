package com.kakaobase.snsapp.domain.posts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 게시글 도메인의 응답 DTO를 관리하는 통합 클래스
 */
public class PostResponseDto {

    @Schema(description = "게시글 목록 아이템")
    @Builder
    public record PostDetails(
            @Schema(description = "게시글 ID", example = "123")
            @JsonProperty("id")
            Long id,

            @Schema(description = "작성자 정보")
            MemberResponseDto.UserInfoWithFollowing user,

            @Schema(description = "게시글 내용", example = "이벤트 버블링 헷갈릴 때는...")
            @JsonProperty("content")
            String content,

            @Schema(description = "이미지 URL", example = "https://s3.../event-tip.png")
            @JsonProperty("image_url")
            String imageUrl,

            @Schema(description = "유튜브 URL", example = "https://www.youtube.com/watch?v=abcd1234")
            @JsonProperty("youtube_url")
            String youtubeUrl,

            @Schema(description = "유튜브 요약본", example = "안녕하세요 침착맨입니다~")
            @JsonProperty("youtube_summary")
            String youtubeSummary,

            @Schema(description = "생성 시간", example = "2024-04-23T10:00:00Z")
            @JsonProperty("created_at")
            LocalDateTime createdAt,

            @Schema(description = "좋아요 수", example = "5")
            @JsonProperty("like_count")
            Long likeCount,

            @Schema(description = "댓글 수", example = "2")
            @JsonProperty("comment_count")
            Long commentCount,

            @Schema(description = "본인 게시글 여부", example = "true")
            @JsonProperty("is_mine")
            Boolean isMine,

            @Schema(description = "좋아요 여부", example = "false")
            @JsonProperty("is_liked")
            Boolean isLiked
    ) {
        public PostDetails withStats(Long newLikeCount, Long newCommentCount) {
            return PostDetails.builder()
                    .id(this.id)
                    .user(this.user)
                    .content(this.content)
                    .imageUrl(this.imageUrl)
                    .youtubeUrl(this.youtubeUrl)
                    .youtubeSummary(this.youtubeSummary)
                    .createdAt(this.createdAt)
                    .likeCount(newLikeCount)
                    .commentCount(newCommentCount)
                    .isMine(this.isMine)
                    .isLiked(this.isLiked)
                    .build();
        }
    }
}