package com.kakaobase.snsapp.fixture;

import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.members.entity.Member;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Follow 엔티티 테스트 데이터 생성을 위한 Fixture 클래스
 *
 * 제공 기능:
 * - 기본 Follow 관계 생성
 * - 복잡한 팔로우 관계 네트워크 생성
 * - 팔로우 시나리오별 데이터 생성
 */
public class FollowFixture {

    /**
     * 기본 Follow 관계 생성
     *
     * @param follower 팔로우를 하는 사용자
     * @param following 팔로우를 받는 사용자
     * @return Follow 엔티티
     */
    public static Follow createFollow(Member follower, Member following) {
        return Follow.builder()
                .followerUser(follower)
                .followingUser(following)
                .build();
    }

    /**
     * ID가 설정된 Follow 관계 생성
     *
     * @param id Follow ID
     * @param follower 팔로우를 하는 사용자
     * @param following 팔로우를 받는 사용자
     * @return ID가 설정된 Follow 엔티티
     */
    public static Follow createFollowWithId(Long id, Member follower, Member following) {
        Follow follow = createFollow(follower, following);
        ReflectionTestUtils.setField(follow, "id", id);
        return follow;
    }

    /**
     * 생성 시간이 설정된 Follow 관계 생성
     *
     * @param follower 팔로우를 하는 사용자
     * @param following 팔로우를 받는 사용자
     * @param createdAt 생성 시간
     * @return 생성 시간이 설정된 Follow 엔티티
     */
    public static Follow createFollowWithCreatedAt(Member follower, Member following, LocalDateTime createdAt) {
        Follow follow = createFollow(follower, following);
        ReflectionTestUtils.setField(follow, "createdAt", createdAt);
        return follow;
    }

    /**
     * 한 사용자가 여러 사용자를 팔로우하는 관계 생성
     *
     * @param follower 팔로우를 하는 사용자
     * @param followings 팔로우를 받는 사용자들
     * @return Follow 관계 리스트
     */
    public static List<Follow> createFollowRelations(Member follower, List<Member> followings) {
        return followings.stream()
                .map(following -> createFollow(follower, following))
                .toList();
    }

    /**
     * 여러 사용자가 한 사용자를 팔로우하는 관계 생성
     *
     * @param followers 팔로우를 하는 사용자들
     * @param following 팔로우를 받는 사용자
     * @return Follow 관계 리스트
     */
    public static List<Follow> createFollowerRelations(List<Member> followers, Member following) {
        return followers.stream()
                .map(follower -> createFollow(follower, following))
                .toList();
    }

    /**
     * 상호 팔로우 관계 생성 (맞팔)
     *
     * @param member1 첫 번째 사용자
     * @param member2 두 번째 사용자
     * @return 상호 Follow 관계 리스트 (2개)
     */
    public static List<Follow> createMutualFollowRelations(Member member1, Member member2) {
        return List.of(
                createFollow(member1, member2),
                createFollow(member2, member1)
        );
    }

