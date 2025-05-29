package com.kakaobase.snsapp.global.fixture;

import com.kakaobase.snsapp.domain.members.entity.Member;
import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;

public class MemberFixture {

    public static Member createKbtMember() {
        return Member.builder()
                .email(MEMBER_EMAIL)
                .name(MEMBER_NAME)
                .nickname(MEMBER_NICKNAME)
                .password(MEMBER_PASSWORD)
                .className(KBT_MEMBER_CLASS_NAME)
                .githubUrl(MEMBER_GITHUB_URL)
                .build();
    }

    public static Member createNonKbtMember() {
        return Member.builder()
                .email(MEMBER_EMAIL)
                .name(MEMBER_NAME)
                .nickname(MEMBER_NICKNAME)
                .password(MEMBER_PASSWORD)
                .className(NON_KBT_MEMBER_CLASS_NAME)
                .githubUrl(MEMBER_GITHUB_URL)
                .build();
    }

    public static Member createAdmin() {
        Member admin = Member.builder()
                .email(ADMIN_EMAIL)
                .name(MEMBER_NAME)
                .nickname(ADMIN_NICKNAME)
                .password(MEMBER_PASSWORD)
                .className(KBT_MEMBER_CLASS_NAME)
                .githubUrl(MEMBER_GITHUB_URL)
                .build();

        admin.updateRole(ADMIN_ROLE);
        return admin;
    }

    public static Member createBannedMember() {
        Member bannedMember = Member.builder()
                .email(BANNED_MEMBER_EMAIL)
                .name(MEMBER_NAME)
                .nickname(BANNED_MEMBER_NICKNAME)
                .password(MEMBER_PASSWORD)
                .className(KBT_MEMBER_CLASS_NAME)
                .githubUrl(MEMBER_GITHUB_URL)
                .build();

        bannedMember.updateBanStatus(BANNED_MEMBER_IS_BANNED);
        return bannedMember;
    }

    public static Member createBot() {
        Member bot = Member.builder()
                .email(BOT_EMAIL)
                .name(MEMBER_NAME)
                .nickname(BOT_NICKNAME)
                .password(MEMBER_PASSWORD)
                .className(KBT_MEMBER_CLASS_NAME)
                .build();

        bot.updateRole(BOT_ROLE);
        return bot;
    }

    public static Member createMemberWithProfile() {
        Member member = createKbtMember();
        member.updateProfile(MEMBER_PROFILE_IMG_URL);
        return member;
    }

    public static Member createMemberWithFollows() {
        Member member = createKbtMember();

        // 팔로잉 카운트 설정
        for (int i = 0; i < FOLLOWING_COUNT; i++) {
            member.incrementFollowingCount();
        }

        // 팔로워 카운트 설정
        for (int i = 0; i < FOLLOWER_COUNT; i++) {
            member.incrementFollowerCount();
        }

        return member;
    }
}