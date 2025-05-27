package com.kakaobase.snsapp.global.mock.builder;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;

import static com.kakaobase.snsapp.global.mock.constants.MockMemberConstants.*;

public class MockCustomUserDetailsBuilder {


    //카부캠 수료생, 수강생 회원의 CustomUserDetails
    public static CustomUserDetails createMockKbtCustomUserDetails() {
        return new CustomUserDetails(
                MOCK_MEMBER_EMAIL,
                MOCK_MEMBER_PASSWORD,
                String.valueOf(MOCK_MEMBER_ID),
                MOCK_MEMBER_ROLE.name(),
                KBT_MOCK_MEMBER_CLASS_NAME.name(),
                MOCK_MEMBER_NICKNAME,
                MOCK_MEMBER_PROFILE_IMG_URL,
                true // isEnabled
        );
    }

    //외부 회원의 CustomUserDetails
    public static CustomUserDetails createMockNonKbtCustomUserDetails() {
        return new CustomUserDetails(
                MOCK_MEMBER_EMAIL,
                MOCK_MEMBER_PASSWORD,
                String.valueOf(MOCK_MEMBER_ID),
                MOCK_MEMBER_ROLE.name(),
                NON_KBT_MOCK_MEMBER_CLASS_NAME.name(),
                MOCK_MEMBER_NICKNAME,
                MOCK_MEMBER_PROFILE_IMG_URL,
                true // isEnabled
        );
    }

    /**
     * JWT 인증 시 사용하는 MockCustomUserDetails 생성
     */
    public static CustomUserDetails createMockCustomUserDetailsForJwt() {
        return new CustomUserDetails(
                String.valueOf(MOCK_MEMBER_ID),
                MOCK_MEMBER_ROLE.name(),
                KBT_MOCK_MEMBER_CLASS_NAME.name(),
                true // isEnabled
        );
    }

    /**
     * 관리자 권한의 MockCustomUserDetails 생성
     */
    public static CustomUserDetails createMockAdminCustomUserDetails() {
        return new CustomUserDetails(
                MOCK_ADMIN_EMAIL,
                MOCK_MEMBER_PASSWORD,
                String.valueOf(MOCK_ADMIN_ID),
                MOCK_ADMIN_ROLE.name(),
                KBT_MOCK_MEMBER_CLASS_NAME.name(),
                MOCK_ADMIN_NICKNAME,
                MOCK_MEMBER_PROFILE_IMG_URL,
                true // isEnabled
        );
    }

    /**
     * 밴된 사용자의 MockCustomUserDetails 생성
     */
    public static CustomUserDetails createMockBannedCustomUserDetails() {
        return new CustomUserDetails(
                MOCK_BANNED_MEMBER_EMAIL,
                MOCK_MEMBER_PASSWORD,
                String.valueOf(MOCK_BANNED_MEMBER_ID),
                MOCK_MEMBER_ROLE.name(),
                KBT_MOCK_MEMBER_CLASS_NAME.name(),
                MOCK_BANNED_MEMBER_NICKNAME,
                MOCK_MEMBER_PROFILE_IMG_URL,
                false // isEnabled (밴된 상태)
        );
    }

    /**
     * 봇 계정의 MockCustomUserDetails 생성
     */
    public static CustomUserDetails createMockBotCustomUserDetails() {
        return new CustomUserDetails(
                MOCK_BOT_EMAIL,
                MOCK_MEMBER_PASSWORD,
                String.valueOf(MOCK_BOT_ID),
                MOCK_BOT_ROLE.name(),
                KBT_MOCK_MEMBER_CLASS_NAME.name(),
                MOCK_BOT_NICKNAME,
                MOCK_MEMBER_PROFILE_IMG_URL,
                true // isEnabled
        );
    }
}
