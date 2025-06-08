package com.kakaobase.snsapp.domain.members.service;

import com.kakaobase.snsapp.annotation.ServiceTest;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.members.dto.MemberRequestDto;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.exception.MemberErrorCode;
import com.kakaobase.snsapp.domain.members.exception.MemberException;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.fixture.auth.CustomUserDetailsFixture;
import com.kakaobase.snsapp.fixture.members.MemberFixture;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * MemberService 프로필 관리 기능 테스트
 */
@ServiceTest
@DisplayName("MemberService 프로필 관리 테스트")
class MemberServiceProfileTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MemberService memberService;

    private Member member;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        member = MemberFixture.createDefaultMember();
        ReflectionTestUtils.setField(member, "id", 1L); // Mock 테스트용 ID 설정
        userDetails = new CustomUserDetailsFixture().createDefaultJwtUser();
    }

    // === SecurityContext가 필요하지 않은 조회 기능 테스트 ===

    @Test
    @DisplayName("회원 ID로 회원 정보를 조회한다")
    void getMemberInfo_WithValidId_ReturnsInfo() {
        // given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        Map<String, String> result = memberService.getMemberInfo(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).containsKeys("nickname", "imageUrl");
        assertThat(result.get("nickname")).isEqualTo(member.getNickname());
        assertThat(result.get("imageUrl")).isEqualTo(member.getProfileImgUrl());

        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 조회 시 예외가 발생한다")
    void getMemberInfo_WithInvalidId_ThrowsException() {
        // given
        Long invalidId = 999L;
        given(memberRepository.findById(invalidId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMemberInfo(invalidId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.MEMBER_NOT_FOUND);

        verify(memberRepository).findById(invalidId);
    }

    @ParameterizedTest
    @DisplayName("여러 회원 ID로 일괄 조회가 성공한다")
    @ValueSource(longs = {1L, 2L, 3L, 999L})
    void getMemberInfoMapByIds_WithValidIds_ReturnsMap(Long id) {
        // given
        List<Long> memberIds = List.of(id);
        Member testMember = MemberFixture.createMemberWithNickname("user" + id);
        ReflectionTestUtils.setField(testMember, "id", id); // Mock 테스트용 ID 설정
        given(memberRepository.findAllByIdIn(memberIds)).willReturn(List.of(testMember));

        // when
        Map<Long, Map<String, String>> result = memberService.getMemberInfoMapByIds(memberIds);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(id);

        Map<String, String> memberInfo = result.get(id);
        assertThat(memberInfo).containsKeys("nickname", "imageUrl");
        assertThat(memberInfo.get("nickname")).isEqualTo(testMember.getNickname());

        verify(memberRepository).findAllByIdIn(memberIds);
    }

    @Test
    @DisplayName("빈 ID 목록으로 일괄 조회 시 빈 맵을 반환한다")
    void getMemberInfoMapByIds_WithEmptyList_ReturnsEmptyMap() {
        // given
        List<Long> emptyList = List.of();
        given(memberRepository.findAllByIdIn(emptyList)).willReturn(List.of());

        // when
        Map<Long, Map<String, String>> result = memberService.getMemberInfoMapByIds(emptyList);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(memberRepository).findAllByIdIn(emptyList);
    }

    @Test
    @DisplayName("회원 존재 여부를 확인한다")
    void existsById_WithValidId_ReturnsTrue() {
        // given
        Long memberId = 1L;
        given(memberRepository.existsById(memberId)).willReturn(true);

        // when
        boolean result = memberService.existsById(memberId);

        // then
        assertThat(result).isTrue();
        verify(memberRepository).existsById(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 존재 여부 확인 시 false를 반환한다")
    void existsById_WithInvalidId_ReturnsFalse() {
        // given
        Long invalidId = 999L;
        given(memberRepository.existsById(invalidId)).willReturn(false);

        // when
        boolean result = memberService.existsById(invalidId);

        // then
        assertThat(result).isFalse();
        verify(memberRepository).existsById(invalidId);
    }

    @Test
    @DisplayName("회원의 기수 정보를 조회한다")
    void getMemberClassName_WithValidId_ReturnsClassName() {
        // given
        Long memberId = 1L;
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        // when
        String result = memberService.getMemberClassName(memberId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(member.getClassName().toString());

        verify(memberRepository).findById(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 기수 조회 시 예외가 발생한다")
    void getMemberClassName_WithInvalidId_ThrowsException() {
        // given
        Long invalidId = 999L;
        given(memberRepository.findById(invalidId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMemberClassName(invalidId))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.MEMBER_NOT_FOUND);

        verify(memberRepository).findById(invalidId);
    }

    @ParameterizedTest
    @DisplayName("다양한 개수의 회원 ID 목록으로 일괄 조회가 성공한다")
    @CsvSource({
            "1, 1",
            "3, 3",
            "5, 5",
            "10, 10"
    })
    void getMemberInfoMapByIds_WithDifferentSizes_ReturnsCorrectSize(int memberCount, int expectedSize) {
        // given
        List<Member> members = MemberFixture.createMembers(memberCount);

        // Mock 테스트용으로 ID 설정
        for (int i = 0; i < members.size(); i++) {
            ReflectionTestUtils.setField(members.get(i), "id", (long) (i + 1));
        }

        List<Long> memberIds = members.stream()
                .map(Member::getId)
                .toList();

        given(memberRepository.findAllByIdIn(memberIds)).willReturn(members);

        // when
        Map<Long, Map<String, String>> result = memberService.getMemberInfoMapByIds(memberIds);

        // then
        assertThat(result).hasSize(expectedSize);
        verify(memberRepository).findAllByIdIn(memberIds);
    }

    // === SecurityContext가 필요한 프로필 수정 기능 테스트 ===

    @Test
    @DisplayName("GitHub URL 변경이 성공한다")
    void changeGithubUrl_WithValidUrl_Success() {
        // given
        MemberRequestDto.GithubUrlChange request =
                new MemberRequestDto.GithubUrlChange("https://github.com/newuser");

        setupSecurityContext();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when & then
        assertThatCode(() -> memberService.changGithubUrl(request))
                .doesNotThrowAnyException();

        // verify
        verify(memberRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 GitHub URL 변경 시 예외가 발생한다")
    void changeGithubUrl_WithNonExistentUser_ThrowsException() {
        // given
        MemberRequestDto.GithubUrlChange request =
                new MemberRequestDto.GithubUrlChange("https://github.com/test");

        setupSecurityContext();
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.changGithubUrl(request))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", GeneralErrorCode.RESOURCE_NOT_FOUND);

        verify(memberRepository).findById(1L);
    }

    @ParameterizedTest
    @DisplayName("유효한 GitHub URL 형식으로 변경이 성공한다")
    @ValueSource(strings = {
            "https://github.com/user1",
            "https://github.com/test-user",
            "https://github.com/user_123",
            "https://github.com/user123/repository"
    })
    void changeGithubUrl_WithValidFormats_Success(String githubUrl) {
        // given
        MemberRequestDto.GithubUrlChange request =
                new MemberRequestDto.GithubUrlChange(githubUrl);

        setupSecurityContext();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when & then
        assertThatCode(() -> memberService.changGithubUrl(request))
                .doesNotThrowAnyException();

        verify(memberRepository).findById(1L);
    }

    @Test
    @DisplayName("프로필 이미지 변경이 성공한다")
    void changeProfileImage_WithValidUrl_Success() {
        // given
        MemberRequestDto.ProfileImageChange request =
                new MemberRequestDto.ProfileImageChange("https://s3.amazonaws.com/bucket/profile.jpg");

        setupSecurityContext();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when
        MemberResponseDto.ProfileImageChange result = memberService.changProfileImageUrl(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.profileImageUrl()).isEqualTo(request.imageUrl());

        verify(memberRepository).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 프로필 이미지 변경 시 예외가 발생한다")
    void changeProfileImage_WithNonExistentUser_ThrowsException() {
        // given
        MemberRequestDto.ProfileImageChange request =
                new MemberRequestDto.ProfileImageChange("https://s3.amazonaws.com/bucket/new.jpg");

        setupSecurityContext();
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.changProfileImageUrl(request))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", GeneralErrorCode.RESOURCE_NOT_FOUND);

        verify(memberRepository).findById(1L);
    }

    @ParameterizedTest
    @DisplayName("유효한 S3 이미지 URL로 변경이 성공한다")
    @ValueSource(strings = {
            "https://s3.amazonaws.com/bucket/uploads/profile1.jpg",
            "https://s3.amazonaws.com/bucket/uploads/user123.png",
            "https://s3.amazonaws.com/bucket/uploads/avatar.jpeg"
    })
    void changeProfileImage_WithValidS3Urls_Success(String imageUrl) {
        // given
        MemberRequestDto.ProfileImageChange request =
                new MemberRequestDto.ProfileImageChange(imageUrl);

        setupSecurityContext();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when
        MemberResponseDto.ProfileImageChange result = memberService.changProfileImageUrl(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.profileImageUrl()).isEqualTo(imageUrl);

        verify(memberRepository).findById(1L);
    }

    @Test
    @DisplayName("다양한 기수의 회원들을 조회할 수 있다")
    void getMemberInfo_WithDifferentClasses_Success() {
        // given
        List<Member> membersWithDifferentClasses = MemberFixture.createMembersWithDifferentClasses();

        for (int i = 0; i < membersWithDifferentClasses.size(); i++) {
            Member testMember = membersWithDifferentClasses.get(i);
            Long testId = (long) (i + 1);
            ReflectionTestUtils.setField(testMember, "id", testId); // Mock 테스트용 ID 설정

            given(memberRepository.findById(testId)).willReturn(Optional.of(testMember));

            // when
            Map<String, String> result = memberService.getMemberInfo(testId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.get("nickname")).isEqualTo(testMember.getNickname());

            verify(memberRepository).findById(testId);
        }
    }

    @Test
    @DisplayName("팔로우 카운트가 설정된 회원 정보를 조회한다")
    void getMemberInfo_WithFollowCounts_Success() {
        // given
        Member memberWithFollows = MemberFixture.createMemberWithFollowCounts(100, 50);
        ReflectionTestUtils.setField(memberWithFollows, "id", 2L); // Mock 테스트용 ID 설정
        given(memberRepository.findById(2L)).willReturn(Optional.of(memberWithFollows));

        // when
        Map<String, String> result = memberService.getMemberInfo(2L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("nickname")).isEqualTo(memberWithFollows.getNickname());

        verify(memberRepository).findById(2L);
    }

    @Test
    @DisplayName("프로필 이미지가 설정된 회원 정보를 조회한다")
    void getMemberInfo_WithProfileImage_Success() {
        // given
        String profileImageUrl = "https://s3.amazonaws.com/bucket/custom-profile.jpg";
        Member memberWithImage = MemberFixture.createMemberWithProfileImage(profileImageUrl);
        ReflectionTestUtils.setField(memberWithImage, "id", 3L); // Mock 테스트용 ID 설정

        given(memberRepository.findById(3L)).willReturn(Optional.of(memberWithImage));

        // when
        Map<String, String> result = memberService.getMemberInfo(3L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("imageUrl")).isEqualTo(profileImageUrl);

        verify(memberRepository).findById(3L);
    }

    // === 예외 케이스 테스트 ===

    @Test
    @DisplayName("null 요청으로 GitHub URL 변경 시 예외가 발생한다")
    void changeGithubUrl_WithNullRequest_ThrowsException() {
        // given
        setupSecurityContext();

        // when & then
        assertThatThrownBy(() -> memberService.changGithubUrl(null))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", GeneralErrorCode.RESOURCE_NOT_FOUND)
                .hasMessageContaining("요청한 리소스를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("null 요청으로 프로필 이미지 변경 시 예외가 발생한다")
    void changeProfileImage_WithNullRequest_ThrowsException() {
        // given
        setupSecurityContext();

        // when & then
        assertThatThrownBy(() -> memberService.changProfileImageUrl(null))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", GeneralErrorCode.RESOURCE_NOT_FOUND)
                .hasMessageContaining("요청한 리소스를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("커스텀 회원 정보 조회가 성공한다")
    void getMemberInfo_WithCustomMember_Success() {
        // given
        Member customMember = MemberFixture.createCustomMember(
                "custom@example.com",
                "customuser",
                "커스텀유저",
                Member.ClassName.JEJU_3
        );
        ReflectionTestUtils.setField(customMember, "id", 5L); // Mock 테스트용 ID 설정
        given(memberRepository.findById(5L)).willReturn(Optional.of(customMember));

        // when
        Map<String, String> result = memberService.getMemberInfo(5L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("nickname")).isEqualTo("customuser");

        verify(memberRepository).findById(5L);
    }

    @Test
    @DisplayName("밴된 회원 정보를 조회할 수 있다")
    void getMemberInfo_WithBannedMember_Success() {
        // given
        Member bannedMember = MemberFixture.createBannedMemberWithNickname("banneduser");
        ReflectionTestUtils.setField(bannedMember, "id", 6L); // Mock 테스트용 ID 설정
        given(memberRepository.findById(6L)).willReturn(Optional.of(bannedMember));

        // when
        Map<String, String> result = memberService.getMemberInfo(6L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("nickname")).isEqualTo("banneduser");

        verify(memberRepository).findById(6L);
    }

    @Test
    @DisplayName("닉네임과 프로필 이미지가 모두 설정된 회원 정보를 조회한다")
    void getMemberInfo_WithNicknameAndProfileImage_Success() {
        // given
        String profileImageUrl = "https://s3.amazonaws.com/bucket/special-profile.jpg";
        Member memberWithBoth = MemberFixture.createMemberWithNicknameAndProfileImage("specialuser", profileImageUrl);
        ReflectionTestUtils.setField(memberWithBoth, "id", 7L); // Mock 테스트용 ID 설정
        given(memberRepository.findById(7L)).willReturn(Optional.of(memberWithBoth));

        // when
        Map<String, String> result = memberService.getMemberInfo(7L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("nickname")).isEqualTo("specialuser");
        assertThat(result.get("imageUrl")).isEqualTo(profileImageUrl);

        verify(memberRepository).findById(7L);
    }

    // === SecurityContext 설정 헬퍼 메서드 ===

    /**
     * SecurityContext가 필요한 테스트에서만 호출하는 설정 메서드
     */
    private void setupSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(userDetails);
    }
}