package com.kakaobase.snsapp.fixture.auth;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.fixture.common.AbstractFixture;
import com.kakaobase.snsapp.global.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 테스트 데이터 생성 Fixture
 *
 * 기능:
 * - 유효한 JWT 토큰 생성
 * - 만료된 JWT 토큰 생성
 * - 잘못된 형식의 JWT 토큰 생성
 * - 테스트용 JWT 토큰 검증
 */
public class JwtTokenFixture extends AbstractFixture {

    // 테스트용 JWT 설정
    private static final String TEST_SECRET = "test-secret-key-for-testing-purposes-only-must-be-very-long-to-meet-requirements";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long ACCESS_TOKEN_VALIDITY = 1800000L; // 30분
    private static final String ISSUER = "kakaobase-test";

    /**
     * JwtTokenProvider를 사용한 유효한 토큰 생성
     * 실제 운영 환경과 동일한 방식으로 토큰 생성
     *
     * @param jwtTokenProvider 실제 JwtTokenProvider 인스턴스
     * @param userDetails CustomUserDetails
     * @return 유효한 JWT 토큰
     */
    public String createValidToken(JwtTokenProvider jwtTokenProvider, CustomUserDetails userDetails) {
        return jwtTokenProvider.createAccessToken(userDetails);
    }

