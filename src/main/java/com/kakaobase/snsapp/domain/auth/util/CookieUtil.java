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

    @Value("${app.jwt.secure}")
    private boolean secureCookie;

    @Value("${app.jwt.refresh.domain}")
    private String cookieDomain;

    @Value("${app.jwt.refresh.same-site}")
    private String cookieSameSite;

    /**
     * 리프레시 토큰을 담은 쿠키를 생성합니다.
     * 생성된 쿠키는 JavaScript에서 접근할 수 없도록 HttpOnly로 설정
     */
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(refreshTokenCookieName, refreshToken)
                .path(refreshTokenCookiePath)
                .domain(cookieDomain)
                .maxAge(refreshTokenExpiration / 1000)
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

    /**
     * 로그아웃 시 사용할 빈 리프레시 토큰 쿠키를 생성합니다.
     * 생성된 쿠키는 즉시 만료되도록 설정됩니다.
     *
     * @return 만료된 쿠키
     */
    public ResponseCookie createEmptyRefreshCookie() {
        return ResponseCookie.from(refreshTokenCookieName, "")
                .maxAge(0) // 즉시 만료
                .path(refreshTokenCookiePath)
                .httpOnly(true)
                .secure(secureCookie)
                .domain(cookieDomain)
                .build();

    }
}