package com.kakaobase.snsapp.domain.follow.repository;

import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.global.fixture.follow.FollowFixture;
import com.kakaobase.snsapp.global.fixture.member.MemberFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
@DisplayName("FollowRepository 통합 테스트")
class FollowRepositoryTest {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private EntityManager entityManager;

    private Member follower1;
    private Member follower2;
    private Member follower3;
    private Member following1;
    private Member following2;
    private Follow follow1;
    private Follow follow2;
    private Follow follow3;

    @BeforeEach
    void setUp() {
        // 팔로워 3명 생성 (KBT 2명, 외부 1명)
        List<Member> followers = MemberFixture.createMixedMembers(2, 1);

        // 팔로잉 대상 2명 생성 (KBT 1명, 외부 1명)
        List<Member> followings = MemberFixture.createMixedMembers(1, 1);

        follower1 = followers.get(0);
        follower2 = followers.get(1);
        follower3 = followers.get(2);
        following1 = followings.get(0);
        following2 = followings.get(1);

        // Member 저장
        followers.forEach(entityManager::persist);
        followings.forEach(entityManager::persist);

        // Follow 관계 생성 및 저장
        // follower1 → following1, following2
        // follower2 → following1
        // follower3 → (팔로우 없음)
        List<Follow> follows = Arrays.asList(
                FollowFixture.createFollow(follower1, following1),
                FollowFixture.createFollow(follower2, following1),
                FollowFixture.createFollow(follower1, following2)
        );

        follows.forEach(entityManager::persist);

        follow1 = follows.get(0);
        follow2 = follows.get(1);
        follow3 = follows.get(2);

        entityManager.flush();
        entityManager.clear();
    }

    // ========== existsByFollowerUserAndFollowingUser() 테스트 ==========

