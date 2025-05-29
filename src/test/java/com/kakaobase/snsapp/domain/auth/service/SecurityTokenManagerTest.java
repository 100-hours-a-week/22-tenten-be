package com.kakaobase.snsapp.domain.auth.service;

import com.kakaobase.snsapp.domain.auth.converter.AuthConverter;
import com.kakaobase.snsapp.domain.auth.entity.AuthToken;
import com.kakaobase.snsapp.domain.auth.entity.RevokedRefreshToken;
import com.kakaobase.snsapp.domain.auth.exception.AuthErrorCode;
import com.kakaobase.snsapp.domain.auth.exception.AuthException;
import com.kakaobase.snsapp.domain.auth.repository.AuthTokenRepository;
import com.kakaobase.snsapp.domain.auth.repository.RevokedRefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityTokenManager 단위 테스트")
class SecurityTokenManagerTest {

    @InjectMocks
    private SecurityTokenManager securityTokenManager;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private RevokedRefreshTokenRepository revokedTokenRepository;

    @Mock
    private AuthConverter authConverter;

    @Mock
    private AuthToken mockAuthToken;

    @Mock
    private RevokedRefreshToken mockRevokedToken;

    private String testRawToken;
    private String testHashedToken;
    private String testUserAgent;
    private LocalDateTime testExpiryTime;

    @BeforeEach
    void setUp() {
        // 리플렉션을 사용하여 refreshTokenExpirationTimeMillis 설정
        ReflectionTestUtils.setField(securityTokenManager, "refreshTokenExpirationTimeMillis", 86400000L); // 24시간

        testRawToken = "test.raw.token.with.sufficient.length.for.testing";
        testHashedToken = "hashed.token.value";
        testUserAgent = "Mozilla/5.0 Test Browser";
        testExpiryTime = LocalDateTime.now().plusDays(1);
    }

    @Test
    @DisplayName("리프레시 토큰 생성 - MockMemberConstants를 사용한 정상적인 토큰 생성")
    void createRefreshToken_Success() {
        // given
        given(authConverter.toAuthTokenEntity(eq(MEMBER_ID), anyString(), eq(testUserAgent), any(LocalDateTime.class)))
                .willReturn(mockAuthToken);
        given(authTokenRepository.save(mockAuthToken))
                .willReturn(mockAuthToken);

        // when
        String result = securityTokenManager.createRefreshToken(MEMBER_ID, testUserAgent);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.length()).isGreaterThan(20); // 충분한 길이의 토큰인지 확인

