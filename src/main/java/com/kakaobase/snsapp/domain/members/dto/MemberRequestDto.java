package com.kakaobase.snsapp.domain.members.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * 회원 요청 DTO
 */
public class MemberRequestDto {

    @Schema(description = "회원가입 요청 DTO")
    public record SignUp(
            @Schema(description = "이메일", example = "example@domain.com")
            @NotBlank(message = "이메일은 필수 입력값입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @Schema(description = "비밀번호", example = "Test1234!")
            @NotBlank(message = "비밀번호는 필수 입력값입니다.")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*\\-_=+<>,\\.\\;:\\\"'\\{\\}\\[\\]/\\?])[A-Za-z\\d!@#$%^&*\\-_=+<>,\\.\\;:\\\"'\\{\\}\\[\\]/\\?]{8,20}$",
                    message = "비밀번호는 8~20자리, 영문, 숫자, 특수문자를 포함해야 합니다"
            )
            String password,

            @Schema(description = "이름", example = "홍길동")
            @NotBlank(message = "이름은 필수 입력값입니다.")
            @Size(min = 2, max = 20, message = "이름은 2~20자리여야 합니다.")
            String name,

            @Schema(description = "닉네임", example = "gildong")
            @NotBlank(message = "닉네임은 필수 입력값입니다.")
            @Pattern(regexp = "^[A-Za-z0-9가-힣.]{2,20}$",
                    message = "닉네임은 2~20자리, 영문자/숫자/한글과 온점(.)만 포함할 수 있습니다.")
            String nickname,

            @Schema(description = "기수명", example = "PANGYO_1", allowableValues = {"PANGYO_1", "PANGYO_2", "JEJU_1", "JEJU_2", "JEJU_3"})
            @NotNull(message = "기수는 필수 입력값입니다.")
            String className,

            @Schema(description = "GitHub 프로필 URL", example = "https://github.com/gildong")
            @Pattern(regexp = "^(https://github\\.com/)[A-Za-z0-9_-]+(/)?.*$",
                    message = "GitHub URL 형식이 올바르지 않습니다. https://github.com/{username} 형식이어야 합니다.")
            String githubUrl
    ) {}

    @Schema(description = "이메일 인증 코드 요청 DTO")
    public record EmailVerificationRequest(
            @Schema(description = "이메일", example = "test@domain.com")
            @NotBlank(message = "이메일은 필수 입력값입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @Schema(description = "인증 목적", example = "sign-up", allowableValues = {"sign-up", "password-reset"})
            @NotNull(message = "인증 목적은 필수 입력값입니다.")
            @Pattern(regexp = "^(sign-up|password-reset)$", message = "인증 목적은 sign-up 또는 password-reset만 가능합니다.")
            String purpose
    ) {}

    @Schema(description = "이메일 인증 코드 확인 DTO")
    public record EmailVerification(
            @Schema(description = "이메일", example = "test@domain.com")
            @NotBlank(message = "이메일은 필수 입력값입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @Schema(description = "인증 코드", example = "839281")
            @NotBlank(message = "인증 코드는 필수 입력값입니다.")
            @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자만 입력 가능합니다.")
            String code
    ) {}
}