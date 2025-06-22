package com.kakaobase.snsapp.domain.posts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * AI 봇 관련 요청/응답 DTO 모음
 *
 * <p>AI 서버와의 통신에 사용되는 DTO record들을 포함합니다.</p>
 */
public class BotRequestDto {

    /**
     * AI 서버에 게시글 생성 요청 DTO
     *
     * @param boardType 게시판 타입 (PANGYO_2 등)
     * @param posts 최근 5개 게시글 정보
     */
    @Schema(description = "AI 서버 게시글 생성 요청")
    public record CreatePostRequest(
            @Schema(description = "게시판 타입", example = "PANGYO_2", required = true)
            @JsonProperty("board_type")
            String boardType,

            @Schema(description = "최근 5개 게시글 목록", required = true)
            List<PostDto> posts
    ) { }

    /**
     * 게시글 정보 DTO
     *
     * @param user 작성자 정보
     * @param createdAt 게시글 작성 시각 (ISO 8601 형식)
     * @param content 게시글 내용
     */
    @Builder
    @Schema(description = "게시글 정보")
    public record PostDto(
            @Schema(description = "게시글 작성자 정보", required = true)
            UserDto user,

            @Schema(description = "게시글 작성 시각", example = "2025-04-27T10:30:32.311141Z", required = true)
            @JsonProperty("created_at")
            String createdAt,

            @Schema(description = "게시글 내용", example = "좋은아침입니당", required = true)
            String content
    ) {
    }

    /**
     * 사용자 정보 DTO
     *
     * @param nickname 사용자 닉네임
     * @param className 사용자 기수
     */
    @Schema(description = "사용자 정보")
    public record UserDto(
            @Schema(description = "사용자 닉네임", example = "hazel.kim", required = true)
            String nickname,

            @Schema(description = "사용자 기수", example = "PANGYO_2", required = true)
            @JsonProperty("class_name")
            String className
    ) {
    }

    /**
     * AI 서버로부터의 게시글 생성 응답 DTO
     *
     * @param message 응답 메시지
     * @param data 응답 데이터
     */
    @Schema(description = "AI 서버로부터의 게시글 생성 응답")
    public record AiPostResponse(
            @Schema(description = "응답 메시지", example = "소셜봇이 게시물을 작성했습니다.")
            String message,

            @Schema(description = "응답 데이터")
            AiResponseData data
    ) {
    }

    /**
     * AI 서버 응답 데이터 DTO
     *
     * @param boardType 게시판 타입
     * @param user 봇 사용자 정보
     * @param content 생성된 게시글 내용
     */
    @Schema(description = "AI 서버 응답 데이터")
    public record AiResponseData(
            @Schema(description = "게시판 타입", example = "PANGYO_2")
            @JsonProperty("board_type")
            String boardType,

            @Schema(description = "봇 사용자 정보")
            UserDto user,

            @Schema(description = "생성된 게시글 내용", example = "ㅎㅎ 다들 아침 인사도 해주시고...")
            String content
    ) {
    }

    /**
     * AI 서버 에러 응답 DTO
     *
     * @param error 에러 코드
     * @param message 에러 메시지
     * @param field 에러 필드 (optional)
     */
    @Schema(description = "AI 서버 에러 응답")
    public record ErrorResponse(
            @Schema(description = "에러 코드", example = "invalid_query_parameter")
            String error,

            @Schema(description = "에러 메시지", example = "전달받은 게시글이 5개 미만입니다.")
            String message,

            @Schema(description = "에러 필드 목록", example = "[\"body\", \"board_type\"]", required = false)
            List<String> field
    ) {
    }
}