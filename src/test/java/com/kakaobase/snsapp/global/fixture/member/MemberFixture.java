package com.kakaobase.snsapp.global.fixture.member;

import com.kakaobase.snsapp.domain.members.entity.Member;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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


    public static List<Member> createMixedMembers(int kbtCount, int nonKbtCount) {
        List<Member> members = new ArrayList<>();

        // KBT 멤버들 생성
        for (int i = 0; i < kbtCount; i++) {
            Member member = createKbtMember();
            ReflectionTestUtils.setField(member, "nickname", "KBT사용자" + (i + 1));
            ReflectionTestUtils.setField(member, "name", "KBT유저" + (i + 1));
            members.add(member);
        }

        // 외부 멤버들 생성
        for (int i = 0; i < nonKbtCount; i++) {
            Member member = createNonKbtMember();
            ReflectionTestUtils.setField(member, "nickname", "외부사용자" + (i + 1));
            ReflectionTestUtils.setField(member, "name", "외부유저" + (i + 1));
            members.add(member);
        }

        return members;
    }

    private static final AtomicLong emailCounter = new AtomicLong(1);

    public static List<Member> createMultipleKbtMembers(int count) {
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long uniqueId = emailCounter.getAndIncrement();
            Member member = Member.builder()
                    .email("kbt-multi-" + uniqueId + "@kakao.com")  // 고유 이메일
                    .nickname("KBT사용자" + uniqueId)
                    .name("KBT유저" + uniqueId)
                    .password("password123")
                    .className(Member.ClassName.ALL)
                    .followerCount(0)
                    .followingCount(0)
                    .isBanned(false)
                    .build();
            members.add(member);
        }
        return members;
    }

    public static List<Member> createMultipleNonKbtMembers(int count) {
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            long uniqueId = emailCounter.getAndIncrement();
            Member member = Member.builder()
                    .email("external-multi-" + uniqueId + "@external.com")  // 고유 이메일
                    .nickname("외부사용자" + uniqueId)
                    .name("외부유저" + uniqueId)
                    .password("password123")
                    .className(null)
                    .followerCount(0)
                    .followingCount(0)
                    .isBanned(false)
                    .build();
            members.add(member);
        }
        return members;
    }

}