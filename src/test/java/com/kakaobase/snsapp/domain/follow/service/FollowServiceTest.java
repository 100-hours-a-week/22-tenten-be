package com.kakaobase.snsapp.domain.follow.service;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.follow.converter.FollowConverter;
import com.kakaobase.snsapp.domain.follow.dto.FollowResponse;
import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.follow.exception.FollowErrorCode;
import com.kakaobase.snsapp.domain.follow.exception.FollowException;
import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.fixture.follow.FollowFixture;
import com.kakaobase.snsapp.global.fixture.member.CustomUserDetailsFixture;
import com.kakaobase.snsapp.global.fixture.member.MemberFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowService 단위 테스트")
class FollowServiceTest {

    @InjectMocks
    private FollowService followService;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private FollowConverter followConverter;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EntityManager entityManager;

    private Member mockFollower;
    private Member mockFollowing;
    private Follow mockFollow;
    private CustomUserDetails mockUserDetails;

    // 테스트용 ID 상수
    private static final Long FOLLOWER_ID = 1L;
    private static final Long FOLLOWING_ID = 2L;
    private static final Long NON_EXISTENT_USER_ID = 999L;
    private static final Integer DEFAULT_LIMIT = 10;
    private static final Long CURSOR = 5L;

    @BeforeEach
    void setUp() {
        // Mock Members 생성
        mockFollower = MemberFixture.createKbtMember();
        ReflectionTestUtils.setField(mockFollower, "id", FOLLOWER_ID);

        mockFollowing = MemberFixture.createNonKbtMember();
        ReflectionTestUtils.setField(mockFollowing, "id", FOLLOWING_ID);

        // Mock Follow 생성
        mockFollow = FollowFixture.createFollow(mockFollower, mockFollowing);

        // Mock CustomUserDetails 생성
        mockUserDetails = CustomUserDetailsFixture.createKbtCustomUserDetails();
    }

    // ========== addFollowing() 메서드 테스트 ==========

    @Test
    @DisplayName("팔로우 추가 성공 - 정상적으로 팔로우 관계가 생성되는지 확인")
    void addFollowing_Success() {
        // given
        given(memberRepository.findById(FOLLOWER_ID)).willReturn(Optional.of(mockFollower));
        given(memberRepository.findById(FOLLOWING_ID)).willReturn(Optional.of(mockFollowing));
        given(followRepository.existsByFollowerUserAndFollowingUser(mockFollower, mockFollowing)).willReturn(false);
        given(followConverter.toFollowEntity(mockFollower, mockFollowing)).willReturn(mockFollow);

        // when
        followService.addFollowing(FOLLOWING_ID, mockUserDetails);

        // then
        verify(memberRepository).findById(FOLLOWER_ID);
        verify(memberRepository).findById(FOLLOWING_ID);
        verify(followRepository).existsByFollowerUserAndFollowingUser(mockFollower, mockFollowing);
        verify(followConverter).toFollowEntity(mockFollower, mockFollowing);
        verify(followRepository).save(mockFollow);

        // 팔로우 카운트 증가 확인은 Member 객체의 상태 변화로 검증하기 어려우므로
        // 실제로는 메서드 호출 여부만 확인
    }

