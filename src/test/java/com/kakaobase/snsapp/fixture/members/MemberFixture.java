package com.kakaobase.snsapp.fixture.members;

import com.kakaobase.snsapp.domain.members.entity.Member;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Member 엔티티 테스트 데이터 생성을 위한 Fixture 클래스
 *
 * 제공 기능:
 * - 기본 Member 객체 생성 (ID 자동 생성)
 * - 커스텀 속성이 설정된 Member 객체 생성
 * - 여러 Member 객체 일괄 생성
 *
 * 주의사항:
 * - ID는 JPA @GeneratedValue에 의해 자동 생성됩니다
 * - 특정 ID가 필요한 경우 저장 후 ID를 확인하여 사용하세요
 */
public class MemberFixture {

    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_NAME = "테스트유저";
    private static final String DEFAULT_NICKNAME = "testuser";
    private static final String DEFAULT_PASSWORD = "encodedPassword123!";
    private static final Member.ClassName DEFAULT_CLASS_NAME = Member.ClassName.PANGYO_1;
    private static final String DEFAULT_GITHUB_URL = "https://github.com/testuser";
    private static final String DEFAULT_PROFILE_IMG_URL = "https://example.com/profile.jpg";

    /**
     * 기본 설정으로 Member 객체 생성
     *
     * @return 기본 Member 객체
     */
    public static Member createDefaultMember() {
        return Member.builder()
                .email(DEFAULT_EMAIL)
                .name(DEFAULT_NAME)
                .nickname(DEFAULT_NICKNAME)
                .password(DEFAULT_PASSWORD)
                .className(DEFAULT_CLASS_NAME)
                .githubUrl(DEFAULT_GITHUB_URL)
                .build();
    }

    /**
     * 지정된 닉네임을 가진 Member 객체 생성
     * 이메일도 닉네임에 맞춰 자동 생성
     * ID는 저장 시점에 자동 생성됩니다
     *
     * @param nickname 설정할 닉네임
     * @return 커스텀 닉네임을 가진 Member 객체
     */
    public static Member createMemberWithNickname(String nickname) {
        return Member.builder()
                .email(nickname + "@example.com")
                .name("테스트유저_" + nickname)
                .nickname(nickname)
                .password(DEFAULT_PASSWORD)
                .className(DEFAULT_CLASS_NAME)
                .githubUrl("https://github.com/" + nickname)
                .build();
    }

    /**
     * 지정된 이메일을 가진 Member 객체 생성
     * ID는 저장 시점에 자동 생성됩니다
     *
     * @param email 설정할 이메일
     * @return 커스텀 이메일을 가진 Member 객체
     */
    public static Member createMemberWithEmail(String email) {
        return Member.builder()
                .email(email)
                .name(DEFAULT_NAME)
                .nickname(DEFAULT_NICKNAME)
                .password(DEFAULT_PASSWORD)
                .className(DEFAULT_CLASS_NAME)
                .githubUrl(DEFAULT_GITHUB_URL)
                .build();
    }

    /**
     * 지정된 기수를 가진 Member 객체 생성
     * ID는 저장 시점에 자동 생성됩니다
     *
     * @param email 설정할 이메일
     * @param className 설정할 기수
     * @return 커스텀 기수를 가진 Member 객체
     */
    public static Member createMemberWithClassName(String email, Member.ClassName className) {
        return Member.builder()
                .email(email)
                .name(DEFAULT_NAME)
                .nickname(DEFAULT_NICKNAME)
                .password(DEFAULT_PASSWORD)
                .className(className)
                .githubUrl(DEFAULT_GITHUB_URL)
                .build();
    }

    /**
     * 팔로우 카운트가 설정된 Member 객체 생성
     * ID는 저장 시점에 자동 생성됩니다
     *
     * @param followerCount 팔로워 수
     * @param followingCount 팔로잉 수
     * @return 팔로우 카운트가 설정된 Member 객체
     */
    public static Member createMemberWithFollowCounts(Integer followerCount, Integer followingCount) {
        Member member = createDefaultMember();
        ReflectionTestUtils.setField(member, "followerCount", followerCount);
        ReflectionTestUtils.setField(member, "followingCount", followingCount);
        return member;
    }

