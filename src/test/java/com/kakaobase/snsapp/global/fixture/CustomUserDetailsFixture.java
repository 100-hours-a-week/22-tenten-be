package com.kakaobase.snsapp.global.fixture;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;

import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;

public class CustomUserDetailsFixture {


    //카부캠 수료생, 수강생 회원의 CustomUserDetails
    public static CustomUserDetails createKbtCustomUserDetails() {
        return new CustomUserDetails(
                MEMBER_EMAIL,
                MEMBER_PASSWORD,
                String.valueOf(MEMBER_ID),
                MEMBER_ROLE.name(),
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_NICKNAME,
                MEMBER_PROFILE_IMG_URL,
                true // isEnabled
        );
    }

    //외부 회원의 CustomUserDetails
    public static CustomUserDetails createNonKbtCustomUserDetails() {
        return new CustomUserDetails(
                MEMBER_EMAIL,
                MEMBER_PASSWORD,
                String.valueOf(MEMBER_ID),
                MEMBER_ROLE.name(),
                NON_KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_NICKNAME,
                MEMBER_PROFILE_IMG_URL,
                true // isEnabled
        );
    }

    /**
     * JWT 인증 시 사용하는 MockCustomUserDetails 생성
     */
    public static CustomUserDetails createCustomUserDetailsForJwt() {
        return new CustomUserDetails(
                String.valueOf(MEMBER_ID),
                MEMBER_ROLE.name(),
                KBT_MEMBER_CLASS_NAME.name(),
                true // isEnabled
        );
    }

    /**
     * 관리자 권한의 MockCustomUserDetails 생성
     */
    public static CustomUserDetails createAdminCustomUserDetails() {
        return new CustomUserDetails(
                ADMIN_EMAIL,
                MEMBER_PASSWORD,
                String.valueOf(ADMIN_ID),
                ADMIN_ROLE.name(),
                KBT_MEMBER_CLASS_NAME.name(),
                ADMIN_NICKNAME,
                MEMBER_PROFILE_IMG_URL,
                true // isEnabled
        );
    }

    /**
     * 밴된 사용자의 MockCustomUserDetails 생성
     */
    public static CustomUserDetails createBannedCustomUserDetails() {
        return new CustomUserDetails(
                BANNED_MEMBER_EMAIL,
                MEMBER_PASSWORD,
                String.valueOf(BANNED_MEMBER_ID),
                MEMBER_ROLE.name(),
                KBT_MEMBER_CLASS_NAME.name(),
                BANNED_MEMBER_NICKNAME,
                MEMBER_PROFILE_IMG_URL,
                false // isEnabled (밴된 상태)
        );
    }

    /**
     * 봇 계정의 MockCustomUserDetails 생성
     */
    public static CustomUserDetails createBotCustomUserDetails() {
        return new CustomUserDetails(
                BOT_EMAIL,
                MEMBER_PASSWORD,
                String.valueOf(BOT_ID),
                BOT_ROLE.name(),
                KBT_MEMBER_CLASS_NAME.name(),
                BOT_NICKNAME,
                MEMBER_PROFILE_IMG_URL,
                true // isEnabled
        );
    }
}
