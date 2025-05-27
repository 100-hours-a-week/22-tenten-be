package com.kakaobase.snsapp.global.mock.constants;
import com.kakaobase.snsapp.domain.members.entity.Member;

public class MockMemberConstants {

    // 기본 Mock 데이터
    public static final Long MOCK_MEMBER_ID = 1L;
    public static final String MOCK_MEMBER_EMAIL = "test@kakao.com";
    public static final String MOCK_MEMBER_NAME = "테스트유저";
    public static final String MOCK_MEMBER_NICKNAME = "테스터";
    public static final String MOCK_MEMBER_PASSWORD = "$2a$12$encodedPassword";
    public static final String MOCK_MEMBER_GITHUB_URL = "https://github.com/testuser";
    public static final String MOCK_MEMBER_PROFILE_IMG_URL = "https://example.com/profile/test-user.jpg";
    public static final Member.ClassName KBT_MOCK_MEMBER_CLASS_NAME = Member.ClassName.PANGYO_1;
    public static final Member.ClassName NON_KBT_MOCK_MEMBER_CLASS_NAME = Member.ClassName.ALL;
    public static final Member.Role MOCK_MEMBER_ROLE = Member.Role.USER;
    public static final Boolean MOCK_MEMBER_IS_BANNED = false;
    public static final Integer MOCK_MEMBER_FOLLOWING_COUNT = 0;
    public static final Integer MOCK_MEMBER_FOLLOWER_COUNT = 0;

    // 관리자 Mock 데이터
    public static final Long MOCK_ADMIN_ID = 2L;
    public static final String MOCK_ADMIN_EMAIL = "admin@kakao.com";
    public static final String MOCK_ADMIN_NICKNAME = "관리자";
    public static final Member.Role MOCK_ADMIN_ROLE = Member.Role.ADMIN;

    // 밴된 유저 Mock 데이터
    public static final Long MOCK_BANNED_MEMBER_ID = 3L;
    public static final String MOCK_BANNED_MEMBER_EMAIL = "banned@kakao.com";
    public static final String MOCK_BANNED_MEMBER_NICKNAME = "밴유저";
    public static final Boolean MOCK_BANNED_MEMBER_IS_BANNED = true;

    // 봇 계정 Mock 데이터
    public static final Long MOCK_BOT_ID = 4L;
    public static final String MOCK_BOT_EMAIL = "bot@kakao.com";
    public static final String MOCK_BOT_NICKNAME = "KakaoBot";
    public static final Member.Role MOCK_BOT_ROLE = Member.Role.BOT;

    // 팔로우 관련 Mock 데이터
    public static final Integer MOCK_FOLLOWING_COUNT = 10;
    public static final Integer MOCK_FOLLOWER_COUNT = 20;

    private MockMemberConstants() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }
}