    @Test
    @DisplayName("팔로우 관계 존재 확인 - 존재하는 경우 true 반환")
    void existsByFollowerUserAndFollowingUser_ExistingRelation_ReturnsTrue() {
        // when
        boolean exists = followRepository.existsByFollowerUserAndFollowingUser(follower1, following1);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("팔로우 관계 존재 확인 - 존재하지 않는 경우 false 반환")
    void existsByFollowerUserAndFollowingUser_NonExistingRelation_ReturnsFalse() {
        // when
        boolean exists = followRepository.existsByFollowerUserAndFollowingUser(follower3, following1);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("팔로우 관계 존재 확인 - 같은 사용자의 경우 false 반환")
    void existsByFollowerUserAndFollowingUser_SameUser_ReturnsFalse() {
        // when
        boolean exists = followRepository.existsByFollowerUserAndFollowingUser(follower1, follower1);

        // then
        assertThat(exists).isFalse();
    }

    // ========== findByFollowerUserAndFollowingUser() 테스트 ==========

    @Test
    @DisplayName("팔로우 관계 조회 - 존재하는 경우 Follow 엔티티 반환")
    void findByFollowerUserAndFollowingUser_ExistingRelation_ReturnsFollow() {
        // when
        Optional<Follow> followOptional = followRepository.findByFollowerUserAndFollowingUser(follower1, following1);

        // then
        assertThat(followOptional).isPresent();
        assertThat(followOptional.get().getFollowerUser().getId()).isEqualTo(follower1.getId());
        assertThat(followOptional.get().getFollowingUser().getId()).isEqualTo(following1.getId());
    }

    @Test
    @DisplayName("팔로우 관계 조회 - 존재하지 않는 경우 Empty 반환")
    void findByFollowerUserAndFollowingUser_NonExistingRelation_ReturnsEmpty() {
        // when
        Optional<Follow> followOptional = followRepository.findByFollowerUserAndFollowingUser(follower3, following1);

        // then
        assertThat(followOptional).isEmpty();
    }

    // ========== findFollowersByFollowingUserWithCursor() 테스트 ==========

    @Test
    @DisplayName("팔로워 목록 조회 - 커서 없이 모든 팔로워 조회")
    void findFollowersByFollowingUserWithCursor_WithoutCursor_ReturnsAllFollowers() {
        // when
        List<Object[]> followers = followRepository.findFollowersByFollowingUserWithCursor(
                following1.getId(), 10, null);

        // then
        assertThat(followers).hasSize(2); // follower1, follower2가 following1을 팔로우

        // ID 순으로 정렬되어 반환되는지 확인
        Long firstFollowerId = (Long) followers.get(0)[0];
        Long secondFollowerId = (Long) followers.get(1)[0];
        assertThat(firstFollowerId).isLessThan(secondFollowerId);

        // 팔로워 정보 확인
        List<Long> followerIds = Arrays.asList(firstFollowerId, secondFollowerId);
        assertThat(followerIds).containsExactlyInAnyOrder(follower1.getId(), follower2.getId());
    }

    @Test
    @DisplayName("팔로워 목록 조회 - 커서를 사용한 페이징")
    void findFollowersByFollowingUserWithCursor_WithCursor_ReturnsFilteredFollowers() {
        // given
        Long cursor = follower1.getId(); // follower1의 ID를 커서로 사용

        // when
        List<Object[]> followers = followRepository.findFollowersByFollowingUserWithCursor(
                following1.getId(), 10, cursor);

        // then
        // 커서보다 큰 ID만 반환되어야 함
        assertThat(followers).isNotEmpty();
        for (Object[] follower : followers) {
            Long followerId = (Long) follower[0];
            assertThat(followerId).isGreaterThan(cursor);
        }
    }

    @Test
    @DisplayName("팔로워 목록 조회 - limit 적용 확인")
    void findFollowersByFollowingUserWithCursor_WithLimit_ReturnsLimitedResults() {
        // when
        List<Object[]> followers = followRepository.findFollowersByFollowingUserWithCursor(
                following1.getId(), 1, null);

        // then
        assertThat(followers).hasSize(1);
    }

    @Test
    @DisplayName("팔로워 목록 조회 - 팔로워가 없는 경우 빈 목록 반환")
    void findFollowersByFollowingUserWithCursor_NoFollowers_ReturnsEmptyList() {
        // given
        List<Member> newUsers = MemberFixture.createMultipleKbtMembers(1);
        Member newUser = newUsers.get(0);
        entityManager.persist(newUser);
        entityManager.flush();

        // when
        List<Object[]> followers = followRepository.findFollowersByFollowingUserWithCursor(
                newUser.getId(), 10, null);

        // then
        assertThat(followers).isEmpty();
    }

    @Test
    @DisplayName("팔로워 목록 조회 - 반환되는 데이터 구조 확인")
    void findFollowersByFollowingUserWithCursor_DataStructure_ReturnsCorrectFormat() {
        // when
        List<Object[]> followers = followRepository.findFollowersByFollowingUserWithCursor(
                following1.getId(), 10, null);

        // then
        assertThat(followers).isNotEmpty();

        Object[] firstFollower = followers.get(0);
        assertThat(firstFollower).hasSize(4); // id, nickname, name, profile_img_url
        assertThat(firstFollower[0]).isInstanceOf(Long.class);    // id
        assertThat(firstFollower[1]).isInstanceOf(String.class); // nickname
        assertThat(firstFollower[2]).isInstanceOf(String.class); // name
        // profile_img_url은 nullable이므로 String 또는 null
    }

    // ========== findFollowingsByFollowerUserWithCursor() 테스트 ==========

    @Test
    @DisplayName("팔로잉 목록 조회 - 커서 없이 모든 팔로잉 조회")
    void findFollowingsByFollowerUserWithCursor_WithoutCursor_ReturnsAllFollowings() {
        // when
        List<Object[]> followings = followRepository.findFollowingsByFollowerUserWithCursor(
                follower1.getId(), 10, null);

        // then
        assertThat(followings).hasSize(2); // follower1은 following1, following2를 팔로우

        // ID 순으로 정렬되어 반환되는지 확인
        Long firstFollowingId = (Long) followings.get(0)[0];
        Long secondFollowingId = (Long) followings.get(1)[0];
        assertThat(firstFollowingId).isLessThan(secondFollowingId);

        // 팔로잉 정보 확인
        List<Long> followingIds = Arrays.asList(firstFollowingId, secondFollowingId);
        assertThat(followingIds).containsExactlyInAnyOrder(following1.getId(), following2.getId());
    }

    @Test
    @DisplayName("팔로잉 목록 조회 - 커서를 사용한 페이징")
    void findFollowingsByFollowerUserWithCursor_WithCursor_ReturnsFilteredFollowings() {
        // given
        Long cursor = following1.getId(); // following1의 ID를 커서로 사용

        // when
        List<Object[]> followings = followRepository.findFollowingsByFollowerUserWithCursor(
                follower1.getId(), 10, cursor);

        // then
        // 커서보다 큰 ID만 반환되어야 함
        assertThat(followings).isNotEmpty();
        for (Object[] following : followings) {
            Long followingId = (Long) following[0];
            assertThat(followingId).isGreaterThan(cursor);
        }
    }

    @Test
    @DisplayName("팔로잉 목록 조회 - limit 적용 확인")
    void findFollowingsByFollowerUserWithCursor_WithLimit_ReturnsLimitedResults() {
        // when
        List<Object[]> followings = followRepository.findFollowingsByFollowerUserWithCursor(
                follower1.getId(), 1, null);

        // then
        assertThat(followings).hasSize(1);
    }

    @Test
    @DisplayName("팔로잉 목록 조회 - 팔로잉이 없는 경우 빈 목록 반환")
    void findFollowingsByFollowerUserWithCursor_NoFollowings_ReturnsEmptyList() {
        // when
        List<Object[]> followings = followRepository.findFollowingsByFollowerUserWithCursor(
                follower3.getId(), 10, null);

        // then
        assertThat(followings).isEmpty();
    }

    @Test
    @DisplayName("팔로잉 목록 조회 - 반환되는 데이터 구조 확인")
    void findFollowingsByFollowerUserWithCursor_DataStructure_ReturnsCorrectFormat() {
        // when
        List<Object[]> followings = followRepository.findFollowingsByFollowerUserWithCursor(
                follower1.getId(), 10, null);

        // then
        assertThat(followings).isNotEmpty();

        Object[] firstFollowing = followings.get(0);
        assertThat(firstFollowing).hasSize(4); // id, nickname, name, profile_img_url
        assertThat(firstFollowing[0]).isInstanceOf(Long.class);    // id
        assertThat(firstFollowing[1]).isInstanceOf(String.class); // nickname
        assertThat(firstFollowing[2]).isInstanceOf(String.class); // name
        // profile_img_url은 nullable이므로 String 또는 null
    }

    // ========== findFollowingUserIdsByFollowerUser() 테스트 ==========

    @Test
    @DisplayName("팔로잉 사용자 ID 목록 조회 - 정상적으로 ID Set 반환")
    void findFollowingUserIdsByFollowerUser_ExistingFollowings_ReturnsIdSet() {
        // when
        Set<Long> followingUserIds = followRepository.findFollowingUserIdsByFollowerUser(follower1);

        // then
        assertThat(followingUserIds).hasSize(2);
        assertThat(followingUserIds).containsExactlyInAnyOrder(following1.getId(), following2.getId());
    }

    @Test
    @DisplayName("팔로잉 사용자 ID 목록 조회 - 팔로잉이 없는 경우 빈 Set 반환")
    void findFollowingUserIdsByFollowerUser_NoFollowings_ReturnsEmptySet() {
        // when
        Set<Long> followingUserIds = followRepository.findFollowingUserIdsByFollowerUser(follower3);

        // then
        assertThat(followingUserIds).isEmpty();
    }

    @Test
    @DisplayName("팔로잉 사용자 ID 목록 조회 - 부분적인 팔로잉 관계 확인")
    void findFollowingUserIdsByFollowerUser_PartialFollowings_ReturnsCorrectIds() {
        // when
        Set<Long> followingUserIds = followRepository.findFollowingUserIdsByFollowerUser(follower2);

        // then
        assertThat(followingUserIds).hasSize(1);
        assertThat(followingUserIds).contains(following1.getId());
        assertThat(followingUserIds).doesNotContain(following2.getId());
    }

    // ========== 복합적인 시나리오 테스트 ==========

    @Test
    @DisplayName("커서 페이징 동작 확인 - 연속적인 페이징 테스트")
    void cursorPaging_ContinuousPaging_WorksCorrectly() {
        // given: 더 많은 팔로워 생성
        List<Member> extraFollowers = MemberFixture.createMixedMembers(1, 1); // KBT 1명, 외부 1명
        extraFollowers.forEach(entityManager::persist);

        List<Follow> extraFollows = extraFollowers.stream()
                .map(follower -> FollowFixture.createFollow(follower, following1))
                .collect(Collectors.toList());
        extraFollows.forEach(entityManager::persist);
        entityManager.flush();

        // when: 첫 번째 페이지 조회 (limit = 2)
        List<Object[]> firstPage = followRepository.findFollowersByFollowingUserWithCursor(
                following1.getId(), 2, null);

        // then: 첫 번째 페이지 검증
        assertThat(firstPage).hasSize(2);
        Long firstCursor = (Long) firstPage.get(1)[0]; // 두 번째 항목의 ID를 커서로 사용

        // when: 두 번째 페이지 조회
        List<Object[]> secondPage = followRepository.findFollowersByFollowingUserWithCursor(
                following1.getId(), 2, firstCursor);

        // then: 두 번째 페이지 검증
        assertThat(secondPage).isNotEmpty();
        for (Object[] follower : secondPage) {
            Long followerId = (Long) follower[0];
            assertThat(followerId).isGreaterThan(firstCursor);
        }
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 - 빈 결과 반환")
    void findWithNonExistentUserId_ReturnsEmptyResults() {
        // given
        Long nonExistentUserId = 99999L;

        // when & then
        List<Object[]> followers = followRepository.findFollowersByFollowingUserWithCursor(
                nonExistentUserId, 10, null);
        assertThat(followers).isEmpty();

        List<Object[]> followings = followRepository.findFollowingsByFollowerUserWithCursor(
                nonExistentUserId, 10, null);
        assertThat(followings).isEmpty();
    }

    @Test
    @DisplayName("팔로우 관계의 양방향성 확인 - A가 B를 팔로우해도 B가 A를 팔로우하지 않을 수 있음")
    void followRelationship_Bidirectional_VerifyAsymmetricRelation() {
        // given: follower1이 following1을 팔로우하는 상태

        // when & then: follower1 → following1 존재
        boolean follower1ToFollowing1 = followRepository.existsByFollowerUserAndFollowingUser(follower1, following1);
        assertThat(follower1ToFollowing1).isTrue();

        // when & then: following1 → follower1 존재하지 않음 (비대칭 관계)
        boolean following1ToFollower1 = followRepository.existsByFollowerUserAndFollowingUser(following1, follower1);
        assertThat(following1ToFollower1).isFalse();
    }

    @Test
    @DisplayName("대량 데이터 페이징 테스트 - 성능 및 정확성 검증")
    void largeDataPaging_PerformanceAndAccuracy_WorksCorrectly() {
        // given: 대량의 팔로워 생성 (10명)
        List<Member> manyFollowers = MemberFixture.createMultipleKbtMembers(10);
        manyFollowers.forEach(entityManager::persist);

        List<Follow> manyFollows = manyFollowers.stream()
                .map(follower -> FollowFixture.createFollow(follower, following2))
                .collect(Collectors.toList());
        manyFollows.forEach(entityManager::persist);
        entityManager.flush();

        // when: 작은 페이지 크기로 여러 번 조회
        List<Object[]> page1 = followRepository.findFollowersByFollowingUserWithCursor(
                following2.getId(), 3, null);

        Long cursor1 = (Long) page1.get(page1.size() - 1)[0];
        List<Object[]> page2 = followRepository.findFollowersByFollowingUserWithCursor(
                following2.getId(), 3, cursor1);

        Long cursor2 = (Long) page2.get(page2.size() - 1)[0];
        List<Object[]> page3 = followRepository.findFollowersByFollowingUserWithCursor(
                following2.getId(), 3, cursor2);

        // then: 페이지별 크기 및 중복 없음 확인
        assertThat(page1).hasSize(3);
        assertThat(page2).hasSize(3);
        assertThat(page3).isNotEmpty();

        // 모든 페이지의 ID가 증가 순서인지 확인
        assertThat((Long) page1.get(2)[0]).isLessThan((Long) page2.get(0)[0]);
        assertThat((Long) page2.get(2)[0]).isLessThan((Long) page3.get(0)[0]);
    }
}