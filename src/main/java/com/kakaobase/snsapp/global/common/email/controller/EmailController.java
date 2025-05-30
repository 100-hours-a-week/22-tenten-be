package com.kakaobase.snsapp.global.common.email.controller;


import com.kakaobase.snsapp.global.common.email.dto.EmailRequest;
import com.kakaobase.snsapp.global.common.email.service.EmailVerificationService;
import com.kakaobase.snsapp.global.common.response.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@Tag(name = "이메일 API", description = "이메일인증 관련 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class EmailController {

    private final EmailVerificationService emailVerificationService;

    /**
     * 이메일 인증 코드 요청 API
     *
     * @param request 이메일 인증 요청 DTO
     * @return 인증 코드 전송 결과
     */
    @Operation(summary = "이메일 인증 코드 요청", description = "이메일 인증 코드를 요청합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "400", description = "필수 입력값 누락 또는 이메일 형식 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요 (비밀번호 변경 목적인 경우)"),
            @ApiResponse(responseCode = "404", description = "해당 이메일을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 등록된 이메일"),
            @ApiResponse(responseCode = "429", description = "요청 횟수 초과"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    @PostMapping("/email/verification-requests")
    public CustomResponse<Void> requestEmailVerification(
            @Parameter(description = "이메일 인증 요청 정보", required = true)
            @Valid @RequestBody EmailRequest.EmailVerificationRequest request) {

        emailVerificationService.sendVerificationCode(request.email(), request.purpose());

        return CustomResponse.success("인증 이메일이 전송되었습니다.");
    }

    /**
     * 이메일 인증 코드 확인 API
     *
     * @param request 이메일 인증 확인 DTO
     * @return 인증 확인 결과
     */
    @Operation(summary = "이메일 인증 코드 확인", description = "이메일 인증 코드를 확인합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 코드 또는 코드 형식 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패 로그아웃 (3회 실패 시)"),
            @ApiResponse(responseCode = "410", description = "인증 코드 만료"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    @PostMapping("/email/verification")
    public CustomResponse<Void> verifyEmail(
            @Parameter(description = "이메일 인증 확인 정보", required = true)
            @Valid @RequestBody EmailRequest.EmailVerification request
    ) {

        emailVerificationService.verifyCode(request.email(), request.code());
        return CustomResponse.success("인증에 성공하였습니다.");
    }
}
