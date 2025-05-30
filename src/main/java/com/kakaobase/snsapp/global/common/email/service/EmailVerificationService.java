package com.kakaobase.snsapp.global.common.email.service;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.members.exception.MemberErrorCode;
import com.kakaobase.snsapp.domain.members.exception.MemberException;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이메일 인증 서비스
 * 임시로 Map을 사용하여 인증 코드를 관리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailSender emailSender;
    private final MemberRepository memberRepository;

    // 이메일 인증 관련 정보를 저장하는 Map
    private final Map<String, VerificationData> verificationStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    // 인증 코드 길이 및 만료 시간 설정
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 3;

    /**
     * 이메일 인증 코드를 생성하고 전송한다.
     *
     * @param email 인증할 이메일
     * @param purpose 인증 목적 (ex. 회원가입, 비밀번호 재설정 등)
     */
    public void sendVerificationCode(String email, String purpose) {
        // 요청 유효성 검증
        validateEmailRequest(email, purpose);

        // 인증 코드 생성 및 저장
        String code = generateCode();
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
        verificationStore.put(email, new VerificationData(code, expirationTime, 0));

        // 이메일 전송
        emailSender.sendVerificationEmail(email, code);
        log.info("Verification code sent to: {}, purpose: {}", email, purpose);

    }

    /**
     * 사용자가 입력한 인증 코드를 검증한다.
     *
     * @param email 인증 이메일
     * @param code 사용자 입력 코드
     */
    public void verifyCode(String email, String code) {
        VerificationData data = verificationStore.get(email);

        // 인증 정보가 없거나 만료되었으면 예외 발생
        if (data == null || LocalDateTime.now().isAfter(data.getExpirationTime())) {
            verificationStore.remove(email);
            throw new MemberException(MemberErrorCode.EMAIL_CODE_EXPIRED);
        }

        // 코드 불일치 시 시도 횟수 증가 및 예외 처리
        if (!data.getCode().equals(code)) {
            data.incrementAttempts();
            throw new MemberException(MemberErrorCode.EMAIL_CODE_INVALID);
        }

        // 인증 성공 시 인증 상태 저장
        data.setVerified(true);
        log.info("Email verified successfully: {}", email);
    }

    /**
     * 이메일 인증 여부를 반환
     *
     * @param email 확인할 이메일
     * @return 인증 완료 여부
     */
    public boolean isEmailVerified(String email) {
        VerificationData data = verificationStore.get(email);
        return data != null && data.isVerified();
    }

    /**
     * 이메일 인증 요청에 대한 유효성 검증을 수행
     *
     * @param email 요청 이메일
     * @param purpose 인증 목적
     */
    private void validateEmailRequest(String email, String purpose) {

        if(purpose.equals("password-change") || purpose.equals("unregister")) {
            if (!memberRepository.existsByEmail(email)) {
                throw new MemberException(MemberErrorCode.MEMBER_NOT_FOUND);
            }
        }
        else if(purpose.equals("sign-up")) {
            if(memberRepository.existsByEmail(email)) {
                throw new MemberException(GeneralErrorCode.RESOURCE_ALREADY_EXISTS, "email");
            }
        }
    }

    /**
     * 랜덤한 6자리 숫자 코드 생성
     *
     * @return 인증 코드
     */
    private String generateCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 이메일 인증 정보 보관 클래스
     */
    @Getter
    private static class VerificationData {
        private final String code;
        private final LocalDateTime expirationTime;
        private int attempts;
        private boolean verified;

        public VerificationData(String code, LocalDateTime expirationTime, int attempts) {
            this.code = code;
            this.expirationTime = expirationTime;
            this.attempts = attempts;
            this.verified = false;
        }

        public void incrementAttempts() {
            this.attempts++;
        }

        public void setVerified(boolean verified) {
            this.verified = verified;
        }
    }
}