package com.kakaobase.snsapp.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 인증 관련 응답 DTO 모음 클래스입니다.
 */
public class AuthResponseDto {

    /**
     * 로그인 Dto
     * - RefreshToken은 쿠키로 전달됨
     */
    @Schema(description = "로그인 또는 토큰 재발급 성공 응답 DTO")
    @Builder
    public record LoginResponse(

            @Schema(description = "로그인 유저의 memberId", example = "12...")
            @JsonProperty("member_id")
            Long memberId,

            @Schema(description = "로그인 유저의 닉네임", example = "rick.lee")
            @JsonProperty("nickname")
            String nickname,

            @Schema(description = "사용자의 기수명", example = "PANGYO_2")
            @JsonProperty("class_name")
            String className,

            @Schema(description = "유저의 프로필 이미지")
            @JsonProperty("image_url")
            String imageUrl,

            @Schema(description = "AccessToken (JWT 형식)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
            @JsonProperty("access_token")
            String accessToken

    ) {}

    /**
     * 액세스 토큰 응답 DTO
     *
     */
    @Schema(description = "로그인 또는 토큰 재발급 성공 응답 DTO")
    public record TokenResponse(

            @Schema(description = "AccessToken (JWT 형식)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6...")
            String access_token

    ) {}

    /**
     * 인증 실패 공통 응답 DTO
     */
    @Schema(description = "인증 실패 응답 DTO")
    public record Failure(
            @Schema(description = "에러 코드", example = "invalid_password")
            String error,

            @Schema(description = "에러 메시지", example = "비밀번호가 일치하지 않습니다.")
            String message,

            @Schema(description = "문제가 발생한 필드", example = "password")
            String field
    ) {}

    /**
     * RefreshToken 누락 에러 응답 DTO
     */
    @Schema(description = "RefreshToken 누락 실패 응답 DTO")
    public record RefreshTokenMissing(
            @Schema(description = "에러 코드", example = "refresh_token_missing")
            String error,

            @Schema(description = "에러 메시지", example = "RefreshToken이 쿠키에 포함되어 있지 않습니다.")
            String message,

            @Schema(description = "에러 필드", example = "refreshToken")
            String field
    ) {}

    /**
     * RefreshToken 무효 응답 DTO
     */
    @Schema(description = "RefreshToken 무효 실패 응답 DTO")
    public record RefreshTokenInvalid(
            @Schema(description = "에러 코드", example = "refresh_token_invalid")
            String error,

            @Schema(description = "에러 메시지", example = "RefreshToken이 만료되었거나 유효하지 않습니다.")
            String message,

            @Schema(description = "에러 필드", example = "refreshToken")
            String field
    ) {}
}
