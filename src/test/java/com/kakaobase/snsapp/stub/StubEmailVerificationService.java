package com.kakaobase.snsapp.stub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 통합 테스트용 이메일 인증 서비스 Stub
 *
 * 실제 이메일 발송 없이 테스트에 필요한 동작만 시뮬레이션
 * 원본 서비스를 상속받지 않고 독립적으로 구현
 */
@Service
@Primary
@Profile("test")
@Slf4j
public class StubEmailVerificationService {

    // 인증 완료된 이메일 목록을 메모리에 저장
    private final Set<String> verifiedEmails = ConcurrentHashMap.newKeySet();

    // 테스트에서 사용할 기본 인증 완료 이메일들
    private static final Set<String> DEFAULT_VERIFIED_EMAILS = Set.of(
            "test@example.com",
            "verified@example.com",
            "admin@example.com"
    );

    public StubEmailVerificationService() {
        // 기본 인증 완료 이메일들을 추가
        verifiedEmails.addAll(DEFAULT_VERIFIED_EMAILS);
    }

    /**
     * 이메일 인증 코드 발송 (Stub - 실제로는 아무것도 하지 않음)
     */
    public void sendVerificationCode(String email, String purpose) {
        log.info("[STUB] 이메일 인증 코드 발송 시뮬레이션: {} (목적: {})", email, purpose);
        // 실제 발송하지 않고 자동으로 인증 완료 처리
        verifiedEmails.add(email);
    }

    /**
     * 인증 코드 검증 (Stub - 항상 성공)
     */
    public void verifyCode(String email, String code) {
        log.info("[STUB] 이메일 인증 코드 검증 시뮬레이션: {} (코드: {})", email, code);
        // 자동으로 인증 완료 처리
        verifiedEmails.add(email);
    }

    /**
     * 이메일 인증 여부 확인
     */
    public boolean isEmailVerified(String email) {
        boolean isVerified = verifiedEmails.contains(email);
        log.info("[STUB] 이메일 인증 상태 확인: {} -> {}", email, isVerified);
        return isVerified;
    }

    // === 테스트 헬퍼 메서드들 ===

    /**
     * 특정 이메일을 인증 완료 상태로 설정 (테스트용)
     */
    public void markAsVerified(String email) {
        verifiedEmails.add(email);
        log.info("[STUB] 이메일 인증 완료로 설정: {}", email);
    }

    /**
     * 특정 이메일의 인증 상태를 제거 (테스트용)
     */
    public void markAsUnverified(String email) {
        verifiedEmails.remove(email);
        log.info("[STUB] 이메일 인증 상태 제거: {}", email);
    }

    /**
     * 모든 인증 상태 초기화 (테스트용)
     */
    public void clearAllVerifications() {
        verifiedEmails.clear();
        verifiedEmails.addAll(DEFAULT_VERIFIED_EMAILS);
        log.info("[STUB] 모든 인증 상태 초기화");
    }

    /**
     * 현재 인증된 이메일 목록 조회 (테스트용)
     */
    public Set<String> getVerifiedEmails() {
        return Set.copyOf(verifiedEmails);
    }
}