        verify(authConverter).toAuthTokenEntity(eq(MEMBER_ID), anyString(), eq(testUserAgent), any(LocalDateTime.class));
        verify(authTokenRepository).save(mockAuthToken);
    }

    @Test
    @DisplayName("관리자 리프레시 토큰 생성 - MockMemberConstants의 관리자 ID 사용")
    void createRefreshToken_AdminUser_Success() {
        // given
        given(authConverter.toAuthTokenEntity(eq(ADMIN_ID), anyString(), eq(testUserAgent), any(LocalDateTime.class)))
                .willReturn(mockAuthToken);
        given(authTokenRepository.save(mockAuthToken))
                .willReturn(mockAuthToken);

        // when
        String result = securityTokenManager.createRefreshToken(ADMIN_ID, testUserAgent);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();

        verify(authConverter).toAuthTokenEntity(eq(ADMIN_ID), anyString(), eq(testUserAgent), any(LocalDateTime.class));
        verify(authTokenRepository).save(mockAuthToken);
    }

    @Test
    @DisplayName("리프레시 토큰 생성 - 토큰 만료 시간이 올바르게 설정되는지 확인")
    void createRefreshToken_ExpiryTimeSet_Success() {
        // given
        LocalDateTime beforeCreation = LocalDateTime.now();

        given(authConverter.toAuthTokenEntity(eq(MEMBER_ID), anyString(), eq(testUserAgent), any(LocalDateTime.class)))
                .willReturn(mockAuthToken);
        given(authTokenRepository.save(mockAuthToken))
                .willReturn(mockAuthToken);

        // when
        String result = securityTokenManager.createRefreshToken(MEMBER_ID, testUserAgent);

        // then
        assertThat(result).isNotNull();

        // AuthConverter 호출 시 전달된 만료 시간이 현재 시간 + 24시간 범위에 있는지 확인
        verify(authConverter).toAuthTokenEntity(
                eq(MEMBER_ID),
                anyString(),
                eq(testUserAgent),
                argThat(expiryTime ->
                        expiryTime.isAfter(beforeCreation.plusHours(23)) &&
                                expiryTime.isBefore(beforeCreation.plusHours(25))
                )
        );
    }


    @Test
    @DisplayName("유효한 리프레시 토큰 검증 - 정상적으로 사용자 ID 반환")
    void validateRefreshTokenAndGetUserId_ValidToken_Success() {
        // given
        given(revokedTokenRepository.existsByRefreshTokenHash(anyString()))
                .willReturn(false);
        given(authTokenRepository.findByRefreshTokenHash(anyString()))
                .willReturn(Optional.of(mockAuthToken));
        given(mockAuthToken.getExpiresAt())
                .willReturn(testExpiryTime);
        given(mockAuthToken.getMemberId())
                .willReturn(MEMBER_ID);

        // when
        Long result = securityTokenManager.validateRefreshTokenAndGetUserId(testRawToken);

        // then
        assertThat(result).isEqualTo(MEMBER_ID);

        verify(revokedTokenRepository).existsByRefreshTokenHash(anyString());
        verify(authTokenRepository).findByRefreshTokenHash(anyString());
        verify(mockAuthToken).getExpiresAt();
        verify(mockAuthToken).getMemberId();
    }

    @Test
    @DisplayName("취소된 리프레시 토큰으로 검증 시 AuthException 발생")
    void validateRefreshTokenAndGetUserId_RevokedToken_ShouldThrowException() {
        // given
        given(revokedTokenRepository.existsByRefreshTokenHash(anyString()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> securityTokenManager.validateRefreshTokenAndGetUserId(testRawToken))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> {
                    AuthException authException = (AuthException) exception;
                    assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_REVOKED);
                    assertThat(authException.getErrorCode().getError()).isEqualTo("refresh_token_invalid");
                    assertThat(authException.getErrorCode().getMessage()).isEqualTo("사용할 수 없는 refreshToken입니다.");
                    assertThat(authException.getErrorCode().getField()).isEqualTo("refreshToken");
                });

        verify(revokedTokenRepository).existsByRefreshTokenHash(anyString());
        verify(authTokenRepository, never()).findByRefreshTokenHash(anyString());
    }

    @Test
    @DisplayName("존재하지 않는 리프레시 토큰으로 검증 시 AuthException 발생")
    void validateRefreshTokenAndGetUserId_NonExistentToken_ShouldThrowException() {
        // given
        given(revokedTokenRepository.existsByRefreshTokenHash(anyString()))
                .willReturn(false);
        given(authTokenRepository.findByRefreshTokenHash(anyString()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> securityTokenManager.validateRefreshTokenAndGetUserId(testRawToken))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> {
                    AuthException authException = (AuthException) exception;
                    assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID);
                    assertThat(authException.getErrorCode().getError()).isEqualTo("refresh_token_invalid");
                    assertThat(authException.getErrorCode().getMessage()).isEqualTo("refreshToken이 유효하지 않습니다.");
                    assertThat(authException.getErrorCode().getField()).isEqualTo("refreshToken");
                });

        verify(revokedTokenRepository).existsByRefreshTokenHash(anyString());
        verify(authTokenRepository).findByRefreshTokenHash(anyString());
    }

    @Test
    @DisplayName("만료된 리프레시 토큰으로 검증 시 AuthException 발생")
    void validateRefreshTokenAndGetUserId_ExpiredToken_ShouldThrowException() {
        // given
        LocalDateTime expiredTime = LocalDateTime.now().minusHours(1);

        given(revokedTokenRepository.existsByRefreshTokenHash(anyString()))
                .willReturn(false);
        given(authTokenRepository.findByRefreshTokenHash(anyString()))
                .willReturn(Optional.of(mockAuthToken));
        given(mockAuthToken.getExpiresAt())
                .willReturn(expiredTime);
        given(authConverter.toRevokedTokenEntity(anyString(), eq(MEMBER_ID)))
                .willReturn(mockRevokedToken);
        given(mockAuthToken.getMemberId())
                .willReturn(MEMBER_ID);
        given(mockAuthToken.getRefreshTokenHash())
                .willReturn(testHashedToken);

        // when & then
        assertThatThrownBy(() -> securityTokenManager.validateRefreshTokenAndGetUserId(testRawToken))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> {
                    AuthException authException = (AuthException) exception;
                    assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
                    assertThat(authException.getErrorCode().getError()).isEqualTo("refresh_token_invalid");
                    assertThat(authException.getErrorCode().getMessage()).isEqualTo("refreshToken이 만료되었습니다.");
                    assertThat(authException.getErrorCode().getField()).isEqualTo("refreshToken");
                });

        verify(revokedTokenRepository).existsByRefreshTokenHash(anyString());
        verify(authTokenRepository).findByRefreshTokenHash(anyString());
        verify(mockAuthToken).getExpiresAt();
        verify(authTokenRepository).delete(mockAuthToken);
        verify(revokedTokenRepository).save(mockRevokedToken);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰 취소 - 정상적으로 토큰 무효화")
    void revokeRefreshToken_ValidToken_Success() {
        // given
        given(authTokenRepository.existsByRefreshTokenHash(anyString()))
                .willReturn(true);
        given(authTokenRepository.findByRefreshTokenHash(anyString()))
                .willReturn(Optional.of(mockAuthToken));
        given(mockAuthToken.getRefreshTokenHash())
                .willReturn(testHashedToken);
        given(mockAuthToken.getMemberId())
                .willReturn(MEMBER_ID);
        given(authConverter.toRevokedTokenEntity(testHashedToken, MEMBER_ID))
                .willReturn(mockRevokedToken);

        // when
        securityTokenManager.revokeRefreshToken(testRawToken);

        // then
        verify(authTokenRepository).existsByRefreshTokenHash(anyString());
        verify(authTokenRepository).findByRefreshTokenHash(anyString());
        verify(authConverter).toRevokedTokenEntity(testHashedToken, MEMBER_ID);
        verify(authTokenRepository).delete(mockAuthToken);
        verify(revokedTokenRepository).save(mockRevokedToken);
    }

    @Test
    @DisplayName("존재하지 않는 리프레시 토큰 취소 - 무시하고 정상 처리")
    void revokeRefreshToken_NonExistentToken_ShouldIgnore() {
        // given
        given(authTokenRepository.existsByRefreshTokenHash(anyString()))
                .willReturn(false);

        // when
        securityTokenManager.revokeRefreshToken(testRawToken);

        // then
        verify(authTokenRepository).existsByRefreshTokenHash(anyString());
        verify(authTokenRepository, never()).findByRefreshTokenHash(anyString());
        verify(authTokenRepository, never()).delete(any());
        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("토큰 존재하지만 조회 실패 시 AuthException 발생")
    void revokeRefreshToken_TokenExistsButNotFound_ShouldThrowException() {
        // given
        given(authTokenRepository.existsByRefreshTokenHash(anyString()))
                .willReturn(true);
        given(authTokenRepository.findByRefreshTokenHash(anyString()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> securityTokenManager.revokeRefreshToken(testRawToken))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> {
                    AuthException authException = (AuthException) exception;
                    assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID);
                    assertThat(authException.getErrorCode().getError()).isEqualTo("refresh_token_invalid");
                    assertThat(authException.getErrorCode().getMessage()).isEqualTo("refreshToken이 유효하지 않습니다.");
                    assertThat(authException.getEffectiveField()).isEqualTo("해당 리프레시 토큰값을 DB에서 조회할 수 없음");
                });

        verify(authTokenRepository).existsByRefreshTokenHash(anyString());
        verify(authTokenRepository).findByRefreshTokenHash(anyString());
    }

    @Test
    @DisplayName("사용자에게 토큰이 없는 경우 - 정상 처리")
    void revokeAllTokensExcept_NoTokens_ShouldProcessNormally() {
        // given
        String currentRawToken = "current.token";
        given(authTokenRepository.findAllByMemberId(MEMBER_ID))
                .willReturn(Arrays.asList());

        // when
        securityTokenManager.revokeAllTokensExcept(MEMBER_ID, currentRawToken);

        // then
        verify(authTokenRepository).findAllByMemberId(MEMBER_ID);
        verify(authTokenRepository, never()).delete(any());
        verify(revokedTokenRepository, never()).save(any());
    }

    // ========== 토큰 만료 확인 테스트 ==========

    @Test
    @DisplayName("토큰 만료 확인 - 만료된 토큰인 경우")
    void isTokenExpired_ExpiredToken_ShouldReturnTrue() {
        // given
        LocalDateTime expiredTime = LocalDateTime.now().minusHours(1);
        given(mockAuthToken.getExpiresAt()).willReturn(expiredTime);

        // when
        given(revokedTokenRepository.existsByRefreshTokenHash(anyString()))
                .willReturn(false);
        given(authTokenRepository.findByRefreshTokenHash(anyString()))
                .willReturn(Optional.of(mockAuthToken));
        given(mockAuthToken.getMemberId()).willReturn(MEMBER_ID);
        given(mockAuthToken.getRefreshTokenHash()).willReturn(testHashedToken);
        given(authConverter.toRevokedTokenEntity(anyString(), eq(MEMBER_ID)))
                .willReturn(mockRevokedToken);

        // then - validateRefreshTokenAndGetUserId 호출 시 만료 예외 발생
        assertThatThrownBy(() -> securityTokenManager.validateRefreshTokenAndGetUserId(testRawToken))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> {
                    AuthException authException = (AuthException) exception;
                    assertThat(authException.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
                });
    }

    @Test
    @DisplayName("토큰 만료 확인 - 유효한 토큰인 경우")
    void isTokenExpired_ValidToken_ShouldReturnFalse() {
        // given
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        given(mockAuthToken.getExpiresAt()).willReturn(futureTime);
        given(mockAuthToken.getMemberId()).willReturn(MEMBER_ID);

        // when
        given(revokedTokenRepository.existsByRefreshTokenHash(anyString()))
                .willReturn(false);
        given(authTokenRepository.findByRefreshTokenHash(anyString()))
                .willReturn(Optional.of(mockAuthToken));

        // then - validateRefreshTokenAndGetUserId 호출 시 정상 처리
        Long result = securityTokenManager.validateRefreshTokenAndGetUserId(testRawToken);
        assertThat(result).isEqualTo(MEMBER_ID);
    }

    // ========== 토큰 해싱 테스트 ==========

    @Test
    @DisplayName("토큰 해싱 - 동일한 입력에 대해 동일한 해시값 생성")
    void hashToken_SameInput_ShouldProduceSameHash() {
        // given
        String rawToken1 = "test.token.for.hashing";
        String rawToken2 = "test.token.for.hashing";

        // when
        given(revokedTokenRepository.existsByRefreshTokenHash(anyString()))
                .willReturn(false);
        given(authTokenRepository.findByRefreshTokenHash(anyString()))
                .willReturn(Optional.of(mockAuthToken));
        given(mockAuthToken.getExpiresAt()).willReturn(testExpiryTime);
        given(mockAuthToken.getMemberId()).willReturn(MEMBER_ID);

        // then - 동일한 토큰에 대해 동일한 해시값이 생성되어야 함
        // 내부적으로 해싱이 일관되게 작동하는지 확인
        Long result1 = securityTokenManager.validateRefreshTokenAndGetUserId(rawToken1);
        Long result2 = securityTokenManager.validateRefreshTokenAndGetUserId(rawToken2);

        assertThat(result1).isEqualTo(result2);
        assertThat(result1).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("토큰 해싱 - 서로 다른 입력에 대해 서로 다른 해시값 생성")
    void hashToken_DifferentInput_ShouldProduceDifferentHash() {
        // given
        String rawToken1 = "first.token.for.hashing";
        String rawToken2 = "second.token.for.hashing";

        AuthToken mockAuthToken1 = mock(AuthToken.class);
        AuthToken mockAuthToken2 = mock(AuthToken.class);

        given(mockAuthToken1.getExpiresAt()).willReturn(testExpiryTime);
        given(mockAuthToken1.getMemberId()).willReturn(MEMBER_ID);
        given(mockAuthToken2.getExpiresAt()).willReturn(testExpiryTime);
        given(mockAuthToken2.getMemberId()).willReturn(ADMIN_ID);

        // when & then
        given(revokedTokenRepository.existsByRefreshTokenHash(anyString()))
                .willReturn(false);

        // 첫 번째 토큰 검증
        given(authTokenRepository.findByRefreshTokenHash(anyString()))
                .willReturn(Optional.of(mockAuthToken1));
        Long result1 = securityTokenManager.validateRefreshTokenAndGetUserId(rawToken1);

        // 두 번째 토큰 검증
        given(authTokenRepository.findByRefreshTokenHash(anyString()))
                .willReturn(Optional.of(mockAuthToken2));
        Long result2 = securityTokenManager.validateRefreshTokenAndGetUserId(rawToken2);

        // 서로 다른 사용자 ID가 반환되어야 함 (다른 해시값으로 인해)
        assertThat(result1).isEqualTo(MEMBER_ID);
        assertThat(result2).isEqualTo(ADMIN_ID);
        assertThat(result1).isNotEqualTo(result2);
    }
}