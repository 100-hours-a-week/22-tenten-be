package com.kakaobase.snsapp.global.fixture;

import com.kakaobase.snsapp.domain.members.dto.MemberRequestDto;

import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;

/**
 * MemberRequestDto 관련 테스트 픽스쳐 클래스
 * 다양한 회원 요청 시나리오에 대한 테스트 데이터를 제공합니다.
 */
public class MemberRequestFixture {

    // ========== 정상 케이스 픽스쳐 ==========

    /**
     * 정상적인 KBT 회원가입 요청 DTO 생성
     */
    public static MemberRequestDto.SignUp createValidKbtSignUpRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 정상적인 Non-KBT 회원가입 요청 DTO 생성
     */
    public static MemberRequestDto.SignUp createValidNonKbtSignUpRequest() {
        return new MemberRequestDto.SignUp(
                NON_KBT_EMAIL,
                VALID_PASSWORD_ALT,
                NON_KBT_NAME,
                NON_KBT_NICKNAME,
                NON_KBT_MEMBER_CLASS_NAME.name(),
                NON_KBT_GITHUB_URL
        );
    }

    /**
     * 정상적인 관리자 회원가입 요청 DTO 생성
     */
    public static MemberRequestDto.SignUp createValidAdminSignUpRequest() {
        return new MemberRequestDto.SignUp(
                ADMIN_EMAIL,
                ADMIN_PASSWORD,
                ADMIN_NAME,
                ADMIN_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                ADMIN_GITHUB_URL
        );
    }

    /**
     * GitHub URL이 없는 정상적인 회원가입 요청 DTO 생성
     */
    public static MemberRequestDto.SignUp createValidSignUpRequestWithoutGithub() {
        return new MemberRequestDto.SignUp(
                NO_GITHUB_EMAIL,
                VALID_PASSWORD,
                NO_GITHUB_NAME,
                NO_GITHUB_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                null
        );
    }

    /**
     * 허용되는 특수문자(온점)가 포함된 정상 닉네임
     */
    public static MemberRequestDto.SignUp createValidDotNicknameRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                VALID_DOT_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    // ========== 이메일 유효성 검증 실패 케이스 ==========

    /**
     * 잘못된 이메일 형식 (@가 없는 경우)
     */
    public static MemberRequestDto.SignUp createInvalidEmailFormatRequest() {
        return new MemberRequestDto.SignUp(
                INVALID_EMAIL_NO_AT,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 빈 이메일
     */
    public static MemberRequestDto.SignUp createBlankEmailRequest() {
        return new MemberRequestDto.SignUp(
                BLANK_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * null 이메일
     */
    public static MemberRequestDto.SignUp createNullEmailRequest() {
        return new MemberRequestDto.SignUp(
                null,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    // ========== 비밀번호 유효성 검증 실패 케이스 ==========

    /**
     * 8자 미만의 비밀번호
     */
    public static MemberRequestDto.SignUp createTooShortPasswordRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                TOO_SHORT_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 대문자가 없는 비밀번호
     */
    public static MemberRequestDto.SignUp createNoUpperCasePasswordRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                NO_UPPERCASE_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 소문자가 없는 비밀번호
     */
    public static MemberRequestDto.SignUp createNoLowerCasePasswordRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                NO_LOWERCASE_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 숫자가 없는 비밀번호
     */
    public static MemberRequestDto.SignUp createNoDigitPasswordRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                NO_DIGIT_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 특수문자가 없는 비밀번호
     */
    public static MemberRequestDto.SignUp createNoSpecialCharPasswordRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                NO_SPECIAL_CHAR_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 빈 비밀번호
     */
    public static MemberRequestDto.SignUp createBlankPasswordRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                BLANK_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 20자 초과 비밀번호
     */
    public static MemberRequestDto.SignUp createTooLongPasswordRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                TOO_LONG_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    // ========== 이름 유효성 검증 실패 케이스 ==========

    /**
     * 빈 이름
     */
    public static MemberRequestDto.SignUp createBlankNameRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                BLANK_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 2자 미만 이름
     */
    public static MemberRequestDto.SignUp createTooShortNameRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                TOO_SHORT_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 20자 초과 이름
     */
    public static MemberRequestDto.SignUp createTooLongNameRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                TOO_LONG_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    // ========== 닉네임 유효성 검증 실패 케이스 ==========

    /**
     * 빈 닉네임
     */
    public static MemberRequestDto.SignUp createBlankNicknameRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                BLANK_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 2자 미만 닉네임
     */
    public static MemberRequestDto.SignUp createTooShortNicknameRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                TOO_SHORT_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 20자 초과 닉네임
     */
    public static MemberRequestDto.SignUp createTooLongNicknameRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                TOO_LONG_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 허용되지 않는 특수문자가 포함된 닉네임
     */
    public static MemberRequestDto.SignUp createInvalidSpecialCharNicknameRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                INVALID_SPECIAL_CHAR_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );
    }

    // ========== 기수명 유효성 검증 실패 케이스 ==========

    /**
     * null 기수명
     */
    public static MemberRequestDto.SignUp createNullClassNameRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                null,
                MEMBER_GITHUB_URL
        );
    }

    /**
     * 존재하지 않는 기수명
     */
    public static MemberRequestDto.SignUp createInvalidClassNameRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                INVALID_CLASS_NAME,
                MEMBER_GITHUB_URL
        );
    }

