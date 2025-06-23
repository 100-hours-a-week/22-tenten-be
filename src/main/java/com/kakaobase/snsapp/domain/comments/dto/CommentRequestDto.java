package com.kakaobase.snsapp.domain.comments.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 댓글 관련 요청 DTO 클래스
 * <p>
 * 댓글 생성, 수정, 조회, 좋아요 등 댓글 관련 API 요청에 사용되는 DTO들을 포함합니다.
 * </p>
 */
@Schema(description = "댓글 관련 요청 DTO 클래스")
public class CommentRequestDto {

    /**
     * 댓글 생성 요청 DTO
     */
    @Schema(description = "댓글 작성 요청 DTO")
    public record CreateCommentRequest(
            @Schema(description = "댓글 내용", example = "이 댓글은 정말 유익하네요!", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "댓글 내용은 공백일 수 없습니다.")
            @Size(min = 1, max = 2000, message = "댓글은 최대 2000자까지 작성할 수 있습니다.")
            String content,

            @Schema(description = "대댓글일 경우 부모 댓글 ID, 없으면 일반 댓글", example = "101", nullable = true)
            Long parent_id
    ) {}

}