package com.kakaobase.snsapp.domain.auth.service;

import com.kakaobase.snsapp.domain.auth.dto.AuthRequestDto;
import com.kakaobase.snsapp.domain.auth.dto.AuthResponseDto;
import com.kakaobase.snsapp.domain.auth.exception.AuthErrorCode;
import com.kakaobase.snsapp.domain.auth.exception.AuthException;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetailsService;
import com.kakaobase.snsapp.domain.auth.util.CookieUtil;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import com.kakaobase.snsapp.global.mock.builder.MockCustomUserDetailsBuilder;
import com.kakaobase.snsapp.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.kakaobase.snsapp.global.mock.constants.MockMemberConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private SecurityTokenManager securityTokenManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private CustomUserDetails mockKbtUserDetails;
    private CustomUserDetails mockAdminUserDetails;
    private CustomUserDetails mockBannedUserDetails;
    private AuthRequestDto.Login loginRequest;

    @BeforeEach
    void setUp() {
        //다양한 유저 타입의 MockCustomUserDetails 생성
        mockKbtUserDetails = MockCustomUserDetailsBuilder.createMockKbtCustomUserDetails();
        mockAdminUserDetails = MockCustomUserDetailsBuilder.createMockAdminCustomUserDetails();
        mockBannedUserDetails = MockCustomUserDetailsBuilder.createMockBannedCustomUserDetails();

        // 로그인 요청 DTO 생성
        loginRequest = new AuthRequestDto.Login(
                MOCK_MEMBER_EMAIL,
                "testPassword123"
        );

        // SecurityContextHolder 초기화
        SecurityContextHolder.clearContext();
    }

    // ========== 로그인 단위 테스트 ==========

    @Test
    @DisplayName("KBT 멤버 로그인 - 정상적인 요청시 AuthToken이 올바르게 생성되는지 확인")
    void login_KbtMember_Success() {
        // given
        String expectedAccessToken = "mock.access.token";

        given(customUserDetailsService.loadUserByUsername(MOCK_MEMBER_EMAIL))
                .willReturn(mockKbtUserDetails);
        given(passwordEncoder.matches(loginRequest.password(), mockKbtUserDetails.getPassword()))
                .willReturn(true);
        given(jwtTokenProvider.createAccessToken(mockKbtUserDetails))
                .willReturn(expectedAccessToken);

        // when
        AuthResponseDto.LoginResponse response = authService.login(loginRequest);

        // then - 응답 DTO 검증
        assertThat(response)
                .isNotNull()
                .satisfies(loginResponse -> {
                    assertThat(loginResponse.memberId()).isEqualTo(MOCK_MEMBER_ID);
                    assertThat(loginResponse.nickname()).isEqualTo(MOCK_MEMBER_NICKNAME);
                    assertThat(loginResponse.className()).isEqualTo(KBT_MOCK_MEMBER_CLASS_NAME.name());
                    assertThat(loginResponse.imageUrl()).isEqualTo(MOCK_MEMBER_PROFILE_IMG_URL);
                    assertThat(loginResponse.accessToken()).isEqualTo(expectedAccessToken);
                });

        // SecurityContext에 인증 정보가 설정되었는지 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(mockKbtUserDetails);

        // verify - 메서드 호출 순서와 횟수 검증
        verify(customUserDetailsService).loadUserByUsername(MOCK_MEMBER_EMAIL);
        verify(passwordEncoder).matches(loginRequest.password(), mockKbtUserDetails.getPassword());
        verify(jwtTokenProvider).createAccessToken(mockKbtUserDetails);
    }

    @Test
    @DisplayName("관리자 로그인 - 관리자가 로그인시 적절한 권한이 부여되는지 확인")
    void login_Admin_Success() {
        // given
        String expectedAccessToken = "mock.admin.access.token";
        AuthRequestDto.Login adminLoginRequest = new AuthRequestDto.Login(
                MOCK_ADMIN_EMAIL,
                "adminPassword123"
        );

        given(customUserDetailsService.loadUserByUsername(MOCK_ADMIN_EMAIL))
                .willReturn(mockAdminUserDetails);
        given(passwordEncoder.matches(adminLoginRequest.password(), mockAdminUserDetails.getPassword()))
                .willReturn(true);
        given(jwtTokenProvider.createAccessToken(mockAdminUserDetails))
                .willReturn(expectedAccessToken);

        // when
        AuthResponseDto.LoginResponse response = authService.login(adminLoginRequest);

        // then - 관리자 권한 검증
        assertThat(response)
                .isNotNull()
                .satisfies(loginResponse -> {
                    assertThat(loginResponse.memberId()).isEqualTo(MOCK_ADMIN_ID);
                    assertThat(loginResponse.nickname()).isEqualTo(MOCK_ADMIN_NICKNAME);
                    assertThat(loginResponse.accessToken()).isEqualTo(expectedAccessToken);
                });

        // SecurityContext에 관리자 인증 정보가 설정되었는지 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        assertThat(userDetails.getRole()).isEqualTo(MOCK_ADMIN_ROLE.name());
    }


    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void login_Fail_UserNotFound() {
        // given
        given(customUserDetailsService.loadUserByUsername(MOCK_MEMBER_EMAIL))
                .willThrow(new UsernameNotFoundException("User not found"));

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> {
                    AuthException authException = (AuthException) exception;
                    assertThat(authException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(authException.getErrorCode().getError()).isEqualTo("resource_not_found");
                    assertThat(authException.getErrorCode().getMessage()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
                    assertThat(authException.getEffectiveField()).isEqualTo(MOCK_MEMBER_EMAIL); // 오버라이드된 필드
                });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(customUserDetailsService).loadUserByUsername(MOCK_MEMBER_EMAIL);
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_Fail_InvalidPassword() {
        // given
        String wrongPassword = "wrongPassword123";
        AuthRequestDto.Login wrongPasswordRequest = new AuthRequestDto.Login(
                MOCK_MEMBER_EMAIL,
                wrongPassword
        );

        given(customUserDetailsService.loadUserByUsername(MOCK_MEMBER_EMAIL))
                .willReturn(mockKbtUserDetails);
        given(passwordEncoder.matches(wrongPassword, mockKbtUserDetails.getPassword()))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(wrongPasswordRequest))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> {
                    AuthException authException = (AuthException) exception;
                    assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_PASSWORD);
                    assertThat(authException.getErrorCode().getError()).isEqualTo("invalid_password");
                    assertThat(authException.getErrorCode().getMessage()).isEqualTo("비밀번호가 일치하지 않습니다.");
                    assertThat(authException.getErrorCode().getField()).isEqualTo("password");
                });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        // 비밀번호 검증 실패 후 토큰 생성이 실행되지 않았는지 확인
        verify(customUserDetailsService).loadUserByUsername(MOCK_MEMBER_EMAIL);
        verify(passwordEncoder).matches(wrongPassword, mockKbtUserDetails.getPassword());
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("로그인 시 MockMemberConstants의 상수값들을 활용한 입력 검증 테스트")
    void login_WithMockConstants_Validation() {
        // given
        String expectedAccessToken = "mock.constants.token";

        given(customUserDetailsService.loadUserByUsername(MOCK_MEMBER_EMAIL))
                .willReturn(mockKbtUserDetails);
        given(passwordEncoder.matches(anyString(), eq(MOCK_MEMBER_PASSWORD)))
                .willReturn(true);
        given(jwtTokenProvider.createAccessToken(mockKbtUserDetails))
                .willReturn(expectedAccessToken);

        // when
        AuthResponseDto.LoginResponse response = authService.login(loginRequest);

        // then - MockMemberConstants 값들이 올바르게 사용되었는지 확인
        assertThat(response.memberId()).isEqualTo(MOCK_MEMBER_ID);
        assertThat(response.nickname()).isEqualTo(MOCK_MEMBER_NICKNAME);
        assertThat(response.className()).isEqualTo(KBT_MOCK_MEMBER_CLASS_NAME.name());
        assertThat(response.imageUrl()).isEqualTo(MOCK_MEMBER_PROFILE_IMG_URL);
    }

    @Test
    @DisplayName("유효한 RefreshToken으로 요청시 새로운 AccessToken이 생성되는지 확인")
    void getAccessToken_ValidRefreshToken_Success() {
        // given
        String validRefreshToken = "valid.refresh.token.with.sufficient.length";
        String expectedAccessToken = "new.access.token";
        Long userId = MOCK_MEMBER_ID;

        given(securityTokenManager.validateRefreshTokenAndGetUserId(validRefreshToken))
                .willReturn(userId);
        given(customUserDetailsService.loadUserById(String.valueOf(userId)))
                .willReturn(mockKbtUserDetails);
        given(jwtTokenProvider.createAccessToken(mockKbtUserDetails))
                .willReturn(expectedAccessToken);

        // when
        String result = authService.getAccessToken(validRefreshToken);

        // then
        assertThat(result).isEqualTo(expectedAccessToken);

        verify(securityTokenManager).validateRefreshTokenAndGetUserId(validRefreshToken);
        verify(customUserDetailsService).loadUserById(String.valueOf(userId));
        verify(jwtTokenProvider).createAccessToken(mockKbtUserDetails);
    }

    @Test
    @DisplayName("만료된 RefreshToken으로 요청시 CustomException이 발생하는지 확인")
    void getAccessToken_ExpiredRefreshToken_ShouldThrowException() {
        // given
        String expiredRefreshToken = "expired.refresh.token.with.sufficient.length";

        given(securityTokenManager.validateRefreshTokenAndGetUserId(expiredRefreshToken))
                .willThrow(new CustomException(AuthErrorCode.REFRESH_TOKEN_EXPIRED));

        // when & then
        assertThatThrownBy(() -> authService.getAccessToken(expiredRefreshToken))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
                    assertThat(customException.getErrorCode().getError()).isEqualTo("refresh_token_invalid");
                    assertThat(customException.getErrorCode().getMessage()).isEqualTo("refreshToken이 만료되었습니다.");
                    assertThat(customException.getErrorCode().getField()).isEqualTo("refreshToken");
                });

        verify(securityTokenManager).validateRefreshTokenAndGetUserId(expiredRefreshToken);
        verify(customUserDetailsService, never()).loadUserById(any());
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("잘못된 형식의 RefreshToken으로 요청시 CustomException이 발생하는지 확인")
    void getAccessToken_InvalidFormatRefreshToken_ShouldThrowException() {
        // given
        String invalidFormatToken = "short";

        // when & then
        assertThatThrownBy(() -> authService.getAccessToken(invalidFormatToken))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISSING);
                    assertThat(customException.getErrorCode().getError()).isEqualTo("refresh_token_missing");
                    assertThat(customException.getErrorCode().getMessage()).isEqualTo("refreshToken이 쿠키에 존재하지 않습니다.");
                    assertThat(customException.getErrorCode().getField()).isEqualTo("refreshToken");
                });

        verify(securityTokenManager, never()).validateRefreshTokenAndGetUserId(any());
    }

    @Test
    @DisplayName("RefreshToken이 null일 경우 CustomException이 발생하는지 확인")
    void getAccessToken_NullRefreshToken_ShouldThrowException() {
        // given
        String nullRefreshToken = null;

        // when & then
        assertThatThrownBy(() -> authService.getAccessToken(nullRefreshToken))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISSING);
                    assertThat(customException.getErrorCode().getError()).isEqualTo("refresh_token_missing");
                    assertThat(customException.getErrorCode().getMessage()).isEqualTo("refreshToken이 쿠키에 존재하지 않습니다.");
                    assertThat(customException.getErrorCode().getField()).isEqualTo("refreshToken");
                });

        verify(securityTokenManager, never()).validateRefreshTokenAndGetUserId(any());
    }

    @Test
    @DisplayName("토큰 재발급시 MockKbtMember 유저 정보가 올바르게 포함되는지 확인")
    void getAccessToken_WithMockKbtMember_UserInfoIncluded() {
        // given
        String validRefreshToken = "valid.refresh.token.with.sufficient.length";
        String expectedAccessToken = "new.access.token.with.kbt.member.info";
        Long userId = MOCK_MEMBER_ID;

        given(securityTokenManager.validateRefreshTokenAndGetUserId(validRefreshToken))
                .willReturn(userId);
        given(customUserDetailsService.loadUserById(String.valueOf(userId)))
                .willReturn(mockKbtUserDetails);
        given(jwtTokenProvider.createAccessToken(mockKbtUserDetails))
                .willReturn(expectedAccessToken);

        // when
        String result = authService.getAccessToken(validRefreshToken);

        // then
        assertThat(result).isEqualTo(expectedAccessToken);

        // MockKbtMember의 정보가 올바르게 사용되었는지 검증
        verify(customUserDetailsService).loadUserById(String.valueOf(MOCK_MEMBER_ID));
        verify(jwtTokenProvider).createAccessToken(argThat(userDetails ->
                userDetails.getId().equals(String.valueOf(MOCK_MEMBER_ID)) &&
                        userDetails.getNickname().equals(MOCK_MEMBER_NICKNAME) &&
                        userDetails.getClassName().equals(KBT_MOCK_MEMBER_CLASS_NAME.name())
        ));
    }


    @Test
    @DisplayName("유효한 토큰으로 로그아웃 요청시 토큰 무효화가 정상적으로 수행되는지 확인")
    void logout_ValidToken_Success() {
        // given
        String validRefreshToken = "valid.refresh.token.with.sufficient.length";
        ResponseCookie expectedEmptyCookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .build();

        given(cookieUtil.createEmptyRefreshCookie())
                .willReturn(expectedEmptyCookie);

        // when
        ResponseCookie result = authService.logout(validRefreshToken);

        // then
        assertThat(result).isEqualTo(expectedEmptyCookie);

        verify(securityTokenManager).revokeRefreshToken(validRefreshToken);
        verify(cookieUtil).createEmptyRefreshCookie();
    }

    @Test
    @DisplayName("이미 무효화된 토큰으로 로그아웃 요청시에도 정상 처리되는지 확인")
    void logout_AlreadyRevokedToken_ShouldProcessNormally() {
        // given
        String revokedRefreshToken = "already.revoked.refresh.token.with.sufficient.length";
        ResponseCookie expectedEmptyCookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .build();

        given(cookieUtil.createEmptyRefreshCookie())
                .willReturn(expectedEmptyCookie);
        // revokeRefreshToken은 이미 무효화된 토큰이어도 예외를 발생시키지 않는다고 가정

        // when
        ResponseCookie result = authService.logout(revokedRefreshToken);

        // then
        assertThat(result).isEqualTo(expectedEmptyCookie);

        verify(securityTokenManager).revokeRefreshToken(revokedRefreshToken);
        verify(cookieUtil).createEmptyRefreshCookie();
    }

    @Test
    @DisplayName("토큰이 없는 상태에서 로그아웃 요청시에도 정상 처리되는지 확인")
    void logout_NoToken_ShouldProcessNormally() {
        // given
        String nullRefreshToken = null;
        ResponseCookie expectedEmptyCookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .build();

        given(cookieUtil.createEmptyRefreshCookie())
                .willReturn(expectedEmptyCookie);

        // when
        ResponseCookie result = authService.logout(nullRefreshToken);

        // then
        assertThat(result).isEqualTo(expectedEmptyCookie);

        // 토큰이 없으므로 revokeRefreshToken이 호출되지 않아야 함
        verify(securityTokenManager, never()).revokeRefreshToken(any());
        verify(cookieUtil).createEmptyRefreshCookie();
    }


    @Test
    @DisplayName("RefreshToken 쿠키 생성시 기존 토큰이 파기되고 새 토큰이 생성되는지 확인")
    void getRefreshCookie_WithExistingToken_ShouldRevokeAndCreateNew() {
        // given
        String existingRefreshToken = "existing.refresh.token.with.sufficient.length";
        String userAgent = "Mozilla/5.0 Test Browser";
        String newRefreshToken = "new.refresh.token";
        ResponseCookie expectedCookie = ResponseCookie.from("refreshToken", newRefreshToken).build();

        // SecurityContext에 인증 정보 설정
        SecurityContextHolder.setContext(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(mockKbtUserDetails);

        given(securityTokenManager.createRefreshToken(MOCK_MEMBER_ID, userAgent))
                .willReturn(newRefreshToken);
        given(cookieUtil.createRefreshTokenCookie(newRefreshToken))
                .willReturn(expectedCookie);

        // when
        ResponseCookie result = authService.getRefreshCookie(existingRefreshToken, userAgent);

        // then
        assertThat(result).isEqualTo(expectedCookie);

        verify(securityTokenManager).revokeRefreshToken(existingRefreshToken);
        verify(securityTokenManager).createRefreshToken(MOCK_MEMBER_ID, userAgent);
        verify(cookieUtil).createRefreshTokenCookie(newRefreshToken);
    }


    @Test
    @DisplayName("현재 토큰이 null일 경우 AuthException이 발생하는지 확인")
    void logoutOtherDevices_NullCurrentToken_ShouldThrowException() {
        // given
        Long memberId = MOCK_MEMBER_ID;
        String nullCurrentToken = null;

        // when & then
        assertThatThrownBy(() -> authService.logoutOtherDevices(memberId, nullCurrentToken))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> {
                    AuthException authException = (AuthException) exception;
                    assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_MISSING);
                    assertThat(authException.getErrorCode().getError()).isEqualTo("refresh_token_missing");
                    assertThat(authException.getErrorCode().getMessage()).isEqualTo("refreshToken이 쿠키에 존재하지 않습니다.");
                    assertThat(authException.getErrorCode().getField()).isEqualTo("refreshToken");
                });

        verify(securityTokenManager, never()).revokeAllTokensExcept(any(), any());
    }
}