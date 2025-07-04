package com.kakaobase.snsapp.domain.members.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 회원 요청 DTO
 */
public class MemberRequestDto {

    @Schema(description = "회원가입 요청 DTO")
    public record SignUp(
            @Schema(description = "이메일", example = "example@domain.com")
            @NotBlank(message = "이메일은 필수 입력값입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            @JsonProperty("email")
            String email,

            @Schema(description = "비밀번호", example = "Test1234!")
            @NotBlank(message = "비밀번호는 필수 입력값입니다.")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*\\-_=+<>,\\.\\;:\\\"'\\{\\}\\[\\]/\\?])[A-Za-z\\d!@#$%^&*\\-_=+<>,\\.\\;:\\\"'\\{\\}\\[\\]/\\?]{8,20}$",
                    message = "비밀번호는 8~20자리, 영문, 숫자, 특수문자를 포함해야 합니다"
            )
            @JsonProperty("password")
            String password,

            @Schema(description = "이름", example = "홍길동")
            @NotBlank(message = "이름은 필수 입력값입니다.")
            @Size(min = 2, max = 20, message = "이름은 2~20자리여야 합니다.")
            @JsonProperty("name")
            String name,

            @Schema(description = "닉네임", example = "gildong")
            @NotBlank(message = "닉네임은 필수 입력값입니다.")
            @Pattern(regexp = "^[A-Za-z0-9가-힣.]{2,20}$",
                    message = "닉네임은 2~20자리, 영문자/숫자/한글과 온점(.)만 포함할 수 있습니다.")
            @JsonProperty("nickname")
            String nickname,

            @Schema(description = "기수명", example = "PANGYO_1", allowableValues = {"PANGYO_1", "PANGYO_2", "JEJU_1", "JEJU_2", "JEJU_3"})
            @NotNull(message = "기수는 필수 입력값입니다.")
            String className,

            @Schema(description = "GitHub 프로필 URL", example = "https://github.com/gildong")
            @Pattern(regexp = "^(https://github\\.com/)[A-Za-z0-9_-]+(/)?.*$",
                    message = "GitHub URL 형식이 올바르지 않습니다. https://github.com/{username} 형식이어야 합니다.")
            String githubUrl
    ) {}

        @Schema(description = "비밀번호 수정 요청 DTO")
        public record PasswordChange(
                @Schema(description = "이메일", example = "example@domain.com")
                @NotBlank(message = "이메일은 필수 입력값입니다.")
                @Email(message = "이메일 형식이 올바르지 않습니다.")
                @JsonProperty("email")
                String email,

                @Schema(description = "비밀번호", example = "Test1234!")
                @NotBlank(message = "비밀번호는 필수 입력값입니다.")
                @Pattern(
                        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*\\-_=+<>,\\.\\;:\\\"'\\{\\}\\[\\]/\\?])[A-Za-z\\d!@#$%^&*\\-_=+<>,\\.\\;:\\\"'\\{\\}\\[\\]/\\?]{8,20}$",
                        message = "비밀번호는 8~20자리, 영문, 숫자, 특수문자를 포함해야 합니다"
                )
                @JsonProperty("new_password")
                String NewPassword
        ) {}

        @Schema(description = "GithubUrl 수정 요청 DTO")
        public record GithubUrlChange(

                @Schema(description = "수정할 GitHub URL", example = "https://github.com/gildong")
                @Pattern(regexp = "^(https://github\\.com/)[A-Za-z0-9_-]+(/)?.*$",
                        message = "GitHub URL 형식이 올바르지 않습니다. https://github.com/{username} 형식이어야 합니다.")
                @JsonProperty("github_url")
                String githubUrl
        ) {}

        @Schema(description = "프로필 이미지 수정 요청 DTO")
        public record ProfileImageChange(

                @Schema(description = "수정할 프로필 이미지 URL", example = "https://s3.amazonaws.com/bucket/uploads/dev.jpg")
                @Pattern(
                        regexp = "^https://[a-zA-Z0-9.-]+\\.s3\\.[a-zA-Z0-9-]+\\.amazonaws\\.com/.+$",
                        message = "올바른 파일 URL 형식이 아닙니다."
                )
                @JsonProperty("image_url")
                String imageUrl
        ) {}
}