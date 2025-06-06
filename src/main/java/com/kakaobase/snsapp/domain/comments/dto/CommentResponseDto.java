package com.kakaobase.snsapp.domain.comments.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 관련 응답 DTO 클래스
 * <p>
 * 댓글 생성, 조회, 삭제, 좋아요 등 댓글 관련 API 응답에 사용되는 DTO들을 포함합니다.
 * </p>
 */
@Schema(description = "댓글 관련 응답 DTO 클래스")
public class CommentResponseDto {


    /**
     * 대댓글 정보 DTO
     */
    @Schema(description = "대댓글 정보")
    public record RecommentInfo(
            @Schema(description = "대댓글 ID", example = "201")
            Long id,

            @Schema(description = "작성자 정보")
            MemberResponseDto.UserInfoWithFollowing user,

            @Schema(description = "대댓글 내용", example = "저도 그렇게 생각해요!")
            String content,

            @Schema(description = "작성 시간", example = "2024-04-25T13:15:00Z")
            LocalDateTime created_at,

            @Schema(description = "좋아요 수", example = "1")
            int like_count,

            @Schema(description = "본인 작성 여부", example = "false")
            boolean is_mine,

            @Schema(description = "좋아요 여부", example = "true")
            boolean is_liked
    ) {}

    /**
     * 댓글 정보 DTO
     */
    @Schema(description = "댓글 정보")
    public record CommentInfo(
            @Schema(description = "댓글 ID", example = "101")
            Long id,

            @Schema(description = "작성자 정보")
            MemberResponseDto.UserInfoWithFollowing user,

            @Schema(description = "댓글 내용", example = "이 게시글 정말 유익하네요!")
            String content,

            @Schema(description = "작성 시간", example = "2024-04-25T13:00:00Z")
            LocalDateTime created_at,

            @Schema(description = "좋아요 수", example = "3")
            int like_count,

            @Schema(description = "대댓글 수", example = "3")
            int recomment_count,

            @Schema(description = "본인 작성 여부", example = "true")
            boolean is_mine,

            @Schema(description = "좋아요 여부", example = "false")
            boolean is_liked
    ) {}

    /**
     * 댓글 생성 응답 DTO
     */
    @Schema(description = "댓글 생성 응답")
    public record CreateCommentResponse(
            @Schema(description = "댓글 ID", example = "456")
            Long id,

            @Schema(description = "작성자 정보")
            MemberResponseDto.UserInfo user,

            @Schema(description = "댓글 내용", example = "이 댓글은 정말 유익하네요!")
            String content,

            @Schema(description = "부모 댓글 ID (대댓글인 경우)", example = "101", nullable = true)
            Long parent_id
    ) {}

    /**
     * 댓글 목록 응답 DTO
     */
    @Schema(description = "댓글 목록 응답")
    public record CommentListResponse(
            @Schema(description = "댓글 목록")
            List<CommentInfo> comments,

            @Schema(description = "다음 페이지 존재 여부", example = "true")
            boolean has_next,

            @Schema(description = "다음 페이지 커서", example = "102", nullable = true)
            Long next_cursor
    ) {}

    /**
     * 대댓글 목록 응답 DTO
     */
    @Schema(description = "대댓글 목록 응답")
    public record RecommentListResponse(
            @Schema(description = "대댓글 목록")
            List<RecommentInfo> recomments,

            @Schema(description = "다음 페이지 존재 여부", example = "true")
            boolean has_next,

            @Schema(description = "다음 페이지 커서", example = "202", nullable = true)
            Long next_cursor
    ) {}


    /**
     * 댓글 상세 조회 응답 DTO
     */
    @Schema(description = "댓글 상세 조회 응답")
    public record CommentDetailResponse(
            @Schema(description = "댓글 상세 정보")
            CommentInfo data
    ) {
    }

    /**
     * 기본 응답 메시지 DTO
     */
    @Schema(description = "기본 응답 메시지")
    public record MessageResponse(
            @Schema(description = "응답 메시지", example = "댓글이 삭제되었습니다.")
            String message
    ) {}

}