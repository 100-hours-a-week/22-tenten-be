package com.kakaobase.snsapp.global.constants;
import com.kakaobase.snsapp.domain.members.entity.Member;

public class MemberFixtureConstants {

    // 기본 Mock 데이터
    public static final Long MEMBER_ID = 1L;
    public static final String MEMBER_EMAIL = "test@kakao.com";
    public static final String MEMBER_NAME = "테스트유저";
    public static final String MEMBER_NICKNAME = "테스터";
    public static final String MEMBER_PASSWORD = "$2a$12$encodedPassword";
    public static final String MEMBER_GITHUB_URL = "https://github.com/testuser";
    public static final String MEMBER_PROFILE_IMG_URL = "https://example.com/profile/test-user.jpg";
    public static final Member.ClassName KBT_MEMBER_CLASS_NAME = Member.ClassName.PANGYO_1;
    public static final Member.ClassName NON_KBT_MEMBER_CLASS_NAME = Member.ClassName.ALL;
    public static final Member.Role MEMBER_ROLE = Member.Role.USER;
    public static final Boolean MEMBER_IS_BANNED = false;
    public static final Integer MEMBER_FOLLOWING_COUNT = 0;
    public static final Integer MEMBER_FOLLOWER_COUNT = 0;

    // 관리자 Mock 데이터
    public static final Long ADMIN_ID = 2L;
    public static final String ADMIN_EMAIL = "admin@kakao.com";
    public static final String ADMIN_NAME = "관리자";
    public static final String ADMIN_NICKNAME = "관리자";
    public static final String ADMIN_PASSWORD = "Admin123!";
    public static final String ADMIN_GITHUB_URL = "https://github.com/admin";
    public static final Member.Role ADMIN_ROLE = Member.Role.ADMIN;

    // 밴된 유저 Mock 데이터
    public static final Long BANNED_MEMBER_ID = 3L;
    public static final String BANNED_MEMBER_EMAIL = "banned@kakao.com";
    public static final String BANNED_MEMBER_NICKNAME = "밴유저";
    public static final Boolean BANNED_MEMBER_IS_BANNED = true;

    // 봇 계정 Mock 데이터
    public static final Long BOT_ID = 4L;
    public static final String BOT_EMAIL = "bot@kakao.com";
    public static final String BOT_NICKNAME = "KakaoBot";
    public static final Member.Role BOT_ROLE = Member.Role.BOT;

    // 팔로우 관련 Mock 데이터
    public static final Integer FOLLOWING_COUNT = 10;
    public static final Integer FOLLOWER_COUNT = 20;

    // ========== 테스트용 유효한 데이터 상수 ==========

    // 유효한 비밀번호들
    public static final String VALID_PASSWORD = "Test1234!";
    public static final String VALID_PASSWORD_ALT = "Valid123!";

    // Non-KBT 회원 데이터
    public static final String NON_KBT_EMAIL = "nonkbt@kakao.com";
    public static final String NON_KBT_NAME = "외부사용자";
    public static final String NON_KBT_NICKNAME = "외부닉네임";
    public static final String NON_KBT_GITHUB_URL = "https://github.com/nonkbtuser";

    // GitHub 없는 회원 데이터
    public static final String NO_GITHUB_EMAIL = "nogithub@kakao.com";
    public static final String NO_GITHUB_NAME = "깃허브없음";
    public static final String NO_GITHUB_NICKNAME = "깃허브없는닉네임";

    // ========== 이메일 유효성 검증 실패 케이스 상수 ==========

    public static final String INVALID_EMAIL_NO_AT = "invalidemail.com";
    public static final String BLANK_EMAIL = "";

    // ========== 비밀번호 유효성 검증 실패 케이스 상수 ==========

    public static final String TOO_SHORT_PASSWORD = "Test1!";  // 7자 (8자 미만)
    public static final String NO_UPPERCASE_PASSWORD = "test1234!";  // 대문자 없음
    public static final String NO_LOWERCASE_PASSWORD = "TEST1234!";  // 소문자 없음
    public static final String NO_DIGIT_PASSWORD = "TestTest!";  // 숫자 없음
    public static final String NO_SPECIAL_CHAR_PASSWORD = "Test1234";  // 특수문자 없음
    public static final String BLANK_PASSWORD = "";  // 빈 비밀번호
    public static final String TOO_LONG_PASSWORD = "VeryLongPassword123456!";  // 21자 (20자 초과)

    // ========== 이름 유효성 검증 실패 케이스 상수 ==========

    public static final String BLANK_NAME = "";
    public static final String TOO_SHORT_NAME = "김";  // 1자 (2자 미만)
    public static final String TOO_LONG_NAME = "매우긴이름을가진사람의이름입니다아주길어요";  // 20자 초과

    // ========== 닉네임 유효성 검증 실패 케이스 상수 ==========

    public static final String BLANK_NICKNAME = "";
    public static final String TOO_SHORT_NICKNAME = "a";  // 1자 (2자 미만)
    public static final String TOO_LONG_NICKNAME = "verylongnicknamethatexceedstwentycharacters";  // 20자 초과
    public static final String INVALID_SPECIAL_CHAR_NICKNAME = "nick@name!";  // @, ! 등 허용되지 않는 특수문자
    public static final String VALID_DOT_NICKNAME = "nick.name";  // 온점(.) 포함 - 허용됨

    // ========== 기수명 유효성 검증 실패 케이스 상수 ==========

    public static final String INVALID_CLASS_NAME = "INVALID_CLASS";  // 존재하지 않는 기수명

    // ========== GitHub URL 유효성 검증 실패 케이스 상수 ==========

    public static final String NON_GITHUB_URL = "https://gitlab.com/testuser";  // GitLab URL
    public static final String INVALID_URL_FORMAT = "not-a-valid-url";  // 잘못된 URL 형식
    public static final String INVALID_GITHUB_FORMAT = "https://github.com/";  // 사용자명 없음

    // ========== 이메일 인증 관련 상수 ==========

    public static final String SIGN_UP_PURPOSE = "sign-up";
    public static final String PASSWORD_RESET_PURPOSE = "password-reset";
    public static final String VALID_VERIFICATION_CODE = "123456";  // 6자리 인증 코드
    public static final String INVALID_CODE_FORMAT = "12345";  // 5자리 (6자리가 아님)
    public static final String INVALID_PURPOSE = "invalid-purpose";  // 허용되지 않는 목적

    // ========== 중복 검증용 상수 ==========

    public static final String DUPLICATE_EMAIL_TEST_EMAIL = "newemail@kakao.com";
    public static final String DUPLICATE_EMAIL_TEST_NAME = "새로운사용자";
    public static final String DUPLICATE_EMAIL_TEST_NICKNAME = "새로운닉네임";
    public static final String DUPLICATE_EMAIL_TEST_GITHUB = "https://github.com/newuser";

    public static final String DUPLICATE_NICKNAME_TEST_EMAIL = "newemail2@kakao.com";
    public static final String DUPLICATE_NICKNAME_TEST_NAME = "새로운사용자2";
    public static final String DUPLICATE_NICKNAME_TEST_GITHUB = "https://github.com/newuser2";

    private MemberFixtureConstants() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }
}