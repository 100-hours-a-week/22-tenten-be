package com.kakaobase.snsapp.domain.auth.service;

import com.kakaobase.snsapp.domain.auth.converter.AuthConverter;
import com.kakaobase.snsapp.domain.auth.entity.AuthToken;
import com.kakaobase.snsapp.domain.auth.entity.RevokedRefreshToken;
import com.kakaobase.snsapp.domain.auth.exception.AuthErrorCode;
import com.kakaobase.snsapp.domain.auth.exception.AuthException;
import com.kakaobase.snsapp.domain.auth.repository.AuthTokenRepository;
import com.kakaobase.snsapp.domain.auth.repository.RevokedRefreshTokenRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.nio.charset.StandardCharsets;

/**
 * 보안 토큰(리프레시 토큰)의 생성, 검증, 관리를 담당하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityTokenManager {

    private final AuthTokenRepository authTokenRepository;
    private final RevokedRefreshTokenRepository revokedTokenRepository;
    private final AuthConverter authConverter;

    @Value("${app.jwt.refresh.expiration-time}")
    private long refreshTokenExpirationTimeMillis;

    /**
     * 리프레시 토큰 생성 및 저장
     */
    @Transactional
    public String createRefreshToken(Long userId, String userAgent) {
        // 1. 랜덤 토큰 생성
        String rawToken = generateSecureToken();

        // 2. 만료 시간 계산
        LocalDateTime expiryTime = LocalDateTime.now()
                .plus(Duration.ofMillis(refreshTokenExpirationTimeMillis));

        // 3. AuthConverter를 사용하여 토큰 엔티티 생성 및 저장
        AuthToken tokenEntity = authConverter.toAuthTokenEntity(
                userId,
                rawToken,
                userAgent,
                expiryTime
        );

        authTokenRepository.save(tokenEntity);

        return rawToken;
    }

    /**
     * 리프레시 토큰 검증 및 사용자 ID 반환
     */
    public Long validateRefreshTokenAndGetUserId(String rawToken) {
        String hashedToken = hashToken(rawToken);

        // 1. 취소된 토큰인지 확인
        if (isTokenRevoked(hashedToken)) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_REVOKED);
        }

        // 2. 토큰 조회
        AuthToken tokenEntity = authTokenRepository.findByRefreshTokenHash(hashedToken)
                .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID));

        // 3. 만료 확인
        if (isTokenExpired(tokenEntity)) {
            revokeToken(tokenEntity);
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        return tokenEntity.getMemberId();
    }

    /**
     * 리프레시 토큰 취소
     */
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String hashedToken = hashToken(rawToken);

        AuthToken tokenEntity = authTokenRepository.findByRefreshTokenHash(hashedToken)
                .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID));

        revokeToken(tokenEntity);
    }


    /**
     * 현재 토큰을 제외한 모든 토큰 취소
     */
    @Transactional
    public void revokeAllTokensExcept(Long memberId, String currentRawToken) {
        String currentHashedToken = hashToken(currentRawToken);
        List<AuthToken> tokens = authTokenRepository.findAllByMemberId(memberId);

        for (AuthToken token : tokens) {
            if (!token.getRefreshTokenHash().equals(currentHashedToken)) {
                revokeToken(token);
            }
        }
    }

    /**
     * 토큰 취소 처리 (내부 메서드)
     */
    @Transactional
    public void revokeToken(AuthToken token) {
        // AuthConverter를 사용하여 취소된 토큰 엔티티 생성
        RevokedRefreshToken revokedToken = authConverter.toRevokedTokenEntity(
                token.getRefreshTokenHash(),
                token.getMemberId()
        );

        // 기존 토큰 삭제 및 취소 토큰 저장
        authTokenRepository.delete(token);
        revokedTokenRepository.save(revokedToken);
    }

    /**
     * 토큰이 취소되었는지 확인
     */
    private boolean isTokenRevoked(String hashedToken) {
        return revokedTokenRepository.existsByRefreshTokenHash(hashedToken);
    }

    /**
     * 토큰 만료 여부 확인
     */
    private boolean isTokenExpired(AuthToken token) {
        return token.getExpiresAt().isBefore(LocalDateTime.now());
    }

    /**
     * 안전한 랜덤 토큰 생성
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 토큰 해싱 (SHA-256)
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash token", e);
            throw new AuthException(GeneralErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}