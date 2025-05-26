package com.kakaobase.snsapp.domain.auth.controller;

import com.kakaobase.snsapp.domain.auth.dto.AuthRequestDto;
import com.kakaobase.snsapp.domain.auth.dto.AuthResponseDto;
import com.kakaobase.snsapp.domain.auth.exception.AuthErrorCode;
import com.kakaobase.snsapp.domain.auth.service.SecurityTokenManager;
import com.kakaobase.snsapp.domain.auth.service.UserAuthenticationService;
import com.kakaobase.snsapp.domain.auth.util.CookieUtil;
import com.kakaobase.snsapp.global.common.response.CustomResponse;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러
 * 로그인, 로그아웃, 토큰 재발급 등의 인증 관련 API를 처리합니다.
 */
@Tag(name = "인증 API", description = "로그인, 로그아웃, 토큰 관련 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserAuthenticationService userAuthenticationService;
    private final CookieUtil cookieUtil;
    private final SecurityTokenManager securityTokenManager;

    /**
     * 로그인 API
     * 이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.
     * Access Token은 응답 본문에, Refresh Token은 HttpOnly Secure 쿠키로 전달됩니다.
     *
     * @param request 로그인 요청 정보 (이메일, 비밀번호)
     * @return 액세스 토큰을 포함한 응답
     */
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT 토큰을 발급합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "필수 입력값 누락 또는 이메일 형식 오류",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.RefreshTokenMissing.class))),
            @ApiResponse(responseCode = "401", description = "등록되지 않은 이메일 또는 비밀번호 불일치",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.RefreshTokenInvalid.class)))
    })
    @PostMapping("/tokens")
    public ResponseEntity<CustomResponse<AuthResponseDto.LoginResponse>>  login(
            @Parameter(description = "로그인 정보", required = true)
            @Valid @RequestBody AuthRequestDto.Login request,
            HttpServletResponse httpResponse,
            @Parameter(hidden = true) @CookieValue(value = "kakaobase_refresh_token", required = false, defaultValue = "") String providedRefreshToken,
            @Parameter(hidden = true) @RequestHeader(value = "User-Agent", required = true) String userAgent
    ) {

        log.info("로그인 요청: {}", request.email());

        // 로그인 처리 및 토큰 발급
        AuthResponseDto.LoginResponse response = userAuthenticationService.login(request);

        ResponseCookie refreshCookie = userAuthenticationService.getRefreshCookie(providedRefreshToken, userAgent);

        log.info("로그인 성공: {}", request.email());

        // 액세스 토큰을 응답 본문에 포함
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(CustomResponse.success("로그인에 성공하였습니다", response));
    }

    /**
     * 액세스 토큰 재발급 API
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.
     * 리프레시 토큰은 쿠키에서 자동으로 전송됩니다.
     *
     * @param httpRequest HTTP 요청 객체
     * @return 새로 발급된 액세스 토큰을 포함한 응답
     */
    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "리프레시 토큰 없음",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.RefreshTokenMissing.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.RefreshTokenInvalid.class)))
    })
    @PostMapping("/tokens/refresh")
    public CustomResponse<AuthResponseDto.TokenResponse> refreshToken(
            HttpServletRequest httpRequest) {

        log.info("액세스 토큰 재발급 요청");


        String newAccessToken = userAuthenticationService.refreshAuthentication(httpRequest);

        log.info("액세스 토큰 재발급 성공");

        // 새 액세스 토큰을 응답 본문에 포함
        return CustomResponse.success(
                "Access Token이 재발급되었습니다.",
                new AuthResponseDto.TokenResponse(newAccessToken)
        );
    }

    /**
     * 로그아웃 API
     * 현재 사용 중인 토큰을 무효화하고 로그아웃합니다.
     * 리프레시 토큰은 서버에서 블랙리스트에 등록되고, 쿠키에서 제거됩니다.
     *
     * @param httpRequest HTTP 요청 객체
     * @param httpResponse HTTP 응답 객체
     * @return 로그아웃 성공 메시지
     */
    @Operation(summary = "로그아웃", description = "현재 사용 중인 토큰을 무효화하고 로그아웃합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공",
                    content = @Content(schema = @Schema(implementation = CustomResponse.class))),
            @ApiResponse(responseCode = "400", description = "리프레시 토큰 없음",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.RefreshTokenMissing.class)))
    })
    @DeleteMapping("/tokens")
    public CustomResponse<Void> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("로그아웃 요청 수신");

        // Service에 전체 로그아웃 처리 위임
        userAuthenticationService.logout(httpRequest, httpResponse);

        return CustomResponse.success("정상적으로 로그아웃되었습니다.");
    }
}