    @Test
    @DisplayName("자기 자신 팔로우 시도 - FollowException이 발생하는지 확인")
    void addFollowing_SelfFollow_ThrowsException() {
        // given
        Long selfId = FOLLOWER_ID;

        // when & then
        assertThatThrownBy(() ->
                followService.addFollowing(selfId, mockUserDetails))
                .isInstanceOf(FollowException.class)
                .satisfies(exception -> {
                    FollowException followException = (FollowException) exception;
                    assertThat(followException.getErrorCode()).isEqualTo(GeneralErrorCode.INVALID_FORMAT);
                    assertThat(followException.getMessage()).contains("입력 형식이 잘못되었습니다");
                });

        // Repository 호출이 없어야 함
        verify(memberRepository, never()).findById(any());
        verify(followRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 팔로워 - FollowException이 발생하는지 확인")
    void addFollowing_FollowerNotFound_ThrowsException() {
        // given
        given(memberRepository.findById(FOLLOWER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                followService.addFollowing(FOLLOWING_ID, mockUserDetails))
                .isInstanceOf(FollowException.class)
                .satisfies(exception -> {
                    FollowException followException = (FollowException) exception;
                    assertThat(followException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(followException.getEffectiveField()).isEqualTo("userId");
                });

        verify(memberRepository).findById(FOLLOWER_ID);
        verify(memberRepository, never()).findById(FOLLOWING_ID);
        verify(followRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 팔로잉 대상 - FollowException이 발생하는지 확인")
    void addFollowing_FollowingUserNotFound_ThrowsException() {
        // given
        given(memberRepository.findById(FOLLOWER_ID)).willReturn(Optional.of(mockFollower));
        given(memberRepository.findById(NON_EXISTENT_USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                followService.addFollowing(NON_EXISTENT_USER_ID, mockUserDetails))
                .isInstanceOf(FollowException.class)
                .satisfies(exception -> {
                    FollowException followException = (FollowException) exception;
                    assertThat(followException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(followException.getEffectiveField()).isEqualTo("targetUserId");
                });

        verify(memberRepository).findById(FOLLOWER_ID);
        verify(memberRepository).findById(NON_EXISTENT_USER_ID);
        verify(followRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 팔로우한 사용자 - FollowException이 발생하는지 확인")
    void addFollowing_AlreadyFollowing_ThrowsException() {
        // given
        given(memberRepository.findById(FOLLOWER_ID)).willReturn(Optional.of(mockFollower));
        given(memberRepository.findById(FOLLOWING_ID)).willReturn(Optional.of(mockFollowing));
        given(followRepository.existsByFollowerUserAndFollowingUser(mockFollower, mockFollowing)).willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                followService.addFollowing(FOLLOWING_ID, mockUserDetails))
                .isInstanceOf(FollowException.class)
                .satisfies(exception -> {
                    FollowException followException = (FollowException) exception;
                    assertThat(followException.getErrorCode()).isEqualTo(FollowErrorCode.ALREADY_FOLLOWING);
                });

        verify(memberRepository).findById(FOLLOWER_ID);
        verify(memberRepository).findById(FOLLOWING_ID);
        verify(followRepository).existsByFollowerUserAndFollowingUser(mockFollower, mockFollowing);
        verify(followRepository, never()).save(any());
    }

    // ========== removeFollowing() 메서드 테스트 ==========

    @Test
    @DisplayName("팔로우 제거 성공 - 정상적으로 팔로우 관계가 제거되는지 확인")
    void removeFollowing_Success() {
        // given
        given(memberRepository.findById(FOLLOWER_ID)).willReturn(Optional.of(mockFollower));
        given(memberRepository.findById(FOLLOWING_ID)).willReturn(Optional.of(mockFollowing));
        given(followRepository.findByFollowerUserAndFollowingUser(mockFollower, mockFollowing))
                .willReturn(Optional.of(mockFollow));

        // when
        followService.removeFollowing(FOLLOWING_ID, mockUserDetails);

        // then
        verify(memberRepository).findById(FOLLOWER_ID);
        verify(memberRepository).findById(FOLLOWING_ID);
        verify(followRepository).findByFollowerUserAndFollowingUser(mockFollower, mockFollowing);
        verify(followRepository).delete(mockFollow);
    }

    @Test
    @DisplayName("팔로우하지 않은 사용자 언팔로우 시도 - FollowException이 발생하는지 확인")
    void removeFollowing_NotFollowing_ThrowsException() {
        // given
        given(memberRepository.findById(FOLLOWER_ID)).willReturn(Optional.of(mockFollower));
        given(memberRepository.findById(FOLLOWING_ID)).willReturn(Optional.of(mockFollowing));
        given(followRepository.findByFollowerUserAndFollowingUser(mockFollower, mockFollowing))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                followService.removeFollowing(FOLLOWING_ID, mockUserDetails))
                .isInstanceOf(FollowException.class)
                .satisfies(exception -> {
                    FollowException followException = (FollowException) exception;
                    assertThat(followException.getErrorCode()).isEqualTo(FollowErrorCode.ALREADY_UNFOLLOWING);
                });

        verify(memberRepository).findById(FOLLOWER_ID);
        verify(memberRepository).findById(FOLLOWING_ID);
        verify(followRepository).findByFollowerUserAndFollowingUser(mockFollower, mockFollowing);
        verify(followRepository, never()).delete(any());
    }

    @Test
    @DisplayName("언팔로우 시 존재하지 않는 팔로워 - FollowException이 발생하는지 확인")
    void removeFollowing_FollowerNotFound_ThrowsException() {
        // given
        given(memberRepository.findById(FOLLOWER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                followService.removeFollowing(FOLLOWING_ID, mockUserDetails))
                .isInstanceOf(FollowException.class)
                .satisfies(exception -> {
                    FollowException followException = (FollowException) exception;
                    assertThat(followException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(followException.getEffectiveField()).isEqualTo("userId");
                });

        verify(memberRepository).findById(FOLLOWER_ID);
        verify(memberRepository, never()).findById(FOLLOWING_ID);
        verify(followRepository, never()).delete(any());
    }

    @Test
    @DisplayName("언팔로우 시 존재하지 않는 대상 사용자 - FollowException이 발생하는지 확인")
    void removeFollowing_TargetUserNotFound_ThrowsException() {
        // given
        given(memberRepository.findById(FOLLOWER_ID)).willReturn(Optional.of(mockFollower));
        given(memberRepository.findById(NON_EXISTENT_USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                followService.removeFollowing(NON_EXISTENT_USER_ID, mockUserDetails))
                .isInstanceOf(FollowException.class)
                .satisfies(exception -> {
                    FollowException followException = (FollowException) exception;
                    assertThat(followException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(followException.getEffectiveField()).isEqualTo("targetUserId");
                });

        verify(memberRepository).findById(FOLLOWER_ID);
        verify(memberRepository).findById(NON_EXISTENT_USER_ID);
        verify(followRepository, never()).delete(any());
    }
}