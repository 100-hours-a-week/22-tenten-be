package com.kakaobase.snsapp.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 파싱 및 클레임 추출을 담당하는 유틸리티 클래스입니다.
 * 토큰에서 사용자 ID, 역할, 발급 시간, 만료 시간 등의 정보를 추출하는 책임만 갖습니다.
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;

    /**
     * 설정 값을 주입받아 JWT 유틸리티를 초기화합니다.
     *
     * @param secret JWT 서명에 사용된 비밀키
     */
    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출합니다.
     *
     * @param request HTTP 요청
     * @return 추출된 JWT 토큰 (존재하지 않으면 null)
     */
    public String resolveTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("kakaobase_access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }


    /**
     * JWT 토큰에서 모든 클레임을 추출합니다.
     * 인증/인가 관련 예외는 발생시키지 않으며, 예외 발생 시 null을 반환합니다.
     * 인증 관련 예외는 JwtTokenValidator와 Spring Security에서 처리합니다.
     *
     * @param token JWT 토큰
     * @return 토큰에 포함된 모든 클레임, 파싱 실패 시 null
     */
    public Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 클레임을 읽을 수 있도록 처리
            return e.getClaims();
        } catch (Exception e) {
            log.debug("JWT 토큰에서 클레임을 파싱할 수 없습니다: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT 토큰에서 사용자 ID를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 ID, 토큰이 유효하지 않으면, null
     */
    public String getSubject(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * JWT 토큰에서 사용자 역할을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 사용자 역할, 토큰이 유효하지 않거나 역할이 없으면 null
     */
    public String getRole(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    /**
     * JWT 토큰에서 기수(class_name) 정보를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 기수 정보, 토큰이 유효하지 않거나 정보가 없으면 null
     */
    public String getClassName(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("class_name", String.class) : null;
    }

    /**
     * JWT 토큰에서 발급 시간을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 토큰 발급 시간, 토큰이 유효하지 않으면 null
     */
    public Date getIssuedAt(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.getIssuedAt() : null;
    }

    /**
     * JWT 토큰에서 만료 시간을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 토큰 만료 시간, 토큰이 유효하지 않으면 null
     */
    public Date getExpiration(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * 토큰 파싱에 사용되는 SecretKey를 반환합니다.
     * JwtTokenValidator에서 사용됩니다.
     *
     * @return JWT 서명 검증에 사용되는 SecretKey
     */
    public SecretKey getSecretKey() {
        return this.secretKey;
    }
}