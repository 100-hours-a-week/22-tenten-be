package com.kakaobase.snsapp.global.mock.builder;

import com.kakaobase.snsapp.domain.members.entity.Member;
import static com.kakaobase.snsapp.global.mock.constants.MockMemberConstants.*;

public class MockMemberBuilder {

    public static Member createMockKbtMember() {
        return Member.builder()
                .email(MOCK_MEMBER_EMAIL)
                .name(MOCK_MEMBER_NAME)
                .nickname(MOCK_MEMBER_NICKNAME)
                .password(MOCK_MEMBER_PASSWORD)
                .className(KBT_MOCK_MEMBER_CLASS_NAME)
                .githubUrl(MOCK_MEMBER_GITHUB_URL)
                .build();
    }

    public static Member createMockNonKbtMember() {
        return Member.builder()
                .email(MOCK_MEMBER_EMAIL)
                .name(MOCK_MEMBER_NAME)
                .nickname(MOCK_MEMBER_NICKNAME)
                .password(MOCK_MEMBER_PASSWORD)
                .className(NON_KBT_MOCK_MEMBER_CLASS_NAME)
                .githubUrl(MOCK_MEMBER_GITHUB_URL)
                .build();
    }

    public static Member createMockAdmin() {
        Member admin = Member.builder()
                .email(MOCK_ADMIN_EMAIL)
                .name(MOCK_MEMBER_NAME)
                .nickname(MOCK_ADMIN_NICKNAME)
                .password(MOCK_MEMBER_PASSWORD)
                .className(KBT_MOCK_MEMBER_CLASS_NAME)
                .githubUrl(MOCK_MEMBER_GITHUB_URL)
                .build();

        admin.updateRole(MOCK_ADMIN_ROLE);
        return admin;
    }

    public static Member createMockBannedMember() {
        Member bannedMember = Member.builder()
                .email(MOCK_BANNED_MEMBER_EMAIL)
                .name(MOCK_MEMBER_NAME)
                .nickname(MOCK_BANNED_MEMBER_NICKNAME)
                .password(MOCK_MEMBER_PASSWORD)
                .className(KBT_MOCK_MEMBER_CLASS_NAME)
                .githubUrl(MOCK_MEMBER_GITHUB_URL)
                .build();

        bannedMember.updateBanStatus(MOCK_BANNED_MEMBER_IS_BANNED);
        return bannedMember;
    }

    public static Member createMockBot() {
        Member bot = Member.builder()
                .email(MOCK_BOT_EMAIL)
                .name(MOCK_MEMBER_NAME)
                .nickname(MOCK_BOT_NICKNAME)
                .password(MOCK_MEMBER_PASSWORD)
                .className(KBT_MOCK_MEMBER_CLASS_NAME)
                .build();

        bot.updateRole(MOCK_BOT_ROLE);
        return bot;
    }

    public static Member createMockMemberWithProfile() {
        Member member = createMockKbtMember();
        member.updateProfile(MOCK_MEMBER_PROFILE_IMG_URL);
        return member;
    }

    public static Member createMockMemberWithFollows() {
        Member member = createMockKbtMember();

        // 팔로잉 카운트 설정
        for (int i = 0; i < MOCK_FOLLOWING_COUNT; i++) {
            member.incrementFollowingCount();
        }

        // 팔로워 카운트 설정
        for (int i = 0; i < MOCK_FOLLOWER_COUNT; i++) {
            member.incrementFollowerCount();
        }

        return member;
    }
}
