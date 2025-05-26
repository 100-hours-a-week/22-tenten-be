package com.kakaobase.snsapp.global.security.jwt;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성을 담당하는 클래스입니다.
 * 사용자 ID와 역할 정보를 기반으로 Access Token을 생성합니다.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliseconds;

    /**
     * 설정 값을 주입받아 JWT 토큰 제공자를 초기화합니다.
     *
     * @param secret JWT 서명에 사용할 비밀키
     * @param accessTokenValidityTime Access Token의 유효 시간(밀리초)
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access.expiration-time}") long accessTokenValidityTime) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInMilliseconds = accessTokenValidityTime;
    }

    /**
     * 생성된 인증객체를 기반으로 Access Token을 생성합니다.
     *
     * @param customUserDetail 인증객체
     */
    public String createAccessToken(CustomUserDetails customUserDetail) {

        String userId = customUserDetail.getId();
        String role = customUserDetail.getRole();
        String className = customUserDetail.getClassName();

        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .claim("class_name", className)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

}