    // ========== GitHub URL 유효성 검증 실패 케이스 ==========

    /**
     * GitHub 도메인이 아닌 URL
     */
    public static MemberRequestDto.SignUp createNonGithubUrlRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                NON_GITHUB_URL
        );
    }

    /**
     * 잘못된 URL 형식
     */
    public static MemberRequestDto.SignUp createInvalidUrlFormatRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                INVALID_URL_FORMAT
        );
    }

    /**
     * GitHub 도메인이지만 잘못된 형식
     */
    public static MemberRequestDto.SignUp createInvalidGithubFormatRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                INVALID_GITHUB_FORMAT
        );
    }

    // ========== 이메일 인증 관련 픽스쳐 ==========

    /**
     * 정상적인 이메일 인증 코드 요청 DTO (회원가입용)
     */
    public static MemberRequestDto.EmailVerificationRequest createValidEmailVerificationRequest() {
        return new MemberRequestDto.EmailVerificationRequest(
                MEMBER_EMAIL,
                SIGN_UP_PURPOSE
        );
    }

    /**
     * 정상적인 이메일 인증 코드 요청 DTO (비밀번호 재설정용)
     */
    public static MemberRequestDto.EmailVerificationRequest createValidPasswordResetVerificationRequest() {
        return new MemberRequestDto.EmailVerificationRequest(
                MEMBER_EMAIL,
                PASSWORD_RESET_PURPOSE
        );
    }

    /**
     * 정상적인 이메일 인증 코드 확인 DTO
     */
    public static MemberRequestDto.EmailVerification createValidEmailVerificationCheck() {
        return new MemberRequestDto.EmailVerification(
                MEMBER_EMAIL,
                VALID_VERIFICATION_CODE
        );
    }

    /**
     * 잘못된 인증 코드 형식 (5자리)
     */
    public static MemberRequestDto.EmailVerification createInvalidCodeFormatRequest() {
        return new MemberRequestDto.EmailVerification(
                MEMBER_EMAIL,
                INVALID_CODE_FORMAT
        );
    }

    /**
     * 잘못된 인증 목적
     */
    public static MemberRequestDto.EmailVerificationRequest createInvalidPurposeRequest() {
        return new MemberRequestDto.EmailVerificationRequest(
                MEMBER_EMAIL,
                INVALID_PURPOSE
        );
    }

    // ========== 중복 검증용 픽스쳐 ==========

    /**
     * 중복된 이메일을 가진 회원가입 요청 (MemberFixtureConstants의 이메일 사용)
     */
    public static MemberRequestDto.SignUp createDuplicateEmailRequest() {
        return new MemberRequestDto.SignUp(
                MEMBER_EMAIL,  // 이미 존재하는 이메일
                VALID_PASSWORD,
                DUPLICATE_EMAIL_TEST_NAME,
                DUPLICATE_EMAIL_TEST_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                DUPLICATE_EMAIL_TEST_GITHUB
        );
    }

    /**
     * 중복된 닉네임을 가진 회원가입 요청 (MemberFixtureConstants의 닉네임 사용)
     */
    public static MemberRequestDto.SignUp createDuplicateNicknameRequest() {
        return new MemberRequestDto.SignUp(
                DUPLICATE_NICKNAME_TEST_EMAIL,
                VALID_PASSWORD,
                DUPLICATE_NICKNAME_TEST_NAME,
                MEMBER_NICKNAME,  // 이미 존재하는 닉네임
                KBT_MEMBER_CLASS_NAME.name(),
                DUPLICATE_NICKNAME_TEST_GITHUB
        );
    }
}