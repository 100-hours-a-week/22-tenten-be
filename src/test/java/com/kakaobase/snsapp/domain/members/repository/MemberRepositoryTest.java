package com.kakaobase.snsapp.domain.members.repository;

import com.kakaobase.snsapp.annotation.RepositoryTest;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.fixture.members.MemberFixture;
import com.kakaobase.snsapp.stub.StubEmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * MemberRepository 통합 테스트
 *
 * MemberFixture와 StubEmailVerificationService를 활용한 테스트
 */
@RepositoryTest
@DisplayName("MemberRepository 통합 테스트")
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private List<Member> testMembers;
    private Member testMember;

    @BeforeEach
    void setUp() {
        // Repository 데이터 초기화
        memberRepository.deleteAll();
        testEntityManager.flush();    // DB에 즉시 반영
        testEntityManager.clear();    // 1차 캐시 완전 초기화

        // 테스트 데이터 설정
        setupTestData();
    }

    /**
     * 테스트용 기본 데이터 설정
     */
    void setupTestData() {
        // 기본 테스트 회원들 생성 (ID는 JPA가 자동 생성)
        List<Member> newMembers = List.of(
                MemberFixture.createMemberWithNickname("user1"),
                MemberFixture.createMemberWithNickname("user2"),
                MemberFixture.createMemberWithNickname("user3")
        );

        // 전체 회원 저장
        testMembers = memberRepository.saveAll(newMembers);

        testMember = testMembers.get(0); // 첫 번째 회원을 기본 테스트 대상으로 사용

        // 저장 후 flush로 즉시 반영
        memberRepository.flush();
    }

    // === 기본 CRUD 테스트 ===

    @Test
    @DisplayName("회원을 저장하고 조회할 수 있다")
    void save_AndFindById_Success() {
        // given
        Member newMember = MemberFixture.createDefaultMember();

        // when
        Member savedMember = memberRepository.save(newMember);
        Optional<Member> foundMember = memberRepository.findById(savedMember.getId());

        // then
        assertThat(savedMember.getId()).isNotNull();
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getEmail()).isEqualTo(newMember.getEmail());
        assertThat(foundMember.get().getNickname()).isEqualTo(newMember.getNickname());

    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional을 반환한다")
    void findById_WithNonExistentId_ReturnsEmpty() {
        // given
        Long nonExistentId = 999999L;

        // when
        Optional<Member> result = memberRepository.findById(nonExistentId);

        // then
        assertThat(result).isEmpty();
    }

    // === 이메일 관련 쿼리 테스트 ===

    @Test
    @DisplayName("이메일로 회원을 조회할 수 있다")
    void findByEmail_WithValidEmail_ReturnsMember() {
        // given
        String email = testMember.getEmail();

        // when
        Optional<Member> result = memberRepository.findByEmail(email);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(email);
        assertThat(result.get().getId()).isEqualTo(testMember.getId());

    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회 시 빈 Optional을 반환한다")
    void findByEmail_WithNonExistentEmail_ReturnsEmpty() {
        // given
        String nonExistentEmail = "nonexistent@example.com";

        // when
        Optional<Member> result = memberRepository.findByEmail(nonExistentEmail);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("이메일 존재 여부를 확인할 수 있다")
    void existsByEmail_WithValidEmail_ReturnsTrue() {
        // given
        String existingEmail = testMember.getEmail();

        // when
        boolean exists = memberRepository.existsByEmail(existingEmail);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 이메일의 존재 여부 확인 시 false를 반환한다")
    void existsByEmail_WithNonExistentEmail_ReturnsFalse() {
        // given
        String nonExistentEmail = "nonexistent@example.com";

        // when
        boolean exists = memberRepository.existsByEmail(nonExistentEmail);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("새로운 이메일로 회원 생성 후 인증 상태를 확인한다")
    void createMemberWithNewEmail_AndVerifyEmailStatus() {
        // given
        String newEmail = "newuser@example.com";
        Member newMember = MemberFixture.createMemberWithEmail(newEmail);

        // when
        Member savedMember = memberRepository.save(newMember);

        // then
        assertThat(savedMember.getId()).isNotNull();
        assertThat(memberRepository.existsByEmail(newEmail)).isTrue();
    }

    // === 닉네임 관련 쿼리 테스트 ===

    @Test
    @DisplayName("닉네임으로 회원을 조회할 수 있다")
    void findByNickname_WithValidNickname_ReturnsMember() {
        // given
        String nickname = testMember.getNickname();

        // when
        Optional<Member> result = memberRepository.findByNickname(nickname);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo(nickname);
        assertThat(result.get().getId()).isEqualTo(testMember.getId());
    }

    @Test
    @DisplayName("닉네임 존재 여부를 확인할 수 있다")
    void existsByNickname_WithValidNickname_ReturnsTrue() {
        // given
        String existingNickname = testMember.getNickname();

        // when
        boolean exists = memberRepository.existsByNickname(existingNickname);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 닉네임의 존재 여부 확인 시 false를 반환한다")
    void existsByNickname_WithNonExistentNickname_ReturnsFalse() {
        // given
        String nonExistentNickname = "nonexistentnickname";

        // when
        boolean exists = memberRepository.existsByNickname(nonExistentNickname);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("닉네임 부분 검색이 동작한다")
    void searchByNickname_WithPartialNickname_ReturnsMatchingMembers() {
        // given
        String partialNickname = "user"; // "user1", "user2", "user3" 매칭 예상

        // when
        List<Member> results = memberRepository.searchByNickname(partialNickname);

        // then
        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(member ->
                assertThat(member.getNickname()).containsIgnoringCase(partialNickname));

    }

    @Test
    @DisplayName("닉네임 목록으로 회원들을 일괄 조회할 수 있다")
    void findAllByNicknameIn_WithNicknameList_ReturnsMatchingMembers() {
        // given
        List<String> nicknames = testMembers.stream()
                .limit(2)
                .map(Member::getNickname)
                .toList();

        // when
        List<Member> results = memberRepository.findAllByNicknameIn(nicknames);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).allSatisfy(member ->
                assertThat(nicknames).contains(member.getNickname()));
    }

    @ParameterizedTest
    @DisplayName("닉네임 부분 검색 시 결과 수를 제한할 수 있다")
    @CsvSource({
            "user, 2",
            "admin, 1",
            "bot, 1"
    })
    void findByNicknameContainingLimit_WithLimitedResults_ReturnsCorrectSize(
            String nickname, int limit) {
        // when
        List<Member> results = memberRepository.findByNicknameContainingLimit(nickname, limit);

        // then
        assertThat(results).hasSizeLessThanOrEqualTo(limit);
        if (!results.isEmpty()) {
            assertThat(results).allSatisfy(member ->
                    assertThat(member.getNickname().toLowerCase()).contains(nickname.toLowerCase()));
        }
    }

    // === 기수 관련 쿼리 테스트 ===

    @ParameterizedTest
    @DisplayName("기수별로 회원을 조회할 수 있다")
    @EnumSource(Member.ClassName.class)
    void findByClassName_WithValidClassName_ReturnsMembers(Member.ClassName className) {
        // given - 해당 기수의 회원 추가 생성
        Member memberWithClass = MemberFixture.createMemberWithClassName(
                "class-test@example.com", className);
        memberRepository.save(memberWithClass);

        // when
        List<Member> results = memberRepository.findByClassName(className);

        // then
        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(member ->
                assertThat(member.getClassName()).isEqualTo(String.valueOf(className)));
    }

    // === ID 목록 조회 테스트 ===

    @Test
    @DisplayName("ID 목록으로 회원들을 일괄 조회할 수 있다")
    void findByIdIn_WithValidIds_ReturnsMatchingMembers() {
        // given
        List<Long> ids = testMembers.stream()
                .limit(2)
                .map(Member::getId)
                .toList();

        // when
        List<Member> results = memberRepository.findByIdIn(ids);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).allSatisfy(member ->
                assertThat(ids).contains(member.getId()));
    }

    @Test
    @DisplayName("findAllByIdIn으로 회원들을 일괄 조회할 수 있다")
    void findAllByIdIn_WithValidIds_ReturnsMatchingMembers() {
        // given
        List<Long> existingIds = testMembers.stream()
                .map(Member::getId)
                .toList();

        // 최소 2개 이상의 회원이 있는지 확인
        assertThat(existingIds).hasSizeGreaterThanOrEqualTo(2);

        // 기존 ID 2개 + 존재하지 않는 ID 1개로 새 리스트 생성
        List<Long> ids = List.of(existingIds.get(0), existingIds.get(1), 999999L);

        // when
        List<Member> results = memberRepository.findAllByIdIn(ids);

        // then
        assertThat(results).hasSize(2); // 존재하지 않는 ID 제외
        assertThat(results).allSatisfy(member ->
                assertThat(ids).contains(member.getId()));
    }

    @Test
    @DisplayName("빈 ID 목록으로 조회 시 빈 리스트를 반환한다")
    void findAllByIdIn_WithEmptyList_ReturnsEmptyList() {
        // given
        List<Long> emptyIds = List.of();

        // when
        List<Member> results = memberRepository.findAllByIdIn(emptyIds);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("MemberFixture의 createMembers로 생성한 회원들을 일괄 조회할 수 있다")
    void findAllByIdIn_WithFixtureCreatedMembers_Success() {
        // given
        List<Member> savedMembers = testMembers; // 👈 이렇게 변경해도 OK

        List<Long> ids = savedMembers.stream().map(Member::getId).toList();

        // when
        List<Member> results = memberRepository.findAllByIdIn(ids);

        // then
        assertThat(results).hasSize(3);
        assertThat(results).allSatisfy(member -> {
            assertThat(ids).contains(member.getId());
        });
    }

    // === 프로필 조회 테스트 ===

    @Test
    @DisplayName("프로필 정보를 조회할 수 있다")
    void findProfileById_WithValidId_ReturnsProfile() {
        // given
        Long validId = testMember.getId();

        // when
        Optional<Member> result = memberRepository.findProfileById(validId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(validId);
        assertThat(result.get().getNickname()).isNotNull();
        assertThat(result.get().getEmail()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 ID로 프로필 조회 시 빈 Optional을 반환한다")
    void findProfileById_WithNonExistentId_ReturnsEmpty() {
        // given
        Long nonExistentId = 999999L;

        // when
        Optional<Member> result = memberRepository.findProfileById(nonExistentId);

        // then
        assertThat(result).isEmpty();
    }

    // === 성능 및 정렬 테스트 ===

    @Test
    @DisplayName("닉네임 검색 결과가 알파벳 순으로 정렬된다")
    void findByNicknameContainingLimit_ResultsAreSorted() {
        // given
        String searchTerm = "user";
        int limit = 10;

        // when
        List<Member> results = memberRepository.findByNicknameContainingLimit(searchTerm, limit);

        // then
        if (results.size() > 1) {
            for (int i = 0; i < results.size() - 1; i++) {
                String currentNickname = results.get(i).getNickname();
                String nextNickname = results.get(i + 1).getNickname();
                assertThat(currentNickname.compareToIgnoreCase(nextNickname))
                        .isLessThanOrEqualTo(0);
            }
        }
    }

    // === 대소문자 구분 테스트 ===

    @Test
    @DisplayName("닉네임 검색 시 대소문자를 구분하지 않는다")
    void findByNicknameContainingLimit_CaseInsensitive() {
        // given
        String lowerCase = "user";
        String upperCase = "USER";
        String mixedCase = "User";

        // when
        List<Member> lowerResults = memberRepository.findByNicknameContainingLimit(lowerCase, 10);
        List<Member> upperResults = memberRepository.findByNicknameContainingLimit(upperCase, 10);
        List<Member> mixedResults = memberRepository.findByNicknameContainingLimit(mixedCase, 10);

        // then
        assertThat(lowerResults).hasSameSizeAs(upperResults);
        assertThat(lowerResults).hasSameSizeAs(mixedResults);

        // 결과의 ID가 동일한지 확인 (순서는 같아야 함)
        if (!lowerResults.isEmpty()) {
            List<Long> lowerIds = lowerResults.stream().map(Member::getId).toList();
            List<Long> upperIds = upperResults.stream().map(Member::getId).toList();
            List<Long> mixedIds = mixedResults.stream().map(Member::getId).toList();

            assertThat(lowerIds).isEqualTo(upperIds);
            assertThat(lowerIds).isEqualTo(mixedIds);
        }
    }

    // === 데이터 무결성 및 이메일 인증 연동 테스트 ===

    @Test
    @DisplayName("삭제된 회원은 조회되지 않는다")
    void findByEmail_WithDeletedMember_ReturnsEmpty() {
        // given
        String deletedEmail = "deleted@example.com";
        Member member = MemberFixture.createMemberWithEmail(deletedEmail);
        member = memberRepository.save(member);

        // when - 회원 삭제
        memberRepository.deleteById(member.getId());
        memberRepository.flush();

        // then
        Optional<Member> result = memberRepository.findByEmail(deletedEmail);
        assertThat(result).isEmpty();
    }


    @Test
    @DisplayName("다양한 조건으로 회원 목록을 조회할 수 있다")
    void complexQueryTest() {
        // given - MemberFixture의 다양한 생성 메서드 활용
        List<Member> additionalMembers = List.of(
                MemberFixture.createMemberWithClassName("jeju1@example.com", Member.ClassName.JEJU_1),
                MemberFixture.createMemberWithClassName("jeju2@example.com", Member.ClassName.JEJU_2),
                MemberFixture.createMemberWithClassName("pangyo1@example.com", Member.ClassName.PANGYO_1)
        );

        memberRepository.saveAll(additionalMembers);

        // when & then
        // 1. 기수별 조회
        List<Member> jejuMembers = memberRepository.findByClassName(Member.ClassName.JEJU_1);
        assertThat(jejuMembers).isNotEmpty();

        // 2. 닉네임 검색
        List<Member> searchResults = memberRepository.searchByNickname("user");
        assertThat(searchResults).isNotEmpty();

        // 3. ID 목록 조회
        List<Long> allIds = memberRepository.findAll().stream().map(Member::getId).toList();
        List<Member> allByIds = memberRepository.findAllByIdIn(allIds);
        assertThat(allByIds).hasSizeGreaterThanOrEqualTo(testMembers.size());
    }

    // === 예외 상황 테스트 ===

    @Test
    @DisplayName("빈 문자열로 검색 시 적절히 처리된다")
    void handleEmptyStrings() {
        // when & then
        assertThat(memberRepository.searchByName("")).isEmpty();
        assertThat(memberRepository.searchByNickname("")).isEmpty();
        assertThat(memberRepository.findByNicknameContainingLimit("", 10)).isEmpty();

    }

    // === MemberFixture 특화 테스트 ===

    @Test
    @DisplayName("MemberFixture로 생성한 팔로우 카운트 정보를 조회할 수 있다")
    void findMemberWithFollowCounts() {
        // given
        Member memberWithFollows = MemberFixture.createMemberWithFollowCounts(50, 25);
        Member savedMember = memberRepository.save(memberWithFollows);

        // when
        Optional<Member> result = memberRepository.findById(savedMember.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getFollowerCount()).isEqualTo(50);
        assertThat(result.get().getFollowingCount()).isEqualTo(25);
    }

    @Test
    @DisplayName("MemberFixture로 생성한 프로필 이미지가 있는 회원을 조회할 수 있다")
    void findMemberWithProfileImage() {
        // given
        String profileImageUrl = "https://s3.amazonaws.com/bucket/test-profile.jpg";
        Member memberWithImage = MemberFixture.createMemberWithProfileImage(profileImageUrl);
        Member savedMember = memberRepository.save(memberWithImage);

        // when
        Optional<Member> result = memberRepository.findById(savedMember.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getProfileImgUrl()).isEqualTo(profileImageUrl);
    }

    @Test
    @DisplayName("MemberFixture로 생성한 다양한 기수의 회원들을 조회할 수 있다")
    void findMembersWithDifferentClasses() {
        // given
        List<Member> membersWithDifferentClasses = MemberFixture.createMembersWithDifferentClasses();

        // 이메일 인증 후 저장
        List<Member> savedMembers = memberRepository.saveAll(membersWithDifferentClasses);

        // when & then
        for (Member savedMember : savedMembers) {
            Optional<Member> result = memberRepository.findById(savedMember.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getClassName()).isNotNull();
        }
    }

    @Test
    @DisplayName("MemberFixture로 생성한 밴된 회원을 조회할 수 있다")
    void findBannedMember() {
        // given
        Member bannedMember = MemberFixture.createBannedMemberWithNickname("banneduser");
        Member savedMember = memberRepository.save(bannedMember);

        // when
        Optional<Member> result = memberRepository.findById(savedMember.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("banneduser");
        assertThat(result.get().getIsBanned()).isTrue();
    }

    @Test
    @DisplayName("MemberFixture로 생성한 닉네임과 프로필 이미지가 모두 설정된 회원을 조회할 수 있다")
    void findMemberWithNicknameAndProfileImage() {
        // given
        String profileImageUrl = "https://s3.amazonaws.com/bucket/special-profile.jpg";
        Member memberWithBoth = MemberFixture.createMemberWithNicknameAndProfileImage("specialuser", profileImageUrl);
        Member savedMember = memberRepository.save(memberWithBoth);

        // when
        Optional<Member> result = memberRepository.findById(savedMember.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("specialuser");
        assertThat(result.get().getProfileImgUrl()).isEqualTo(profileImageUrl);
    }


    @Test
    @DisplayName("MemberFixture로 생성한 팔로우 카운트와 닉네임이 모두 설정된 회원을 조회할 수 있다")
    void findMemberWithNicknameAndFollowCounts() {
        // given
        Member memberWithBoth = MemberFixture.createMemberWithNicknameAndFollowCounts("followuser", 100, 50);
        Member savedMember = memberRepository.save(memberWithBoth);

        // when
        Optional<Member> result = memberRepository.findById(savedMember.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("followuser");
        assertThat(result.get().getFollowerCount()).isEqualTo(100);
        assertThat(result.get().getFollowingCount()).isEqualTo(50);
    }

    @Test
    @DisplayName("MemberFixture로 대량의 회원 데이터를 생성하고 조회할 수 있다")
    void handleLargeDataSet() {
        // given
        int memberCount = 50;
        List<Member> largeDataSet = MemberFixture.createMembers(memberCount);

        List<Member> savedMembers = memberRepository.saveAll(largeDataSet);

        // when
        List<Long> allIds = savedMembers.stream().map(Member::getId).toList();
        List<Member> retrievedMembers = memberRepository.findAllByIdIn(allIds);

        // then
        assertThat(retrievedMembers).hasSize(memberCount);
        assertThat(retrievedMembers).allSatisfy(member -> {
            assertThat(member.getId()).isNotNull();
            assertThat(member.getNickname()).startsWith("example");
        });
    }

    @Test
    @DisplayName("MemberFixture로 생성한 회원들의 닉네임 검색이 정상 동작한다")
    void searchMembersByNicknameFromFixture() {
        // given
        List<Member> searchTestMembers = List.of(
                MemberFixture.createMemberWithNickname("searchuser1"),
                MemberFixture.createMemberWithNickname("searchuser2"),
                MemberFixture.createMemberWithNickname("testuser")
        );

        memberRepository.saveAll(searchTestMembers);

        // when
        List<Member> searchResults = memberRepository.searchByNickname("search");

        // then
        assertThat(searchResults).hasSizeGreaterThanOrEqualTo(2);
        assertThat(searchResults).allSatisfy(member -> {
            assertThat(member.getNickname()).containsIgnoringCase("search");
        });
    }
}