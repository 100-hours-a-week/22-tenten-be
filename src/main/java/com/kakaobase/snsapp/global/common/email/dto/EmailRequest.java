package com.kakaobase.snsapp.global.common.email.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class EmailRequest {
    @Schema(description = "이메일 인증 코드 요청 DTO")
    public record EmailVerificationRequest(
            @Schema(description = "이메일", example = "test@domain.com")
            @NotBlank(message = "이메일은 필수 입력값입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            @JsonProperty("email")
            String email,

            @Schema(description = "인증 목적", example = "sign-up", allowableValues = {"sign-up", "password-change", "unregister"})
            @NotNull(message = "인증 목적은 필수 입력값입니다.")
            @Pattern(regexp = "^(sign-up|password-change|unregister)$", message = "인증목적이 올바르지 않습니다")
            @JsonProperty("purpose")
            String purpose
    ) {}

    @Schema(description = "이메일 인증 코드 확인 DTO")
    public record EmailVerification(
            @Schema(description = "이메일", example = "test@domain.com")
            @NotBlank(message = "이메일은 필수 입력값입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            @JsonProperty("email")
            String email,

            @Schema(description = "인증 코드", example = "839281")
            @NotBlank(message = "인증 코드는 필수 입력값입니다.")
            @Pattern(regexp = "^\\d{6}$", message = "인증 코드는 6자리 숫자만 입력 가능합니다.")
            @JsonProperty("code")
            String code
    ) {}
}
