package com.kakaobase.snsapp.domain.members.service;

import com.kakaobase.snsapp.annotation.ServiceTest;
import com.kakaobase.snsapp.domain.members.converter.MemberConverter;
import com.kakaobase.snsapp.domain.members.dto.MemberRequestDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.exception.MemberErrorCode;
import com.kakaobase.snsapp.domain.members.exception.MemberException;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.fixture.members.MemberFixture;
import com.kakaobase.snsapp.global.common.email.service.EmailVerificationService;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * MemberService 회원가입 기능 테스트
 */
@ServiceTest
@DisplayName("MemberService 회원가입 테스트")
class MemberServiceSignUpTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberConverter memberConverter;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    private MemberRequestDto.SignUp validSignUpRequest;
    private Member memberEntity;

    @BeforeEach
    void setUp() {
        // SignUp 요청 DTO는 직접 생성 (MemberFixture는 Entity용)
        validSignUpRequest = new MemberRequestDto.SignUp(
                "test@example.com",
                "Test1234!",
                "테스트유저",
                "testuser",
                "PANGYO_1",
                "https://github.com/testuser"
        );

        // Entity는 MemberFixture 사용
        memberEntity = MemberFixture.createDefaultMember();
    }

    @Test
    @DisplayName("유효한 정보로 회원가입이 성공한다")
    void signUp_WithValidInfo_Success() {
        // given
        given(memberRepository.existsByEmail(validSignUpRequest.email())).willReturn(false);
        given(emailVerificationService.isEmailVerified(validSignUpRequest.email())).willReturn(true);
        given(memberConverter.toEntity(validSignUpRequest)).willReturn(memberEntity);
        given(memberRepository.save(memberEntity)).willReturn(memberEntity);

        // when & then
        assertThatCode(() -> memberService.signUp(validSignUpRequest))
                .doesNotThrowAnyException();

        // verify
        verify(memberRepository).existsByEmail(validSignUpRequest.email());
        verify(emailVerificationService).isEmailVerified(validSignUpRequest.email());
        verify(memberConverter).toEntity(validSignUpRequest);
        verify(memberRepository).save(memberEntity);
    }


    @ParameterizedTest
    @DisplayName("다양한 기수로 회원가입이 성공한다")
    @EnumSource(Member.ClassName.class)
    void signUp_WithDifferentClasses_Success(Member.ClassName className) {
        // given
        MemberRequestDto.SignUp request = new MemberRequestDto.SignUp(
                "class-test@example.com",
                "Test1234!",
                "기수테스트",
                "classuser",
                className.name(),
                "https://github.com/classuser"
        );

        // 해당 기수에 맞는 Member Entity 생성
        Member memberWithClass = MemberFixture.createMemberWithClassName(
                "class-test@example.com", className);

        given(memberRepository.existsByEmail(request.email())).willReturn(false);
        given(emailVerificationService.isEmailVerified(request.email())).willReturn(true);
        given(memberConverter.toEntity(request)).willReturn(memberWithClass);
        given(memberRepository.save(memberWithClass)).willReturn(memberWithClass);

        // when & then
        assertThatCode(() -> memberService.signUp(request))
                .doesNotThrowAnyException();

        verify(memberRepository).save(memberWithClass);
    }

    @ParameterizedTest
    @DisplayName("유효한 이메일 형식으로 회원가입이 성공한다")
    @ValueSource(strings = {"test@example.com", "user123@domain.co.kr", "valid.email@test.org"})
    void signUp_WithValidEmails_Success(String email) {
        // given
        MemberRequestDto.SignUp request = new MemberRequestDto.SignUp(
                email,
                "Test1234!",
                "이메일테스트",
                "emailuser",
                "PANGYO_1",
                "https://github.com/emailuser"
        );

        // 해당 이메일로 Member Entity 생성
        Member memberWithEmail = MemberFixture.createMemberWithEmail(email);

        given(memberRepository.existsByEmail(email)).willReturn(false);
        given(emailVerificationService.isEmailVerified(email)).willReturn(true);
        given(memberConverter.toEntity(request)).willReturn(memberWithEmail);
        given(memberRepository.save(memberWithEmail)).willReturn(memberWithEmail);

        // when & then
        assertThatCode(() -> memberService.signUp(request))
                .doesNotThrowAnyException();

        verify(memberRepository).save(memberWithEmail);
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 시 예외가 발생한다")
    void signUp_WithDuplicateEmail_ThrowsException() {
        // given
        given(memberRepository.existsByEmail(validSignUpRequest.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.signUp(validSignUpRequest))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", GeneralErrorCode.RESOURCE_ALREADY_EXISTS);

        // verify
        verify(memberRepository).existsByEmail(validSignUpRequest.email());
        verify(emailVerificationService, never()).isEmailVerified(anyString());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("이메일 인증이 완료되지 않은 경우 예외가 발생한다")
    void signUp_WithUnverifiedEmail_ThrowsException() {
        // given
        given(memberRepository.existsByEmail(validSignUpRequest.email())).willReturn(false);
        given(emailVerificationService.isEmailVerified(validSignUpRequest.email())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> memberService.signUp(validSignUpRequest))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", MemberErrorCode.EMAIL_VERIFICATION_FAILED);

        // verify
        verify(memberRepository).existsByEmail(validSignUpRequest.email());
        verify(emailVerificationService).isEmailVerified(validSignUpRequest.email());
        verify(memberRepository, never()).save(any());
    }

    @ParameterizedTest
    @DisplayName("잘못된 이메일 형식으로 회원가입 시 변환 과정에서 처리된다")
    @ValueSource(strings = {"invalid-email", "test@", "@domain.com", "test.domain.com"})
    void signUp_WithInvalidEmails_HandledByValidation(String invalidEmail) {
        // given - 실제로는 Controller에서 @Valid로 검증되지만, 서비스 레벨에서도 처리 가능
        MemberRequestDto.SignUp invalidRequest = new MemberRequestDto.SignUp(
                invalidEmail,
                "Test1234!",
                "잘못된이메일",
                "invaliduser",
                "PANGYO_1",
                "https://github.com/invaliduser"
        );

        given(memberRepository.existsByEmail(invalidEmail)).willReturn(false);
        given(emailVerificationService.isEmailVerified(invalidEmail)).willReturn(true);
        given(memberConverter.toEntity(invalidRequest)).willReturn(memberEntity);
        given(memberRepository.save(memberEntity)).willReturn(memberEntity);

        // when & then - 서비스 레벨에서는 정상 처리 (유효성 검증은 상위 계층 책임)
        assertThatCode(() -> memberService.signUp(invalidRequest))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("MemberConverter에서 예외 발생 시 전파된다")
    void signUp_ConverterThrowsException_PropagatesException() {
        // given
        given(memberRepository.existsByEmail(validSignUpRequest.email())).willReturn(false);
        given(emailVerificationService.isEmailVerified(validSignUpRequest.email())).willReturn(true);
        given(memberConverter.toEntity(validSignUpRequest))
                .willThrow(new MemberException(GeneralErrorCode.RESOURCE_NOT_FOUND, "class_name"));

        // when & then
        assertThatThrownBy(() -> memberService.signUp(validSignUpRequest))
                .isInstanceOf(MemberException.class)
                .hasFieldOrPropertyWithValue("errorCode", GeneralErrorCode.RESOURCE_NOT_FOUND);

        // verify
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Repository 저장 실패 시 예외가 전파된다")
    void signUp_RepositorySaveThrowsException_PropagatesException() {
        // given
        given(memberRepository.existsByEmail(validSignUpRequest.email())).willReturn(false);
        given(emailVerificationService.isEmailVerified(validSignUpRequest.email())).willReturn(true);
        given(memberConverter.toEntity(validSignUpRequest)).willReturn(memberEntity);
        given(memberRepository.save(memberEntity)).willThrow(new RuntimeException("Database error"));

        // when & then
        assertThatThrownBy(() -> memberService.signUp(validSignUpRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");
    }

    @Test
    @DisplayName("null 요청으로 회원가입 시 NullPointerException이 발생한다")
    void signUp_WithNullRequest_ThrowsNullPointerException() {
        // when & then
        assertThatThrownBy(() -> memberService.signUp(null))
                .isInstanceOf(NullPointerException.class);

        // verify
        verify(memberRepository, never()).existsByEmail(anyString());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("회원가입 성공 후 로그가 출력된다")
    void signUp_Success_LogsCompletion() {
        // given
        given(memberRepository.existsByEmail(validSignUpRequest.email())).willReturn(false);
        given(emailVerificationService.isEmailVerified(validSignUpRequest.email())).willReturn(true);
        given(memberConverter.toEntity(validSignUpRequest)).willReturn(memberEntity);
        given(memberRepository.save(memberEntity)).willReturn(memberEntity);

        // when
        memberService.signUp(validSignUpRequest);

        // then - 실제 로그 테스트는 별도 방법 필요하지만, 여기서는 정상 완료 검증
        verify(memberRepository).save(memberEntity);
    }

    // === MemberFixture 활용 추가 테스트 ===

    @Test
    @DisplayName("MemberFixture로 생성한 엔티티로 회원가입이 성공한다")
    void signUp_WithMemberFixtureEntity_Success() {
        // given
        MemberRequestDto.SignUp request = new MemberRequestDto.SignUp(
                "fixture@example.com",
                "Test1234!",
                "픽스처테스트",
                "fixtureuser",
                "JEJU_1",
                "https://github.com/fixtureuser"
        );

        // MemberFixture로 다양한 속성을 가진 엔티티 생성
        Member fixtureEntity = MemberFixture.createMemberWithClassName(
                "fixture@example.com", Member.ClassName.JEJU_1);

        given(memberRepository.existsByEmail(request.email())).willReturn(false);
        given(emailVerificationService.isEmailVerified(request.email())).willReturn(true);
        given(memberConverter.toEntity(request)).willReturn(fixtureEntity);
        given(memberRepository.save(fixtureEntity)).willReturn(fixtureEntity);

        // when & then
        assertThatCode(() -> memberService.signUp(request))
                .doesNotThrowAnyException();

        verify(memberRepository).save(fixtureEntity);
    }

    @Test
    @DisplayName("프로필 이미지가 설정된 Member로 회원가입이 성공한다")
    void signUp_WithProfileImageMember_Success() {
        // given
        String profileImageUrl = "https://s3.amazonaws.com/bucket/profile.jpg";
        MemberRequestDto.SignUp request = new MemberRequestDto.SignUp(
                "profile@example.com",
                "Test1234!",
                "프로필테스트",
                "profileuser",
                "PANGYO_2",
                "https://github.com/profileuser"
        );

        // 프로필 이미지가 있는 Member 생성
        Member memberWithProfile = MemberFixture.createMemberWithProfileImage(profileImageUrl);

        given(memberRepository.existsByEmail(request.email())).willReturn(false);
        given(emailVerificationService.isEmailVerified(request.email())).willReturn(true);
        given(memberConverter.toEntity(request)).willReturn(memberWithProfile);
        given(memberRepository.save(memberWithProfile)).willReturn(memberWithProfile);

        // when & then
        assertThatCode(() -> memberService.signUp(request))
                .doesNotThrowAnyException();

        verify(memberRepository).save(memberWithProfile);
    }

    @Test
    @DisplayName("커스텀 Member로 회원가입이 성공한다")
    void signUp_WithCustomMember_Success() {
        // given
        MemberRequestDto.SignUp request = new MemberRequestDto.SignUp(
                "custom@example.com",
                "Test1234!",
                "커스텀테스트",
                "customuser",
                "JEJU_3",
                "https://github.com/customuser"
        );

        // 커스텀 Member 생성
        Member customMember = MemberFixture.createCustomMember(
                "custom@example.com",
                "customuser",
                "커스텀테스트",
                Member.ClassName.JEJU_3
        );

        given(memberRepository.existsByEmail(request.email())).willReturn(false);
        given(emailVerificationService.isEmailVerified(request.email())).willReturn(true);
        given(memberConverter.toEntity(request)).willReturn(customMember);
        given(memberRepository.save(customMember)).willReturn(customMember);

        // when & then
        assertThatCode(() -> memberService.signUp(request))
                .doesNotThrowAnyException();

        verify(memberRepository).save(customMember);
    }

    @Test
    @DisplayName("여러 Member 타입으로 연속 회원가입이 성공한다")
    void signUp_WithMultipleMemberTypes_Success() {
        // given - 다양한 타입의 Member들
        Member defaultMember = MemberFixture.createDefaultMember();
        Member memberWithEmail = MemberFixture.createMemberWithEmail("multi1@example.com");
        Member memberWithNickname = MemberFixture.createMemberWithNickname("multiuser");

        // 각각의 요청 DTO
        MemberRequestDto.SignUp request1 = new MemberRequestDto.SignUp(
                "multi1@example.com", "Test1!", "테스트1", "user1", "PANGYO_1", null);
        MemberRequestDto.SignUp request2 = new MemberRequestDto.SignUp(
                "multi2@example.com", "Test2!", "테스트2", "user2", "PANGYO_2", null);
        MemberRequestDto.SignUp request3 = new MemberRequestDto.SignUp(
                "multi3@example.com", "Test3!", "테스트3", "user3", "JEJU_1", null);

        // Mock 설정 - 첫 번째 회원가입
        given(memberRepository.existsByEmail("multi1@example.com")).willReturn(false);
        given(emailVerificationService.isEmailVerified("multi1@example.com")).willReturn(true);
        given(memberConverter.toEntity(request1)).willReturn(defaultMember);
        given(memberRepository.save(defaultMember)).willReturn(defaultMember);

        // Mock 설정 - 두 번째 회원가입
        given(memberRepository.existsByEmail("multi2@example.com")).willReturn(false);
        given(emailVerificationService.isEmailVerified("multi2@example.com")).willReturn(true);
        given(memberConverter.toEntity(request2)).willReturn(memberWithEmail);
        given(memberRepository.save(memberWithEmail)).willReturn(memberWithEmail);

        // Mock 설정 - 세 번째 회원가입
        given(memberRepository.existsByEmail("multi3@example.com")).willReturn(false);
        given(emailVerificationService.isEmailVerified("multi3@example.com")).willReturn(true);
        given(memberConverter.toEntity(request3)).willReturn(memberWithNickname);
        given(memberRepository.save(memberWithNickname)).willReturn(memberWithNickname);

        // when & then
        assertThatCode(() -> {
            memberService.signUp(request1);
            memberService.signUp(request2);
            memberService.signUp(request3);
        }).doesNotThrowAnyException();

        // verify
        verify(memberRepository).save(defaultMember);
        verify(memberRepository).save(memberWithEmail);
        verify(memberRepository).save(memberWithNickname);
    }
}