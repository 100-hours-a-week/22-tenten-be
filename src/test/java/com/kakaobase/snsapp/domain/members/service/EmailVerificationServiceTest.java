package com.kakaobase.snsapp.domain.members.service;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.members.exception.MemberErrorCode;
import com.kakaobase.snsapp.domain.members.exception.MemberException;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.common.email.EmailSender;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import com.kakaobase.snsapp.global.fixture.CustomUserDetailsFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService 단위 테스트")
class EmailVerificationServiceTest {

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Mock
    private EmailSender emailSender;

    @Mock
    private MemberRepository memberRepository;

    private CustomUserDetails mockUserDetails;
    private Map<String, Object> verificationStore;

    @BeforeEach
    void setUp() {
        mockUserDetails = CustomUserDetailsFixture.createKbtCustomUserDetails();

        // ReflectionTestUtils를 사용하여 private 필드에 접근
        verificationStore = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(emailVerificationService, "verificationStore", verificationStore);
    }

    // ========== 이메일 인증 코드 전송 단위 테스트 ==========

    @Test
    @DisplayName("회원가입용 이메일 인증 코드 전송 성공")
    void sendVerificationCode_SignUp_Success() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // when
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);

        // then
        verify(memberRepository).existsByEmail(MEMBER_EMAIL);
        verify(emailSender).sendVerificationEmail(eq(MEMBER_EMAIL), anyString());

        // verificationStore에 데이터가 저장되었는지 확인
        assertThat(verificationStore).containsKey(MEMBER_EMAIL);
    }

    @Test
    @DisplayName("비밀번호 재설정용 이메일 인증 코드 전송 성공")
    void sendVerificationCode_PasswordReset_Success() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(true);

        // when
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, PASSWORD_RESET_PURPOSE, mockUserDetails);

        // then
        verify(memberRepository).existsByEmail(MEMBER_EMAIL);
        verify(emailSender).sendVerificationEmail(eq(MEMBER_EMAIL), anyString());

        // verificationStore에 데이터가 저장되었는지 확인
        assertThat(verificationStore).containsKey(MEMBER_EMAIL);
    }

    @Test
    @DisplayName("회원가입용 이메일 전송 - 이미 존재하는 이메일로 요청시 MemberException 발생")
    void sendVerificationCode_SignUp_DuplicateEmail_ShouldThrowException() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null))
                .isInstanceOf(MemberException.class)
                .satisfies(exception -> {
                    MemberException memberException = (MemberException) exception;
                    assertThat(memberException.getErrorCode()).isEqualTo(GeneralErrorCode.RESOURCE_ALREADY_EXISTS);
                    assertThat(memberException.getEffectiveField()).isEqualTo("email");
                });

        verify(memberRepository).existsByEmail(MEMBER_EMAIL);
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정용 이메일 전송 - UserDetails가 null일 때 CustomException 발생")
    void sendVerificationCode_PasswordReset_NullUserDetails_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(MEMBER_EMAIL, PASSWORD_RESET_PURPOSE, null))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode()).isEqualTo(MemberErrorCode.UNAUTHORIZED_ACCESS);
                });

        verify(memberRepository, never()).existsByEmail(anyString());
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정용 이메일 전송 - 존재하지 않는 이메일로 요청시 MemberException 발생")
    void sendVerificationCode_PasswordReset_EmailNotFound_ShouldThrowException() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(MEMBER_EMAIL, PASSWORD_RESET_PURPOSE, mockUserDetails))
                .isInstanceOf(MemberException.class)
                .satisfies(exception -> {
                    MemberException memberException = (MemberException) exception;
                    assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
                });

        verify(memberRepository).existsByEmail(MEMBER_EMAIL);
        verify(emailSender, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("인증 코드 생성 및 저장 확인 - 6자리 숫자 코드가 생성되는지 확인")
    void sendVerificationCode_CodeGeneration_SixDigitNumber() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // when
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);

        // then
        verify(emailSender).sendVerificationEmail(eq(MEMBER_EMAIL), argThat(code ->
                code.matches("\\d{6}")  // 6자리 숫자인지 확인
        ));
    }

    // ========== 이메일 인증 코드 검증 단위 테스트 ==========

    @Test
    @DisplayName("이메일 인증 코드 검증 성공")
    void verifyCode_ValidCode_Success() {
        // given
        String testCode = VALID_VERIFICATION_CODE;
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // 먼저 인증 코드를 전송하여 verificationStore에 데이터 저장
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);

        // 실제 생성된 코드를 가져와서 테스트 (ReflectionTestUtils 사용)
        Object verificationData = verificationStore.get(MEMBER_EMAIL);
        String actualCode = (String) ReflectionTestUtils.getField(verificationData, "code");

        // when
        emailVerificationService.verifyCode(MEMBER_EMAIL, actualCode);

        // then
        boolean isVerified = emailVerificationService.isEmailVerified(MEMBER_EMAIL);
        assertThat(isVerified).isTrue();
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 실패 - 잘못된 코드")
    void verifyCode_InvalidCode_ShouldThrowException() {
        // given
        String wrongCode = "999999";
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // 먼저 인증 코드를 전송하여 verificationStore에 데이터 저장
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);

        // when & then
        assertThatThrownBy(() -> emailVerificationService.verifyCode(MEMBER_EMAIL, wrongCode))
                .isInstanceOf(MemberException.class)
                .satisfies(exception -> {
                    MemberException memberException = (MemberException) exception;
                    assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.EMAIL_CODE_INVALID);
                    assertThat(memberException.getErrorCode().getError()).isEqualTo("email_code_invalid");
                    assertThat(memberException.getErrorCode().getMessage()).isEqualTo("인증 코드가 유효하지 않습니다.");
                    assertThat(memberException.getErrorCode().getField()).isEqualTo("code");
                });

        // 인증이 실패했으므로 isEmailVerified는 false여야 함
        boolean isVerified = emailVerificationService.isEmailVerified(MEMBER_EMAIL);
        assertThat(isVerified).isFalse();
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 실패 - 인증 정보가 없는 경우")
    void verifyCode_NoVerificationData_ShouldThrowException() {
        // given
        String testCode = VALID_VERIFICATION_CODE;

        // when & then
        assertThatThrownBy(() -> emailVerificationService.verifyCode(MEMBER_EMAIL, testCode))
                .isInstanceOf(MemberException.class)
                .satisfies(exception -> {
                    MemberException memberException = (MemberException) exception;
                    assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.EMAIL_CODE_EXPIRED);
                    assertThat(memberException.getErrorCode().getError()).isEqualTo("email_code_expired");
                    assertThat(memberException.getErrorCode().getMessage()).isEqualTo("인증 코드가 만료되었습니다.");
                    assertThat(memberException.getErrorCode().getField()).isEqualTo("code");
                });
    }

    @Test
    @DisplayName("이메일 인증 코드 검증 실패 - 만료된 코드")
    void verifyCode_ExpiredCode_ShouldThrowException() throws Exception {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // 먼저 인증 코드를 전송
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);

        // verificationData를 가져와서 만료 시간을 과거로 설정
        Object verificationData = verificationStore.get(MEMBER_EMAIL);
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(11); // 10분 + 1분 전
        ReflectionTestUtils.setField(verificationData, "expirationTime", pastTime);

        String actualCode = (String) ReflectionTestUtils.getField(verificationData, "code");

        // when & then
        assertThatThrownBy(() -> emailVerificationService.verifyCode(MEMBER_EMAIL, actualCode))
                .isInstanceOf(MemberException.class)
                .satisfies(exception -> {
                    MemberException memberException = (MemberException) exception;
                    assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.EMAIL_CODE_EXPIRED);
                });

        // 만료된 코드로 인해 verificationStore에서 제거되었는지 확인
        assertThat(verificationStore).doesNotContainKey(MEMBER_EMAIL);
    }

    @Test
    @DisplayName("잘못된 코드 입력시 시도 횟수 증가 확인")
    void verifyCode_WrongCode_AttemptsIncremented() {
        // given
        String wrongCode = "999999";
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // 먼저 인증 코드를 전송
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);

        Object verificationData = verificationStore.get(MEMBER_EMAIL);
        int initialAttempts = (int) ReflectionTestUtils.getField(verificationData, "attempts");

        // when
        try {
            emailVerificationService.verifyCode(MEMBER_EMAIL, wrongCode);
        } catch (MemberException e) {
            // 예외는 예상된 것이므로 무시
        }

        // then
        int afterAttempts = (int) ReflectionTestUtils.getField(verificationData, "attempts");
        assertThat(afterAttempts).isEqualTo(initialAttempts + 1);
    }

    // ========== 이메일 인증 여부 확인 단위 테스트 ==========

    @Test
    @DisplayName("이메일 인증 여부 확인 - 인증 완료된 경우")
    void isEmailVerified_VerifiedEmail_ReturnsTrue() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // 인증 코드 전송 및 검증 완료
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);
        Object verificationData = verificationStore.get(MEMBER_EMAIL);
        String actualCode = (String) ReflectionTestUtils.getField(verificationData, "code");
        emailVerificationService.verifyCode(MEMBER_EMAIL, actualCode);

        // when
        boolean result = emailVerificationService.isEmailVerified(MEMBER_EMAIL);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("이메일 인증 여부 확인 - 인증되지 않은 경우")
    void isEmailVerified_NotVerifiedEmail_ReturnsFalse() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // 인증 코드만 전송하고 검증하지 않음
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);

        // when
        boolean result = emailVerificationService.isEmailVerified(MEMBER_EMAIL);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("이메일 인증 여부 확인 - 인증 정보가 없는 경우")
    void isEmailVerified_NoVerificationData_ReturnsFalse() {
        // when
        boolean result = emailVerificationService.isEmailVerified(MEMBER_EMAIL);

        // then
        assertThat(result).isFalse();
    }

    // ========== MemberFixtureConstants 상수값 활용 테스트 ==========

    @Test
    @DisplayName("MemberFixtureConstants의 이메일 인증 관련 상수값들이 올바르게 사용되는지 확인")
    void emailVerification_WithMemberFixtureConstants_Success() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // when - 회원가입용 인증 코드 전송
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);

        // then - MemberFixtureConstants 값들이 올바르게 사용되었는지 확인
        verify(memberRepository).existsByEmail(MEMBER_EMAIL);
        verify(emailSender).sendVerificationEmail(eq(MEMBER_EMAIL), anyString());

        // verificationStore에 올바른 이메일로 저장되었는지 확인
        assertThat(verificationStore).containsKey(MEMBER_EMAIL);
    }


    // ========== 에지 케이스 테스트 ==========

    @Test
    @DisplayName("동일한 이메일로 여러 번 인증 코드 요청시 기존 데이터 덮어쓰기")
    void sendVerificationCode_MultipleRequests_OverwritesPreviousData() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // when - 첫 번째 요청
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);
        Object firstData = verificationStore.get(MEMBER_EMAIL);
        String firstCode = (String) ReflectionTestUtils.getField(firstData, "code");

        // 두 번째 요청
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);
        Object secondData = verificationStore.get(MEMBER_EMAIL);
        String secondCode = (String) ReflectionTestUtils.getField(secondData, "code");

        // then
        // 새로운 코드가 생성되어야 함 (매우 높은 확률로 다름)
        assertThat(firstCode).isNotEqualTo(secondCode);

        // verificationStore에는 여전히 하나의 항목만 있어야 함
        assertThat(verificationStore).hasSize(1);
        assertThat(verificationStore).containsKey(MEMBER_EMAIL);

        // 두 번 호출되었는지 확인
        verify(emailSender, times(2)).sendVerificationEmail(eq(MEMBER_EMAIL), anyString());
    }

    @Test
    @DisplayName("VerificationData 객체의 초기 상태 확인")
    void sendVerificationCode_VerificationDataInitialState() {
        // given
        given(memberRepository.existsByEmail(MEMBER_EMAIL)).willReturn(false);

        // when
        emailVerificationService.sendVerificationCode(MEMBER_EMAIL, SIGN_UP_PURPOSE, null);

        // then
        Object verificationData = verificationStore.get(MEMBER_EMAIL);

        // 초기 상태 확인
        assertThat(ReflectionTestUtils.getField(verificationData, "attempts")).isEqualTo(0);
        assertThat(ReflectionTestUtils.getField(verificationData, "verified")).isEqualTo(false);
        assertThat(ReflectionTestUtils.getField(verificationData, "code")).isNotNull();
        assertThat(ReflectionTestUtils.getField(verificationData, "expirationTime")).isNotNull();

        // 만료 시간이 현재 시간 + 10분 정도인지 확인 (오차 1분 허용)
        LocalDateTime expirationTime = (LocalDateTime) ReflectionTestUtils.getField(verificationData, "expirationTime");
        LocalDateTime expectedTime = LocalDateTime.now().plusMinutes(10);
        assertThat(expirationTime).isBetween(expectedTime.minusMinutes(1), expectedTime.plusMinutes(1));
    }
}