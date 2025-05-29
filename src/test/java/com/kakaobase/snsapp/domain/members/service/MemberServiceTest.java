package com.kakaobase.snsapp.domain.members.service;

import com.kakaobase.snsapp.domain.comments.dto.BotRecommentRequestDto;
import com.kakaobase.snsapp.domain.members.converter.MemberConverter;
import com.kakaobase.snsapp.domain.members.dto.MemberRequestDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.exception.MemberErrorCode;
import com.kakaobase.snsapp.domain.members.exception.MemberException;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.fixture.MemberFixture;
import com.kakaobase.snsapp.global.fixture.MemberRequestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberConverter memberConverter;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private Member mockMember;

    private MemberRequestDto.SignUp validKbtSignUpRequest;
    private MemberRequestDto.SignUp validNonKbtSignUpRequest;

    @BeforeEach
    void setUp() {
        validKbtSignUpRequest = MemberRequestFixture.createValidKbtSignUpRequest();
        validNonKbtSignUpRequest = MemberRequestFixture.createValidNonKbtSignUpRequest();
    }

    // ========== 회원가입 단위 테스트 ==========

    @Test
    @DisplayName("KBT 회원가입 성공 - 정상적인 요청시 Member 엔티티가 올바르게 생성되는지 확인")
    void signUp_KbtMember_Success() {
        // given
        Member kbtMember = MemberFixture.createKbtMember();

        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);
        given(emailVerificationService.isEmailVerified(MEMBER_EMAIL)).willReturn(true);
        given(memberConverter.toEntity(validKbtSignUpRequest)).willReturn(kbtMember);
        given(memberRepository.save(kbtMember)).willReturn(kbtMember);

        // when
        memberService.signUp(validKbtSignUpRequest);

        // then
        verify(memberRepository).existsByEmail(MEMBER_EMAIL);
        verify(emailVerificationService).isEmailVerified(MEMBER_EMAIL);
        verify(memberConverter).toEntity(validKbtSignUpRequest);
        verify(memberRepository).save(kbtMember);
    }

    @Test
    @DisplayName("Non-KBT 회원가입 성공 - className이 올바르게 설정되는지 확인")
    void signUp_NonKbtMember_Success() {
        // given
        Member nonKbtMember = MemberFixture.createNonKbtMember();

        given(memberRepository.existsByEmail(NON_KBT_EMAIL)).willReturn(false);
        given(emailVerificationService.isEmailVerified(NON_KBT_EMAIL)).willReturn(true);
        given(memberConverter.toEntity(validNonKbtSignUpRequest)).willReturn(nonKbtMember);
        given(memberRepository.save(nonKbtMember)).willReturn(nonKbtMember);

        // when
        memberService.signUp(validNonKbtSignUpRequest);

        // then
        verify(memberRepository).existsByEmail(NON_KBT_EMAIL);
        verify(emailVerificationService).isEmailVerified(NON_KBT_EMAIL);
        verify(memberConverter).toEntity(validNonKbtSignUpRequest);
        verify(memberRepository).save(nonKbtMember);
    }


    @Test
    @DisplayName("중복된 이메일로 회원가입 시 MemberException 발생")
    void signUp_DuplicateEmail_ShouldThrowException() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.signUp(validKbtSignUpRequest))
                .isInstanceOf(MemberException.class)
                .satisfies(exception -> {
                    MemberException memberException = (MemberException) exception;
                    assertThat(memberException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_ALREADY_EXISTS);
                    assertThat(memberException.getEffectiveField()).isEqualTo("email");
                });

        verify(memberRepository).existsByEmail(MEMBER_EMAIL);
        verify(emailVerificationService, never()).isEmailVerified(any());
        verify(memberConverter, never()).toEntity(any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("이메일 인증 미완료시 MemberException 발생")
    void signUp_EmailNotVerified_ShouldThrowException() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);
        given(emailVerificationService.isEmailVerified(MEMBER_EMAIL)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberService.signUp(validKbtSignUpRequest))
                .isInstanceOf(MemberException.class)
                .satisfies(exception -> {
                    MemberException memberException = (MemberException) exception;
                    assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.EMAIL_VERIFICATION_FAILED);
                    assertThat(memberException.getErrorCode().getError()).isEqualTo("email_verification_failed");
                    assertThat(memberException.getErrorCode().getMessage()).isEqualTo("이메일 인증이 완료되지 않았습니다.");
                    assertThat(memberException.getErrorCode().getField()).isEqualTo("email");
                });

        verify(memberRepository).existsByEmail(MEMBER_EMAIL);
        verify(emailVerificationService).isEmailVerified(MEMBER_EMAIL);
        verify(memberConverter, never()).toEntity(any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Repository save 메서드가 올바른 Member 객체와 함께 호출되는지 확인")
    void signUp_RepositorySave_CalledWithCorrectMember() {
        // given
        Member expectedMember = MemberFixture.createKbtMember();

        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);
        given(emailVerificationService.isEmailVerified(MEMBER_EMAIL)).willReturn(true);
        given(memberConverter.toEntity(validKbtSignUpRequest)).willReturn(expectedMember);
        given(memberRepository.save(expectedMember)).willReturn(expectedMember);

        // when
        memberService.signUp(validKbtSignUpRequest);

        // then
        verify(memberRepository).save(argThat(member ->
                member.equals(expectedMember)
        ));
    }

    // ========== 회원 정보 조회 단위 테스트 ==========

    @Test
    @DisplayName("회원 ID로 회원 정보 조회 성공")
    void getMemberInfo_Success() {
        // given
        Member member = MemberFixture.createMemberWithProfile();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        // when
        Map<String, String> result = memberService.getMemberInfo(MEMBER_ID);

        // then
        assertThat(result)
                .containsEntry("nickname", MEMBER_NICKNAME)
                .containsEntry("imageUrl", MEMBER_PROFILE_IMG_URL);

        verify(memberRepository).findById(MEMBER_ID);
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 조회시 MemberException 발생")
    void getMemberInfo_MemberNotFound_ShouldThrowException() {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMemberInfo(MEMBER_ID))
                .isInstanceOf(MemberException.class)
                .satisfies(exception -> {
                    MemberException memberException = (MemberException) exception;
                    assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
                    assertThat(memberException.getErrorCode().getError()).isEqualTo("resource_not_found");
                    assertThat(memberException.getErrorCode().getMessage()).isEqualTo("해당 회원을 찾을 수 없습니다.");
                    assertThat(memberException.getEffectiveField()).isEqualTo("memberId");
                });

        verify(memberRepository).findById(MEMBER_ID);
    }

    // ========== 여러 회원 정보 일괄 조회 단위 테스트 ==========


    @Test
    @DisplayName("빈 회원 ID 목록으로 조회시 빈 맵 반환")
    void getMemberInfoMapByIds_EmptyList_ReturnsEmptyMap() {
        // given
        List<Long> emptyIds = Collections.emptyList();
        given(memberRepository.findAllByIdIn(emptyIds)).willReturn(Collections.emptyList());

        // when
        Map<Long, Map<String, String>> result = memberService.getMemberInfoMapByIds(emptyIds);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(memberRepository).findAllByIdIn(emptyIds);
    }


    // ========== 회원 className 조회 단위 테스트 ==========

    @Test
    @DisplayName("회원 className 조회 성공")
    void getMemberClassName_Success() {
        // given
        Member member = MemberFixture.createKbtMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        // when
        String result = memberService.getMemberClassName(MEMBER_ID);

        // then
        assertThat(result).isEqualTo(KBT_MEMBER_CLASS_NAME.name());

        verify(memberRepository).findById(MEMBER_ID);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 className 조회시 MemberException 발생")
    void getMemberClassName_MemberNotFound_ShouldThrowException() {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMemberClassName(MEMBER_ID))
                .isInstanceOf(MemberException.class)
                .satisfies(exception -> {
                    MemberException memberException = (MemberException) exception;
                    assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
                });

        verify(memberRepository).findById(MEMBER_ID);
    }

    // ========== 회원 존재 여부 확인 단위 테스트 ==========

    @Test
    @DisplayName("회원 존재 여부 확인 - 존재하는 경우")
    void existsById_MemberExists_ReturnsTrue() {
        // given
        given(memberRepository.existsById(MEMBER_ID)).willReturn(true);

        // when
        boolean result = memberService.existsById(MEMBER_ID);

        // then
        assertThat(result).isTrue();

        verify(memberRepository).existsById(MEMBER_ID);
    }

    @Test
    @DisplayName("회원 존재 여부 확인 - 존재하지 않는 경우")
    void existsById_MemberNotExists_ReturnsFalse() {
        // given
        given(memberRepository.existsById(MEMBER_ID)).willReturn(false);

        // when
        boolean result = memberService.existsById(MEMBER_ID);

        // then
        assertThat(result).isFalse();

        verify(memberRepository).existsById(MEMBER_ID);
    }

    // ========== 봇용 회원 정보 조회 단위 테스트 ==========

    @Test
    @DisplayName("봇용 회원 정보 조회 성공")
    void getMemberBotInfo_Success() {
        // given
        Member member = MemberFixture.createKbtMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        // when
        BotRecommentRequestDto.UserInfo result = memberService.getMemberBotInfo(MEMBER_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.nickname()).isEqualTo(MEMBER_NICKNAME);
        assertThat(result.className()).isEqualTo(KBT_MEMBER_CLASS_NAME.name());

        verify(memberRepository).findById(MEMBER_ID);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 봇용 정보 조회시 MemberException 발생")
    void getMemberBotInfo_MemberNotFound_ShouldThrowException() {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMemberBotInfo(MEMBER_ID))
                .isInstanceOf(MemberException.class)
                .satisfies(exception -> {
                    MemberException memberException = (MemberException) exception;
                    assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
                    assertThat(memberException.getErrorCode().getError()).isEqualTo("resource_not_found");
                    assertThat(memberException.getErrorCode().getMessage()).isEqualTo("해당 회원을 찾을 수 없습니다.");
                    assertThat(memberException.getEffectiveField()).isEqualTo("memberId");
                });

        verify(memberRepository).findById(MEMBER_ID);
    }
}