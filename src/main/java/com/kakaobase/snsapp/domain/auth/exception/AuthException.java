package com.kakaobase.snsapp.domain.auth.exception;

import com.kakaobase.snsapp.global.error.code.BaseErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;

/**
 * 인증 도메인에서 발생하는 예외를 처리하는 커스텀 예외 클래스입니다.
 * 로그인, 토큰, 인증 관련 비즈니스 예외는 모두 이 클래스를 사용합니다.
 * Spring Security 관련 예외는 SecurityExceptionHandler에서 처리합니다.
 */
public class AuthException extends CustomException {

    /**
     * 인증 에러 코드를 받아 AuthException을 생성합니다.
     *
     * @param errorCode 인증 에러 코드 (AuthErrorCode)
     */
    public AuthException(BaseErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 인증 에러 코드와 필드를 받아 AuthException을 생성합니다.
     * 에러가 발생한 필드를 동적으로 지정할 때 사용합니다.
     *
     * @param errorCode 인증 에러 코드 (BaseErrorCode)
     * @param field 에러가 발생한 필드
     */
    public AuthException(BaseErrorCode errorCode, String field) {
        super(errorCode, field);
    }

    /**
     * 인증 에러 코드와 필드, 추가 메시지를 받아 AuthException을 생성합니다.
     * 에러 메시지에 추가적인 정보를 포함할 때 사용합니다.
     *
     * @param errorCode 인증 에러 코드 (BaseErrorCode)
     * @param field 에러가 발생한 필드
     * @param additionalMessage 추가 메시지
     */
    public AuthException(BaseErrorCode errorCode, String field, String additionalMessage) {
        super(errorCode, field, additionalMessage);
    }
}