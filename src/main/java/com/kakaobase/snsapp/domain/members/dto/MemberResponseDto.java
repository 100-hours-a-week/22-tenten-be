package com.kakaobase.snsapp.domain.members.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 회원 응답 DTO
 */
public class MemberResponseDto {


    @Schema(description = "회원 프로필 수정 DTO")
    public record ProfileImageChange(
            @Schema(description = "프로필 이미지 URL", example = "https://example.com/images/profile.jpg")
            @JsonProperty("image_url")
            String profileImageUrl
    ){}

    //게시물 생성, 좋아요 유저 목록, 팔로잉&팔로워 유저 목록에 사용
    @Schema(description = "회원 정보 DTO")
    @Builder
    public record UserInfo(
            @Schema(description = "회원 ID", example = "10")
            Long id,

            @Schema(description = "회원 이름", example = "홍길동")
            String name,

            @Schema(description = "회원 닉네임", example = "gildong.hong")
            String nickname,

            @Schema(description = "회원 프로필 이미지 URL", example = "https://cdn.service.com/img1.jpg", nullable = true)
            @JsonProperty("image_url")
            String imageUrl
    ){}

    //게시물 목록 조회, 상세조회에 사용
    @Schema(description = "팔로우 필드 추가 회원 정보 DTO")
    @Builder
    public record UserInfoWithFollowing(
            @Schema(description = "회원 ID", example = "10")
            Long id,

            @Schema(description = "회원 닉네임", example = "홍길동")
            String nickname,

            @Schema(description = "회원 프로필 이미지 URL", example = "https://cdn.service.com/img1.jpg", nullable = true)
            @JsonProperty("image_url")
            String imageUrl,

            @Schema(description = "팔로우 여부", example = "false")
            @JsonProperty("is_followed")
            boolean isFollowed
    ) {}

    @Schema(description = "마이페이지 조회 회원 정보 DTO")
    @Builder
    public record Mypage(
            @Schema(description = "회원 ID", example = "10")
            Long id,

            @Schema(description = "회원 이름", example = "홍길동")
            String name,

            @Schema(description = "회원 닉네임", example = "gildong.hond")
            String nickname,

            @Schema(description = "회원 프로필 이미지 URL", example = "https://cdn.service.com/img1.jpg", nullable = true)
            @JsonProperty("image_url")
            String imageUrl,

            @Schema(description = "기수명", example = "PANGYO_1")
            @JsonProperty("class_name")
            String className,

            @Schema(description = "GitHub URL", example = "https://github.com/gildong")
            @JsonProperty("github_url")
            String githubUrl,

            @Schema(description = "회원이 작성한 게시글 수", example = "10")
            @JsonProperty("post_count")
            Long postCount,
            
            @Schema(description = "회원의 팔로워 수", example = "14")
            @JsonProperty("follower_count")
            Long followerCount,

            @Schema(description = "회원의 팔로잉 수", example = "55")
            @JsonProperty("following_count")
            Long followingCount,

            @Schema(description = "본인 여부", example = "true")
            @JsonProperty("is_me")
            boolean isMe,

            @Schema(description = "팔로우 여부", example = "false")
            @JsonProperty("is_followed")
            boolean isFollowed
    ){}
}