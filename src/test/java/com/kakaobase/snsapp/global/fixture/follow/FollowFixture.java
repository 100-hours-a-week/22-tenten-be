package com.kakaobase.snsapp.global.fixture.follow;

import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.global.fixture.member.MemberFixture;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;

public class FollowFixture {

    /**
     * 기본 팔로우 관계 생성
     * follower: KBT 멤버, following: Non-KBT 멤버
     */
    public static Follow createBasicFollow() {
        Member follower = MemberFixture.createKbtMember();
        Member following = MemberFixture.createNonKbtMember();

        return Follow.builder()
                .followerUser(follower)
                .followingUser(following)
                .build();
    }

    /**
     * 특정 멤버들 간의 팔로우 관계 생성
     */
    public static Follow createFollow(Member follower, Member following) {
        return Follow.builder()
                .followerUser(follower)
                .followingUser(following)
                .build();
    }

    /**
     * 관리자가 일반 사용자를 팔로우하는 관계
     */
    public static Follow createAdminFollowUser() {
        Member admin = MemberFixture.createAdmin();
        Member user = MemberFixture.createKbtMember();

        return Follow.builder()
                .followerUser(admin)
                .followingUser(user)
                .build();
    }

    /**
     * 일반 사용자가 관리자를 팔로우하는 관계
     */
    public static Follow createUserFollowAdmin() {
        Member user = MemberFixture.createKbtMember();
        Member admin = MemberFixture.createAdmin();

        return Follow.builder()
                .followerUser(user)
                .followingUser(admin)
                .build();
    }

    /**
     * 밴된 사용자가 관련된 팔로우 관계 (팔로워가 밴된 경우)
     */
    public static Follow createBannedUserFollow() {
        Member bannedUser = MemberFixture.createBannedMember();
        Member normalUser = MemberFixture.createKbtMember();

        return Follow.builder()
                .followerUser(bannedUser)
                .followingUser(normalUser)
                .build();
    }

    /**
     * 봇이 관련된 팔로우 관계
     */
    public static Follow createBotFollow() {
        Member bot = MemberFixture.createBot();
        Member user = MemberFixture.createKbtMember();

        return Follow.builder()
                .followerUser(bot)
                .followingUser(user)
                .build();
    }

    /**
     * 상호 팔로우를 위한 두 개의 Follow 엔티티 생성
     * A -> B, B -> A 팔로우 관계
     */
    public static Follow[] createMutualFollow() {
        Member userA = createMemberWithEmail("userA@kakao.com", "사용자A");
        Member userB = createMemberWithEmail("userB@kakao.com", "사용자B");

        Follow followAtoB = Follow.builder()
                .followerUser(userA)
                .followingUser(userB)
                .build();

        Follow followBtoA = Follow.builder()
                .followerUser(userB)
                .followingUser(userA)
                .build();

        return new Follow[]{followAtoB, followBtoA};
    }

    /**
     * 여러 개의 팔로우 관계 생성 (페이징 테스트용)
     * 한 사용자가 여러 사용자를 팔로우하는 경우
     */
    public static Follow[] createMultipleFollows(int count) {
        Member follower = MemberFixture.createKbtMember();
        Follow[] follows = new Follow[count];

        for (int i = 0; i < count; i++) {
            Member following = createMemberWithEmail(
                    "following" + i + "@kakao.com",
                    "팔로잉사용자" + i
            );

            follows[i] = Follow.builder()
                    .followerUser(follower)
                    .followingUser(following)
                    .build();
        }

        return follows;
    }

    /**
     * 특정 사용자를 여러 사용자가 팔로우하는 관계 생성 (인기 사용자)
     */
    public static Follow[] createMultipleFollowers(int count) {
        Member popularUser = MemberFixture.createKbtMember();
        Follow[] follows = new Follow[count];

        for (int i = 0; i < count; i++) {
            Member follower = createMemberWithEmail(
                    "follower" + i + "@kakao.com",
                    "팔로워" + i
            );

            follows[i] = Follow.builder()
                    .followerUser(follower)
                    .followingUser(popularUser)
                    .build();
        }

        return follows;
    }

    /**
     * ID를 가진 Follow 엔티티 생성 (Repository 테스트용)
     */
    public static Follow createFollowWithId(Long id, Member follower, Member following) {
        return Follow.builder()
                .id(id)
                .followerUser(follower)
                .followingUser(following)
                .build();
    }

    // ========== 헬퍼 메서드 ==========

    /**
     * 커스텀 이메일과 닉네임으로 Member 생성
     */
    private static Member createMemberWithEmail(String email, String nickname) {
        return Member.builder()
                .email(email)
                .name(MEMBER_NAME)
                .nickname(nickname)
                .password(MEMBER_PASSWORD)
                .className(KBT_MEMBER_CLASS_NAME)
                .githubUrl(MEMBER_GITHUB_URL)
                .build();
    }

    /**
     * 팔로우 관계에서 사용할 두 개의 서로 다른 Member 생성
     */
    public static Member[] createTwoMembers() {
        Member memberA = createMemberWithEmail("memberA@kakao.com", "멤버A");
        Member memberB = createMemberWithEmail("memberB@kakao.com", "멤버B");

        return new Member[]{memberA, memberB};
    }

    /**
     * isFollow 테스트를 위한 팔로우 관계가 있는 Member들과 Follow 엔티티 반환
     */
    public static FollowTestData createFollowTestData() {
        Member[] members = createTwoMembers();
        Follow follow = createFollow(members[0], members[1]);

        return new FollowTestData(members[0], members[1], follow);
    }

    /**
     * 팔로우 테스트에 필요한 데이터를 담는 클래스
     */
    public static class FollowTestData {
        public final Member follower;
        public final Member following;
        public final Follow follow;

        public FollowTestData(Member follower, Member following, Follow follow) {
            this.follower = follower;
            this.following = following;
            this.follow = follow;
        }
    }

    /**
     * 한 명이 여러 명을 팔로우하는 관계 생성
     * @param follower 팔로우하는 사용자 (1명)
     * @param followings 팔로우받는 사용자들 (여러명)
     * @return Follow 리스트
     */
    public static List<Follow> createFollowsOneToMany(Member follower, List<Member> followings) {
        return followings.stream()
                .map(following -> createFollow(follower, following))
                .collect(Collectors.toList());
    }

    /**
     * 여러 명이 한 명을 팔로우하는 관계 생성
     * @param followers 팔로우하는 사용자들 (여러명)
     * @param following 팔로우받는 사용자 (1명)
     * @return Follow 리스트
     */
    public static List<Follow> createFollowsManyToOne(List<Member> followers, Member following) {
        return followers.stream()
                .map(follower -> createFollow(follower, following))
                .collect(Collectors.toList());
    }

    /**
     * 완전 연결 - 모든 사용자가 서로를 팔로우 (자기 자신 제외)
     * @param members 사용자 목록
     * @return Follow 리스트
     */
    public static List<Follow> createFullyConnectedFollows(List<Member> members) {
        List<Follow> follows = new ArrayList<>();
        for (Member follower : members) {
            for (Member following : members) {
                if (!follower.equals(following)) {  // 자기 자신은 팔로우하지 않음
                    follows.add(createFollow(follower, following));
                }
            }
        }
        return follows;
    }
}