    /**
     * 팔로우 카운트와 닉네임이 설정된 Member 객체 생성
     * ID는 저장 시점에 자동 생성됩니다
     *
     * @param nickname 설정할 닉네임
     * @param followerCount 팔로워 수
     * @param followingCount 팔로잉 수
     * @return 팔로우 카운트와 닉네임이 설정된 Member 객체
     */
    public static Member createMemberWithNicknameAndFollowCounts(String nickname, Integer followerCount, Integer followingCount) {
        Member member = createMemberWithNickname(nickname);
        ReflectionTestUtils.setField(member, "followerCount", followerCount);
        ReflectionTestUtils.setField(member, "followingCount", followingCount);
        return member;
    }

    /**
     * 완전히 커스터마이징된 Member 객체 생성
     * ID는 저장 시점에 자동 생성됩니다
     *
     * @param email 이메일
     * @param nickname 닉네임
     * @param name 이름
     * @param className 기수
     * @return 완전히 커스터마이징된 Member 객체
     */
    public static Member createCustomMember(String email, String nickname, String name, Member.ClassName className) {
        return Member.builder()
                .email(email)
                .name(name)
                .nickname(nickname)
                .password(DEFAULT_PASSWORD)
                .className(className)
                .githubUrl("https://github.com/" + nickname)
                .build();
    }

    /**
     * 지정된 개수만큼 Member 객체들을 생성
     * ID는 각 객체 저장 시점에 자동 생성됩니다
     *
     * @param count 생성할 Member 개수
     * @return Member 객체 리스트
     */
    public static List<Member> createMembers(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> createMemberWithNickname("example" + i))
                .toList();
    }

    /**
     * 서로 다른 기수의 Member 객체들을 생성
     * ID는 각 객체 저장 시점에 자동 생성됩니다
     *
     * @return 다양한 기수의 Member 리스트
     */
    public static List<Member> createMembersWithDifferentClasses() {
        return List.of(
                createCustomMember("pangyo1@example.com", "pangyo1", "판교1기", Member.ClassName.PANGYO_1),
                createCustomMember("pangyo2@example.com", "pangyo2", "판교2기", Member.ClassName.PANGYO_2),
                createCustomMember("jeju1@example.com", "jeju1", "제주1기", Member.ClassName.JEJU_1),
                createCustomMember("jeju2@example.com", "jeju2", "제주2기", Member.ClassName.JEJU_2),
                createCustomMember("jeju3@example.com", "jeju3", "제주3기", Member.ClassName.JEJU_3)
        );
    }

    /**
     * 비활성화된 Member 객체 생성 (밴 상태)
     * ID는 저장 시점에 자동 생성됩니다
     *
     * @return 비활성화된 Member 객체
     */
    public static Member createBannedMember() {
        Member member = createDefaultMember();
        ReflectionTestUtils.setField(member, "isBanned", true);
        return member;
    }

    /**
     * 비활성화된 Member 객체 생성 (닉네임 지정)
     * ID는 저장 시점에 자동 생성됩니다
     *
     * @param nickname 설정할 닉네임
     * @return 비활성화된 Member 객체
     */
    public static Member createBannedMemberWithNickname(String nickname) {
        Member member = createMemberWithNickname(nickname);
        ReflectionTestUtils.setField(member, "isBanned", true);
        return member;
    }

    /**
     * 프로필 이미지가 설정된 Member 객체 생성
     * ID는 저장 시점에 자동 생성됩니다
     *
     * @param profileImgUrl 프로필 이미지 URL
     * @return 프로필 이미지가 설정된 Member 객체
     */
    public static Member createMemberWithProfileImage(String profileImgUrl) {
        Member member = createDefaultMember();
        ReflectionTestUtils.setField(member, "profileImgUrl", profileImgUrl);
        return member;
    }

    /**
     * 프로필 이미지와 닉네임이 설정된 Member 객체 생성
     * ID는 저장 시점에 자동 생성됩니다
     *
     * @param nickname 설정할 닉네임
     * @param profileImgUrl 프로필 이미지 URL
     * @return 프로필 이미지와 닉네임이 설정된 Member 객체
     */
    public static Member createMemberWithNicknameAndProfileImage(String nickname, String profileImgUrl) {
        Member member = createMemberWithNickname(nickname);
        ReflectionTestUtils.setField(member, "profileImgUrl", profileImgUrl);
        return member;
    }
}