package com.kakaobase.snsapp.domain.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 인증 관련 쿠키를 생성하고 관리하는 유틸리티 클래스
 * 리프레시 토큰을 위한 보안 쿠키 생성 및 추출 기능을 제공합니다.
 */
@Component
public class CookieUtil {

    /**
     * application.yml에서 주입받은 리프레시 토큰 쿠키 관련 설정값들
     */
    @Value("${app.jwt.refresh.token-name}")
    private String refreshTokenCookieName;

    @Value("${app.jwt.refresh.expiration-time}")
    private long refreshTokenExpiration;

    @Value("${app.jwt.refresh.path}")
    private String refreshTokenCookiePath;

    @Value("${app.jwt.access.token-name}")
    private String accessTokenCookieName;

    @Value("${app.jwt.access.expiration-time}")
    private long accessTokenExpiration;

    @Value("${app.jwt.access.path}")
    private String accessTokenCookiePath;

    @Value("${app.jwt.secure}")
    private boolean secureCookie;

    @Value("${app.jwt.refresh.domain}")
    private String cookieDomain;

    @Value("${app.jwt.refresh.same-site}")
    private String cookieSameSite;

    public ResponseCookie createRefreshTokenToCookie(String refreshToken) {
        return createTokenToCookie(refreshToken, refreshTokenCookieName, refreshTokenCookiePath, refreshTokenExpiration);
    }

    public ResponseCookie createTokenToAccessCookie(String accessToken) {
        return createTokenToCookie(accessToken, accessTokenCookieName, accessTokenCookiePath, refreshTokenExpiration);
    }

    public ResponseCookie createEmptyAccessCookie() {
        return createTokenToCookie("", accessTokenCookieName, accessTokenCookiePath, 1000L);
    }

    public ResponseCookie createEmptyRefreshCookie() {
        return createTokenToCookie("", refreshTokenCookieName, refreshTokenCookiePath, 1000L);
    }

    private ResponseCookie createTokenToCookie(String token, String tokenCookieName,String path,Long maxAge) {
        return ResponseCookie.from(tokenCookieName, token)
                .path(path)
                .domain(cookieDomain)
                .maxAge(maxAge / 1000)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite)
                .build();
    }

    /**
     * HTTP 요청의 쿠키에서 리프레시 토큰을 추출합니다.
     *
     * @param request HTTP 요청
     * @return 추출된 리프레시 토큰, 없으면 null
     */
    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (refreshTokenCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}