    /**
     * 테스트용 유효한 JWT 토큰 생성 (JwtTokenProvider 없이)
     *
     * @param userDetails CustomUserDetails
     * @return 유효한 JWT 토큰
     */
    public String createValidTestToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY);

        return Jwts.builder()
                .setSubject(userDetails.getId())
                .claim("role", userDetails.getRole())
                .claim("class_name", userDetails.getClassName())
                .setIssuer(ISSUER)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 기본 사용자용 유효한 JWT 토큰 생성
     *
     * @return 기본 사용자의 유효한 JWT 토큰
     */
    public String createDefaultUserToken() {
        CustomUserDetails defaultUser = createDefaultJwtUser();
        return createValidTestToken(defaultUser);
    }

    /**
     * 관리자용 유효한 JWT 토큰 생성
     *
     * @return 관리자의 유효한 JWT 토큰
     */
    public String createAdminToken() {
        CustomUserDetails admin = createAdminJwtUser();
        return createValidTestToken(admin);
    }

    /**
     * 봇용 유효한 JWT 토큰 생성
     *
     * @return 봇의 유효한 JWT 토큰
     */
    public String createBotToken() {
        CustomUserDetails bot = createBotJwtUser();
        return createValidTestToken(bot);
    }

    /**
     * 지정된 ID를 가진 사용자의 JWT 토큰 생성
     *
     * @param userId 사용자 ID
     * @return 지정된 사용자의 JWT 토큰
     */
    public String createTokenForUser(Long userId) {
        CustomUserDetails user = createJwtUserWithId(userId);
        return createValidTestToken(user);
    }

    /**
     * 지정된 역할을 가진 사용자의 JWT 토큰 생성
     *
     * @param userId 사용자 ID
     * @param role 사용자 역할
     * @param className 사용자 기수
     * @return 커스텀 정보를 가진 JWT 토큰
     */
    public String createTokenWithRole(Long userId, String role, String className) {
        CustomUserDetails user = createJwtUser(userId, role, className);
        return createValidTestToken(user);
    }

    /**
     * 만료된 JWT 토큰 생성
     *
     * @param userDetails CustomUserDetails
     * @return 만료된 JWT 토큰
     */
    public String createExpiredToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiredTime = new Date(now.getTime() - 3600000L); // 1시간 전 만료

        return Jwts.builder()
                .setSubject(userDetails.getId())
                .claim("role", userDetails.getRole())
                .claim("class_name", userDetails.getClassName())
                .setIssuer(ISSUER)
                .setIssuedAt(new Date(expiredTime.getTime() - ACCESS_TOKEN_VALIDITY))
                .setExpiration(expiredTime)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 기본 사용자용 만료된 JWT 토큰 생성
     *
     * @return 만료된 JWT 토큰
     */
    public String createExpiredDefaultUserToken() {
        CustomUserDetails defaultUser = createDefaultJwtUser();
        return createExpiredToken(defaultUser);
    }

    /**
     * 잘못된 서명을 가진 JWT 토큰 생성
     *
     * @param userDetails CustomUserDetails
     * @return 잘못된 서명의 JWT 토큰
     */
    public String createInvalidSignatureToken(CustomUserDetails userDetails) {
        // 다른 시크릿 키로 서명
        SecretKey wrongSecretKey = Keys.hmacShaKeyFor("wrong-secret-key-for-testing".getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date validity = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY);

        return Jwts.builder()
                .setSubject(userDetails.getId())
                .claim("role", userDetails.getRole())
                .claim("class_name", userDetails.getClassName())
                .setIssuer(ISSUER)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(wrongSecretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 잘못된 형식의 JWT 토큰 생성
     *
     * @return 잘못된 형식의 토큰 문자열
     */
    public String createMalformedToken() {
        return "invalid.jwt.token.format";
    }

    /**
     * Bearer 접두사가 포함된 Authorization 헤더 값 생성
     *
     * @param token JWT 토큰
     * @return "Bearer {token}" 형태의 문자열
     */
    public String createBearerToken(String token) {
        return "Bearer " + token;
    }

    /**
     * 기본 사용자용 Bearer 토큰 생성
     *
     * @return "Bearer {token}" 형태의 기본 사용자 토큰
     */
    public String createDefaultBearerToken() {
        return createBearerToken(createDefaultUserToken());
    }

    /**
     * 관리자용 Bearer 토큰 생성
     *
     * @return "Bearer {token}" 형태의 관리자 토큰
     */
    public String createAdminBearerToken() {
        return createBearerToken(createAdminToken());
    }

    /**
     * 만료된 Bearer 토큰 생성
     *
     * @return "Bearer {token}" 형태의 만료된 토큰
     */
    public String createExpiredBearerToken() {
        return createBearerToken(createExpiredDefaultUserToken());
    }

    /**
     * 잘못된 Bearer 토큰 생성
     *
     * @return "Bearer {token}" 형태의 잘못된 토큰
     */
    public String createInvalidBearerToken() {
        return createBearerToken(createMalformedToken());
    }

    /**
     * 테스트용 시크릿 키 반환
     *
     * @return 테스트용 SecretKey
     */
    public SecretKey getTestSecretKey() {
        return SECRET_KEY;
    }

    /**
     * 테스트용 시크릿 문자열 반환
     *
     * @return 테스트용 시크릿 문자열
     */
    public String getTestSecret() {
        return TEST_SECRET;
    }

    // === CustomUserDetails 생성 메서드들 ===

    /**
     * 기본 JWT 사용자 생성
     */
    private CustomUserDetails createDefaultJwtUser() {
        return new CustomUserDetails("1", "USER", "PANGYO_1", true);
    }

    /**
     * 관리자 JWT 사용자 생성
     */
    private CustomUserDetails createAdminJwtUser() {
        return new CustomUserDetails("999", "ADMIN", "ALL", true);
    }

    /**
     * 봇 JWT 사용자 생성
     */
    private CustomUserDetails createBotJwtUser() {
        return new CustomUserDetails("1000", "BOT", "ALL", true);
    }

    /**
     * 지정된 ID를 가진 JWT 사용자 생성
     */
    private CustomUserDetails createJwtUserWithId(Long id) {
        return new CustomUserDetails(String.valueOf(id), "USER", "PANGYO_1", true);
    }

    /**
     * 커스텀 정보를 가진 JWT 사용자 생성
     */
    private CustomUserDetails createJwtUser(Long id, String role, String className) {
        return new CustomUserDetails(String.valueOf(id), role, className, true);
    }

    /**
     * 비활성화된 JWT 사용자 생성
     */
    private CustomUserDetails createDisabledJwtUser() {
        return new CustomUserDetails("100", "USER", "PANGYO_1", false);
    }

    /**
     * 비활성화된 사용자용 토큰 생성
     *
     * @return 비활성화된 사용자의 토큰
     */
    public String createDisabledUserToken() {
        CustomUserDetails disabledUser = createDisabledJwtUser();
        return createValidTestToken(disabledUser);
    }

    /**
     * 토큰에서 사용자 ID 추출 (테스트용)
     *
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public String extractUserIdFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * 토큰에서 역할 추출 (테스트용)
     *
     * @param token JWT 토큰
     * @return 사용자 역할
     */
    public String extractRoleFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    /**
     * 토큰에서 기수 추출 (테스트용)
     *
     * @param token JWT 토큰
     * @return 사용자 기수
     */
    public String extractClassNameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("class_name", String.class);
    }

    @Override
    protected Object reset() {
        return this;
    }
}