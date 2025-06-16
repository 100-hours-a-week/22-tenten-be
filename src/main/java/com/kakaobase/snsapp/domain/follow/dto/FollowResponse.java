package com.kakaobase.snsapp.domain.follow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팔로우 관련 응답 DTO 클래스")
public class FollowResponse {

    @Schema(description = "팔로워 정보")
    public record UserInfo(
            @Schema(description = "회원 ID", example = "10")
            Long id,

            @Schema(description = "회원 닉네임", example = "kevin.hong")
            String nickname,

            @Schema(description = "회원 이름", example = "홍길동")
            String name,

            @Schema(description = "회원 프로필 이미지 URL", example = "https://cdn.service.com/img1.jpg", nullable = true)
            @JsonProperty("image_url")
            String imageUrl
    ){}

}
