package com.kakaobase.snsapp.global.common.email.service;

import com.kakaobase.snsapp.domain.members.exception.MemberErrorCode;
import com.kakaobase.snsapp.domain.members.exception.MemberException;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

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
    private final StringRedisTemplate redisTemplate;

    private static final String EMAIL_VERIFICATION_PREFIX = "email:verification:";
    private static final String EMAIL_VERIFIED_PREFIX = "email:verified:";
    private final SecureRandom random = new SecureRandom();

    // 인증 코드 만료 시간
    private static final Duration TTL = Duration.ofMinutes(3);

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
        String key = EMAIL_VERIFICATION_PREFIX + email;
        String code = generateCode();

        redisTemplate.opsForValue().set(key, code, TTL);

        // 이메일 전송
        emailSender.sendVerificationEmail(email, code);
        log.info("인증 이메일 전송 to: {}, purpose: {}", email, purpose);

    }

    /**
     * 사용자가 입력한 인증 코드를 검증한다.
     *
     * @param email 인증 이메일
     * @param inputCode 사용자 입력 코드
     */
    public void verifyCode(String email, String inputCode) {

        String key = EMAIL_VERIFICATION_PREFIX + email;
        String code = redisTemplate.opsForValue().get(key);

        if(code==null) {
            throw new MemberException(MemberErrorCode.EMAIL_CODE_EXPIRED);
        }

        // 코드 불일치 시 시도 횟수 증가 및 예외 처리
        if (!code.equals(inputCode)) {
            throw new MemberException(MemberErrorCode.EMAIL_CODE_INVALID);
        }

        // 인증 성공 시 인증 상태 저장
        redisTemplate.opsForValue().set(EMAIL_VERIFIED_PREFIX+email, email, TTL);
        redisTemplate.delete(key);
        log.info("Email verified successfully: {}", email);
    }

    /**
     * 이메일 인증 여부를 반환
     *
     * @param email 확인할 이메일
     * @return 인증 완료 여부
     */
    public boolean isEmailVerified(String email) {
        return redisTemplate.hasKey(EMAIL_VERIFIED_PREFIX+email);
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
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}