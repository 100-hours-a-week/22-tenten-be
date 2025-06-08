package com.kakaobase.snsapp.stub;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.global.security.jwt.JwtTokenValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 통합 테스트용 JWT 검증 서비스 Stub
 *
 * 실제 JWT 검증 로직 없이 테스트에 필요한 토큰 검증만 시뮬레이션
 */
@Component
@Primary
@Profile("test")
@Slf4j
public class StubJwtTokenValidator extends JwtTokenValidator {

    // 유효한 토큰 목록 (토큰 -> 사용자 정보 매핑)
    private final Map<String, TokenInfo> validTokens = new ConcurrentHashMap<>();

    // 기본 유효 토큰들
    private static final String DEFAULT_VALID_TOKEN = "valid-jwt-token";
    private static final String ADMIN_TOKEN = "admin-jwt-token";

    public StubJwtTokenValidator() {
        super(null); // JwtUtil 의존성 없이 생성
        setupDefaultTokens();
    }

    /**
     * 기본 토큰들 설정
     */
    private void setupDefaultTokens() {
        // 일반 사용자 토큰
        validTokens.put(DEFAULT_VALID_TOKEN, new TokenInfo("1", "USER", "PANGYO_1"));
        // 관리자 토큰
        validTokens.put(ADMIN_TOKEN, new TokenInfo("999", "ADMIN", "ALL"));
    }

    /**
     * 토큰 유효성 검증 (Stub)
     */
    @Override
    public boolean validateToken(String token) {
        boolean isValid = validTokens.containsKey(token);
        log.info("[STUB] JWT 토큰 검증: {} -> {}", token, isValid);
        return isValid;
    }

    // === 테스트 헬퍼 메서드들 ===

    /**
     * 새로운 유효 토큰 추가 (테스트용)
     */
    public void addValidToken(String token, String userId, String role, String className) {
        validTokens.put(token, new TokenInfo(userId, role, className));
        log.info("[STUB] 유효 토큰 추가: {} -> User({})", token, userId);
    }

    /**
     * 토큰 제거 (테스트용)
     */
    public void removeToken(String token) {
        validTokens.remove(token);
        log.info("[STUB] 토큰 제거: {}", token);
    }

    /**
     * 토큰에서 사용자 정보 조회 (테스트용)
     */
    public TokenInfo getTokenInfo(String token) {
        return validTokens.get(token);
    }

    /**
     * 모든 토큰 초기화 (테스트용)
     */
    public void clearAllTokens() {
        validTokens.clear();
        setupDefaultTokens();
        log.info("[STUB] 모든 토큰 초기화");
    }

    /**
     * 토큰 정보를 담는 내부 클래스
     */
    public static class TokenInfo {
        private final String userId;
        private final String role;
        private final String className;

        public TokenInfo(String userId, String role, String className) {
            this.userId = userId;
            this.role = role;
            this.className = className;
        }

        public String getUserId() { return userId; }
        public String getRole() { return role; }
        public String getClassName() { return className; }
    }
}