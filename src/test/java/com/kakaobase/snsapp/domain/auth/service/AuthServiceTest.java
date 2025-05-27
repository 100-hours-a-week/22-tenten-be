package com.kakaobase.snsapp.domain.auth.service;

import com.kakaobase.snsapp.domain.auth.dto.AuthRequestDto;
import com.kakaobase.snsapp.domain.auth.dto.AuthResponseDto;
import com.kakaobase.snsapp.domain.auth.exception.AuthErrorCode;
import com.kakaobase.snsapp.domain.auth.exception.AuthException;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetailsService;
import com.kakaobase.snsapp.domain.auth.util.CookieUtil;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.mock.builder.MockCustomUserDetailsBuilder;
import com.kakaobase.snsapp.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
@DisplayName("AuthService 테스트")
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

    private CustomUserDetails mockUserDetails;
    private AuthRequestDto.Login loginRequest;

    @BeforeEach
    void setUp() {
        // MockCustomUserDetailsBuilder를 사용해 테스트용 CustomUserDetails 생성
        mockUserDetails = MockCustomUserDetailsBuilder.createMockKbtCustomUserDetails();

        // 로그인 요청 DTO 생성
        loginRequest = new AuthRequestDto.Login(
                MOCK_MEMBER_EMAIL,
                "testPassword123"
        );
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void login_Success() {
        // given
        String expectedAccessToken = "mock.access.token";

        given(customUserDetailsService.loadUserByUsername(MOCK_MEMBER_EMAIL))
                .willReturn(mockUserDetails);
        given(passwordEncoder.matches(loginRequest.password(), mockUserDetails.getPassword()))
                .willReturn(true);
        given(jwtTokenProvider.createAccessToken(mockUserDetails))
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

        // verify - 메서드 호출 순서와 횟수 검증 (가장 중요!)
        verify(customUserDetailsService).loadUserByUsername(MOCK_MEMBER_EMAIL);
        verify(passwordEncoder).matches(loginRequest.password(), mockUserDetails.getPassword());
        verify(jwtTokenProvider).createAccessToken(mockUserDetails);
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 존재하지 않는 사용자")
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
                    assertThat(authException.getMessage()).contains(MOCK_MEMBER_EMAIL);
                });

        // verify - 사용자를 찾지 못했으므로 이후 로직들이 실행되지 않았는지 확인
        verify(customUserDetailsService).loadUserByUsername(MOCK_MEMBER_EMAIL);
        // 사용자를 찾지 못했으므로 비밀번호 검증과 토큰 생성이 실행되지 않아야 함
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 잘못된 비밀번호")
    void login_Fail_InvalidPassword() {
        // given
        String wrongPassword = "wrongPassword123";
        AuthRequestDto.Login wrongPasswordRequest = new AuthRequestDto.Login(
                MOCK_MEMBER_EMAIL,
                wrongPassword
        );

        given(customUserDetailsService.loadUserByUsername(MOCK_MEMBER_EMAIL))
                .willReturn(mockUserDetails);
        given(passwordEncoder.matches(wrongPassword, mockUserDetails.getPassword()))
                .willReturn(false); // 비밀번호 불일치

        // when & then
        assertThatThrownBy(() -> authService.login(wrongPasswordRequest))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> {
                    AuthException authException = (AuthException) exception;
                    assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_PASSWORD);
                });

        // SecurityContext에 인증 정보가 설정되지 않았는지 확인
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        // verify - 비밀번호 검증 실패 후 인증 관련 로직들이 실행되지 않았는지 확인
        verify(customUserDetailsService).loadUserByUsername(MOCK_MEMBER_EMAIL);
        verify(passwordEncoder).matches(wrongPassword, mockUserDetails.getPassword());
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }
}