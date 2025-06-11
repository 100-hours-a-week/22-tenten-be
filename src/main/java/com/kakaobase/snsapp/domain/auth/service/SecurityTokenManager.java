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
        String hashedToken = hashToken(rawToken);

        // 2. 만료 시간 계산
        LocalDateTime expiryTime = LocalDateTime.now()
                .plus(Duration.ofMillis(refreshTokenExpirationTimeMillis));

        // 3. AuthConverter를 사용하여 토큰 엔티티 생성 및 저장
        AuthToken tokenEntity = authConverter.toAuthTokenEntity(
                userId,
                hashedToken,
                userAgent,
                expiryTime
        );

        authTokenRepository.save(tokenEntity);

        return rawToken;
    }

    /**
     * 리프레시 토큰 취소
     */
    @Transactional
    public void revokeRefreshToken(String rawToken) {

        AuthToken refeshToken = findByRefreshToken(rawToken);

        // AuthConverter를 사용하여 취소된 토큰 엔티티 생성
        RevokedRefreshToken revokedToken = authConverter.toRevokedTokenEntity(
                refeshToken.getRefreshTokenHash(),
                refeshToken.getMemberId()
        );

        // 기존 토큰 삭제 및 취소 토큰 저장
        authTokenRepository.delete(refeshToken);
        revokedTokenRepository.save(revokedToken);
    }

    @Transactional
    public boolean isExistRefreshToken(String rawToken) {
        String hashedToken = hashToken(rawToken);
        return authTokenRepository.existsByRefreshTokenHash(hashedToken);
    }

    @Transactional
    public boolean isRevokedToken(String rawToken) {
        String hashedToken = hashToken(rawToken);
        return revokedTokenRepository.existsByRefreshTokenHash(hashedToken);
    }

    @Transactional
    public AuthToken findByRefreshToken(String rawToken) {
        String hashedToken = hashToken(rawToken);
        return authTokenRepository.findByRefreshTokenHash(hashedToken)
                .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID));
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