    /**
     * 순환 팔로우 관계 생성 (A → B → C → A)
     *
     * @param members 순환 관계를 만들 사용자들 (최소 3명)
     * @return 순환 Follow 관계 리스트
     */
    public static List<Follow> createCircularFollowRelations(List<Member> members) {
        if (members.size() < 3) {
            throw new IllegalArgumentException("순환 관계를 만들려면 최소 3명의 사용자가 필요합니다.");
        }

        List<Follow> follows = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            Member follower = members.get(i);
            Member following = members.get((i + 1) % members.size()); // 마지막은 첫 번째로 순환
            follows.add(createFollow(follower, following));
        }
        return follows;
    }

    /**
     * 스타 팔로우 관계 생성 (한 명이 모든 사람을 팔로우)
     *
     * @param star 모든 사람을 팔로우하는 사용자
     * @param others 팔로우를 받는 사용자들
     * @return Follow 관계 리스트
     */
    public static List<Follow> createStarFollowRelations(Member star, List<Member> others) {
        return others.stream()
                .map(other -> createFollow(star, other))
                .toList();
    }

    /**
     * 인플루언서 팔로우 관계 생성 (한 명이 모든 사람에게 팔로우를 받음)
     *
     * @param influencer 모든 사람에게 팔로우를 받는 사용자
     * @param followers 팔로우를 하는 사용자들
     * @return Follow 관계 리스트
     */
    public static List<Follow> createInfluencerFollowRelations(Member influencer, List<Member> followers) {
        return followers.stream()
                .map(follower -> createFollow(follower, influencer))
                .toList();
    }

    /**
     * 복잡한 팔로우 네트워크 생성
     * 여러 사용자 간의 복잡한 팔로우 관계 생성
     *
     * @param members 관계를 만들 사용자들
     * @return 복잡한 Follow 관계 리스트
     */
    public static List<Follow> createComplexFollowNetwork(List<Member> members) {
        if (members.size() < 4) {
            throw new IllegalArgumentException("복잡한 네트워크를 만들려면 최소 4명의 사용자가 필요합니다.");
        }

        List<Follow> follows = new ArrayList<>();

        // 첫 번째 사용자가 나머지 모든 사용자를 팔로우
        Member firstUser = members.get(0);
        for (int i = 1; i < members.size(); i++) {
            follows.add(createFollow(firstUser, members.get(i)));
        }

        // 마지막 사용자가 첫 번째를 제외한 모든 사용자를 팔로우
        Member lastUser = members.get(members.size() - 1);
        for (int i = 1; i < members.size() - 1; i++) {
            follows.add(createFollow(lastUser, members.get(i)));
        }

        // 중간 사용자들 간의 일부 상호 팔로우
        for (int i = 1; i < members.size() - 2; i++) {
            follows.add(createFollow(members.get(i), members.get(i + 1)));
        }

        return follows;
    }

    /**
     * 시간 순서가 있는 Follow 관계 생성
     *
     * @param followers 팔로우를 하는 사용자들
     * @param following 팔로우를 받는 사용자
     * @param startTime 시작 시간
     * @param intervalMinutes 팔로우 간격 (분)
     * @return 시간 순서가 있는 Follow 관계 리스트
     */
    public static List<Follow> createTimeOrderedFollowRelations(List<Member> followers, Member following,
                                                                LocalDateTime startTime, int intervalMinutes) {
        List<Follow> follows = new ArrayList<>();
        LocalDateTime currentTime = startTime;

        for (Member follower : followers) {
            Follow follow = createFollowWithCreatedAt(follower, following, currentTime);
            follows.add(follow);
            currentTime = currentTime.plusMinutes(intervalMinutes);
        }

        return follows;
    }

    /**
     * 페이징 테스트용 Follow 관계 생성
     * ID가 순차적으로 설정된 Follow 관계들 생성
     *
     * @param follower 팔로우를 하는 사용자
     * @param followings 팔로우를 받는 사용자들 (ID 순서대로 정렬되어야 함)
     * @return ID가 순차적으로 설정된 Follow 관계 리스트
     */
    public static List<Follow> createPaginationTestFollows(Member follower, List<Member> followings) {
        List<Follow> follows = new ArrayList<>();

        for (int i = 0; i < followings.size(); i++) {
            Follow follow = createFollowWithId((long) (i + 1), follower, followings.get(i));
            follows.add(follow);
        }

        return follows;
    }

    /**
     * 팔로우 취소 시나리오용 Follow 관계 생성
     *
     * @param follower 팔로우를 하는 사용자
     * @param following 팔로우를 받는 사용자
     * @return 팔로우 취소 테스트용 Follow 엔티티
     */
    public static Follow createFollowForUnfollowTest(Member follower, Member following) {
        Follow follow = createFollow(follower, following);

        // 팔로우 카운트 증가 시뮬레이션
        follower.incrementFollowingCount();
        following.incrementFollowerCount();

        return follow;
    }
}