package com.kakaobase.snsapp.domain.members.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.members.dto.MemberRequestDto;
import com.kakaobase.snsapp.domain.members.exception.MemberErrorCode;
import com.kakaobase.snsapp.domain.members.exception.MemberException;
import com.kakaobase.snsapp.domain.members.service.EmailVerificationService;
import com.kakaobase.snsapp.domain.members.service.MemberService;
import com.kakaobase.snsapp.global.config.TestSecurityConfig;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import com.kakaobase.snsapp.global.security.jwt.JwtAuthenticationFilter;
import com.kakaobase.snsapp.global.security.jwt.JwtTokenProvider;
import com.kakaobase.snsapp.global.security.jwt.JwtTokenValidator;
import com.kakaobase.snsapp.global.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.kakaobase.snsapp.global.constants.MemberFixtureConstants.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)
@WebMvcTest(MemberController.class)
@Import(TestSecurityConfig.class)
@DisplayName("MemberController 단위 테스트")
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    // JWT 관련 의존성 Mock 처리
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtTokenValidator jwtTokenValidator;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private MemberRequestDto.SignUp validKbtSignUpRequest;
    private MemberRequestDto.SignUp validNonKbtSignUpRequest;
    private MemberRequestDto.EmailVerificationRequest validEmailVerificationRequest;
    private MemberRequestDto.EmailVerification validEmailVerificationCheck;

    @BeforeEach
    void setUp() {
        // 직접 DTO 생성 - Fixture 사용하지 않음
        validKbtSignUpRequest = new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );

        validNonKbtSignUpRequest = new MemberRequestDto.SignUp(
                NON_KBT_EMAIL,
                VALID_PASSWORD_ALT,
                NON_KBT_NAME,
                NON_KBT_NICKNAME,
                NON_KBT_MEMBER_CLASS_NAME.name(),
                NON_KBT_GITHUB_URL
        );

        validEmailVerificationRequest = new MemberRequestDto.EmailVerificationRequest(
                MEMBER_EMAIL,
                SIGN_UP_PURPOSE
        );

        validEmailVerificationCheck = new MemberRequestDto.EmailVerification(
                MEMBER_EMAIL,
                VALID_VERIFICATION_CODE
        );
    }

    // ========== 회원가입 API 테스트 ==========

    @Test
    @WithAnonymousUser
    @WithUserDetails
    @DisplayName("회원가입 성공 - KBT 회원가입 시 201 Created 응답")
    void signUp_KbtMember_Success() throws Exception {
        // given
        doNothing().when(memberService).signUp(any(MemberRequestDto.SignUp.class));

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validKbtSignUpRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.field").doesNotExist());

        verify(memberService).signUp(any(MemberRequestDto.SignUp.class));
    }


    @Test
    @DisplayName("회원가입 성공 - Non-KBT 회원가입 시 201 Created 응답")
    void signUp_NonKbtMember_Success() throws Exception {
        // given
        doNothing().when(memberService).signUp(any(MemberRequestDto.SignUp.class));

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validNonKbtSignUpRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.field").doesNotExist());

        verify(memberService).signUp(any(MemberRequestDto.SignUp.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 이메일로 404 Not Found 응답")
    void signUp_DuplicateEmail_ShouldReturn404() throws Exception {
        // given
        MemberException exception = new MemberException(GeneralErrorCode.RESOURCE_ALREADY_EXISTS, "email");
        doThrow(exception).when(memberService).signUp(any(MemberRequestDto.SignUp.class));

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validKbtSignUpRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("resource_alread_exists"))
                .andExpect(jsonPath("$.message").value("중복된 리소스 입니다."))
                .andExpect(jsonPath("$.field").value("email"));

        verify(memberService).signUp(any(MemberRequestDto.SignUp.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 인증 미완료로 401 Unauthorized 응답")
    void signUp_EmailNotVerified_ShouldReturn401() throws Exception {
        // given
        MemberException exception = new MemberException(MemberErrorCode.EMAIL_VERIFICATION_FAILED);
        doThrow(exception).when(memberService).signUp(any(MemberRequestDto.SignUp.class));

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validKbtSignUpRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("email_verification_failed"))
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되지 않았습니다."))
                .andExpect(jsonPath("$.field").value("email"));

        verify(memberService).signUp(any(MemberRequestDto.SignUp.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 유효하지 않은 이메일 형식으로 400 Bad Request 응답")
    void signUp_InvalidEmailFormat_ShouldReturn400() throws Exception {
        // given - 직접 생성
        MemberRequestDto.SignUp invalidEmailRequest = new MemberRequestDto.SignUp(
                INVALID_EMAIL_NO_AT,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_format"))
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.field").value("email"));

        verify(memberService, never()).signUp(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 빈 이메일로 400 Bad Request 응답")
    void signUp_BlankEmail_ShouldReturn400() throws Exception {
        // given - 직접 생성
        MemberRequestDto.SignUp blankEmailRequest = new MemberRequestDto.SignUp(
                BLANK_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankEmailRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_format"))
                .andExpect(jsonPath("$.message").value("이메일은 필수 입력값입니다."))
                .andExpect(jsonPath("$.field").value("email"));

        verify(memberService, never()).signUp(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 조건 미충족으로 400 Bad Request 응답")
    void signUp_InvalidPassword_ShouldReturn400() throws Exception {
        // given - 직접 생성
        MemberRequestDto.SignUp invalidPasswordRequest = new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                TOO_SHORT_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPasswordRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_format"))
                .andExpect(jsonPath("$.message").value("비밀번호는 8~20자리, 영문, 숫자, 특수문자를 포함해야 합니다"))
                .andExpect(jsonPath("$.field").value("password"));

        verify(memberService, never()).signUp(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 잘못된 JSON 형식으로 400 Bad Request 응답")
    void signUp_InvalidJsonFormat_ShouldReturn400() throws Exception {
        // given
        String invalidJson = "{ invalid json }";

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_format"));

        verify(memberService, never()).signUp(any());
    }

    // ========== 이메일 인증 코드 요청 API 테스트 ==========

    @Test
    @DisplayName("이메일 인증 코드 요청 성공 - 회원가입용")
    void requestEmailVerification_SignUp_Success() throws Exception {
        // given
        doNothing().when(emailVerificationService)
                .sendVerificationCode(eq(MEMBER_EMAIL), eq(SIGN_UP_PURPOSE), isNull());

        // when & then
        mockMvc.perform(post("/users/email/verification-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmailVerificationRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 이메일이 전송되었습니다."))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(emailVerificationService).sendVerificationCode(eq(MEMBER_EMAIL), eq(SIGN_UP_PURPOSE), isNull());
    }

    @Test
    @WithMockUser(username = "testuser@example.com", roles = "USER")
    @DisplayName("이메일 인증 코드 요청 성공 - 비밀번호 재설정용 (인증된 사용자)")
    void requestEmailVerification_PasswordReset_Success() throws Exception {
        // given
        MemberRequestDto.EmailVerificationRequest passwordResetRequest =
                new MemberRequestDto.EmailVerificationRequest(MEMBER_EMAIL, PASSWORD_RESET_PURPOSE);

        doNothing().when(emailVerificationService)
                .sendVerificationCode(eq(MEMBER_EMAIL), eq(PASSWORD_RESET_PURPOSE), any(CustomUserDetails.class));

        // when & then
        mockMvc.perform(post("/users/email/verification-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordResetRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 이메일이 전송되었습니다."))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(emailVerificationService).sendVerificationCode(eq(MEMBER_EMAIL), eq(PASSWORD_RESET_PURPOSE), any(CustomUserDetails.class));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("이메일 인증 코드 요청 성공 - 관리자 권한으로 비밀번호 재설정용")
    void requestEmailVerification_PasswordReset_AdminUser_Success() throws Exception {
        // given
        MemberRequestDto.EmailVerificationRequest adminPasswordResetRequest =
                new MemberRequestDto.EmailVerificationRequest(ADMIN_EMAIL, PASSWORD_RESET_PURPOSE);

        doNothing().when(emailVerificationService)
                .sendVerificationCode(eq(ADMIN_EMAIL), eq(PASSWORD_RESET_PURPOSE), any(CustomUserDetails.class));

        // when & then
        mockMvc.perform(post("/users/email/verification-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminPasswordResetRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 이메일이 전송되었습니다."))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(emailVerificationService).sendVerificationCode(eq(ADMIN_EMAIL), eq(PASSWORD_RESET_PURPOSE), any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("이메일 인증 코드 요청 실패 - 이미 존재하는 이메일 (회원가입용)")
    void requestEmailVerification_DuplicateEmail_ShouldReturn404() throws Exception {
        // given
        MemberException exception = new MemberException(GeneralErrorCode.RESOURCE_ALREADY_EXISTS, "email");
        doThrow(exception).when(emailVerificationService)
                .sendVerificationCode(anyString(), anyString(), any());

        // when & then
        mockMvc.perform(post("/users/email/verification-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmailVerificationRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("resource_already_exists"))
                .andExpect(jsonPath("$.field").value("email"));

        verify(emailVerificationService).sendVerificationCode(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("이메일 인증 코드 요청 실패 - 권한 없음 (비밀번호 재설정용)")
    void requestEmailVerification_Unauthorized_ShouldReturn403() throws Exception {
        // given
        MemberRequestDto.EmailVerificationRequest passwordResetRequest =
                new MemberRequestDto.EmailVerificationRequest(MEMBER_EMAIL, PASSWORD_RESET_PURPOSE);

        CustomException exception = new CustomException(MemberErrorCode.UNAUTHORIZED_ACCESS);
        doThrow(exception).when(emailVerificationService)
                .sendVerificationCode(anyString(), anyString(), isNull());

        // when & then - 인증되지 않은 사용자
        mockMvc.perform(post("/users/email/verification-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordResetRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));

        verify(emailVerificationService).sendVerificationCode(anyString(), anyString(), isNull());
    }

    @Test
    @DisplayName("이메일 인증 코드 요청 실패 - 유효하지 않은 이메일 형식")
    void requestEmailVerification_InvalidEmail_ShouldReturn400() throws Exception {
        // given - 직접 생성
        MemberRequestDto.EmailVerificationRequest invalidEmailRequest =
                new MemberRequestDto.EmailVerificationRequest(INVALID_EMAIL_NO_AT, SIGN_UP_PURPOSE);

        // when & then
        mockMvc.perform(post("/users/email/verification-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_format"))
                .andExpect(jsonPath("$.field").value("email"));

        verify(emailVerificationService, never()).sendVerificationCode(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("이메일 인증 코드 요청 실패 - 유효하지 않은 목적")
    void requestEmailVerification_InvalidPurpose_ShouldReturn400() throws Exception {
        // given - 직접 생성
        MemberRequestDto.EmailVerificationRequest invalidPurposeRequest =
                new MemberRequestDto.EmailVerificationRequest(MEMBER_EMAIL, INVALID_PURPOSE);

        // when & then
        mockMvc.perform(post("/users/email/verification-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPurposeRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_format"))
                .andExpect(jsonPath("$.field").value("purpose"));

        verify(emailVerificationService, never()).sendVerificationCode(anyString(), anyString(), any());
    }

    // ========== 이메일 인증 코드 확인 API 테스트 ==========

    @Test
    @DisplayName("이메일 인증 코드 확인 성공")
    void verifyEmail_Success() throws Exception {
        // given
        doNothing().when(emailVerificationService).verifyCode(MEMBER_EMAIL, VALID_VERIFICATION_CODE);

        // when & then
        mockMvc.perform(post("/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmailVerificationCheck)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증에 성공하였습니다."))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(emailVerificationService).verifyCode(MEMBER_EMAIL, VALID_VERIFICATION_CODE);
    }

    @Test
    @DisplayName("이메일 인증 코드 확인 실패 - 유효하지 않은 코드")
    void verifyEmail_InvalidCode_ShouldReturn400() throws Exception {
        // given
        MemberException exception = new MemberException(MemberErrorCode.EMAIL_CODE_INVALID);
        doThrow(exception).when(emailVerificationService).verifyCode(anyString(), anyString());

        // when & then
        mockMvc.perform(post("/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmailVerificationCheck)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("email_code_invalid"))
                .andExpect(jsonPath("$.message").value("인증 코드가 유효하지 않습니다."))
                .andExpect(jsonPath("$.field").value("code"));

        verify(emailVerificationService).verifyCode(anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증 코드 확인 실패 - 만료된 코드")
    void verifyEmail_ExpiredCode_ShouldReturn410() throws Exception {
        // given
        MemberException exception = new MemberException(MemberErrorCode.EMAIL_CODE_EXPIRED);
        doThrow(exception).when(emailVerificationService).verifyCode(anyString(), anyString());

        // when & then
        mockMvc.perform(post("/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmailVerificationCheck)))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("email_code_expired"))
                .andExpect(jsonPath("$.message").value("인증 코드가 만료되었습니다."))
                .andExpect(jsonPath("$.field").value("code"));

        verify(emailVerificationService).verifyCode(anyString(), anyString());
    }

    @Test
    @DisplayName("이메일 인증 코드 확인 실패 - 잘못된 코드 형식")
    void verifyEmail_InvalidCodeFormat_ShouldReturn400() throws Exception {
        // given - 직접 생성
        MemberRequestDto.EmailVerification invalidFormatRequest =
                new MemberRequestDto.EmailVerification(MEMBER_EMAIL, INVALID_CODE_FORMAT);

        // when & then
        mockMvc.perform(post("/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidFormatRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_format"))
                .andExpect(jsonPath("$.message").value("인증 코드는 6자리 숫자만 입력 가능합니다."))
                .andExpect(jsonPath("$.field").value("code"));

        verify(emailVerificationService, never()).verifyCode(anyString(), anyString());
    }


    @Test
    @DisplayName("여러 타입의 회원가입 요청이 올바르게 처리되는지 확인")
    void signUp_DifferentMemberTypes_AllProcessedCorrectly() throws Exception {
        // given
        doNothing().when(memberService).signUp(any(MemberRequestDto.SignUp.class));

        // when & then - KBT 회원
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validKbtSignUpRequest)))
                .andExpect(status().isCreated());

        // Non-KBT 회원
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validNonKbtSignUpRequest)))
                .andExpect(status().isCreated());

        verify(memberService, times(3)).signUp(any(MemberRequestDto.SignUp.class));
    }

    // ========== 추가 예외 케이스 테스트 ==========

    @Test
    @DisplayName("회원가입 실패 - null 이름으로 400 Bad Request 응답")
    void signUp_NullName_ShouldReturn400() throws Exception {
        // given - 직접 생성
        MemberRequestDto.SignUp nullNameRequest = new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                null,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullNameRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_format"))
                .andExpect(jsonPath("$.message").value("이름은 필수 입력값입니다."))
                .andExpect(jsonPath("$.field").value("name"));

        verify(memberService, never()).signUp(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 너무 긴 닉네임으로 400 Bad Request 응답")
    void signUp_TooLongNickname_ShouldReturn400() throws Exception {
        // given - 직접 생성
        MemberRequestDto.SignUp tooLongNicknameRequest = new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                TOO_LONG_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooLongNicknameRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_format"))
                .andExpect(jsonPath("$.message").value("닉네임은 2~20자까지 입력 가능합니다."))
                .andExpect(jsonPath("$.field").value("nickname"));

        verify(memberService, never()).signUp(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 잘못된 GitHub URL로 400 Bad Request 응답")
    void signUp_InvalidGithubUrl_ShouldReturn400() throws Exception {
        // given - 직접 생성
        MemberRequestDto.SignUp invalidGithubRequest = new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                MEMBER_NAME,
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                NON_GITHUB_URL
        );

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidGithubRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_format"))
                .andExpect(jsonPath("$.message").value("올바른 GitHub URL 형식이 아닙니다."))
                .andExpect(jsonPath("$.field").value("githubUrl"));

        verify(memberService, never()).signUp(any());
    }

    @Test
    @DisplayName("회원가입 성공 - GitHub URL 없이 회원가입")
    void signUp_WithoutGithubUrl_Success() throws Exception {
        // given
        MemberRequestDto.SignUp requestWithoutGithub = new MemberRequestDto.SignUp(
                NO_GITHUB_EMAIL,
                VALID_PASSWORD,
                NO_GITHUB_NAME,
                NO_GITHUB_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                null
        );
        doNothing().when(memberService).signUp(any(MemberRequestDto.SignUp.class));

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithoutGithub)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(memberService).signUp(any(MemberRequestDto.SignUp.class));
    }

    // ========== 이메일 인증 추가 테스트 ==========

    @Test
    @WithMockUser(username = "banned@example.com", roles = "USER")
    @DisplayName("이메일 인증 코드 요청 - 밴된 사용자도 비밀번호 재설정 가능")
    void requestEmailVerification_BannedUser_PasswordReset_Success() throws Exception {
        // given
        MemberRequestDto.EmailVerificationRequest bannedUserRequest =
                new MemberRequestDto.EmailVerificationRequest(BANNED_MEMBER_EMAIL, PASSWORD_RESET_PURPOSE);

        doNothing().when(emailVerificationService)
                .sendVerificationCode(eq(BANNED_MEMBER_EMAIL), eq(PASSWORD_RESET_PURPOSE), any(CustomUserDetails.class));

        // when & then
        mockMvc.perform(post("/users/email/verification-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bannedUserRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증 이메일이 전송되었습니다."))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(emailVerificationService).sendVerificationCode(eq(BANNED_MEMBER_EMAIL), eq(PASSWORD_RESET_PURPOSE), any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("이메일 인증 코드 요청 실패 - 존재하지 않는 회원 (비밀번호 재설정용)")
    @WithMockUser
    void requestEmailVerification_MemberNotFound_ShouldReturn404() throws Exception {
        // given
        String nonExistentEmail = "nonexistent@example.com";
        MemberRequestDto.EmailVerificationRequest notFoundRequest =
                new MemberRequestDto.EmailVerificationRequest(nonExistentEmail, PASSWORD_RESET_PURPOSE);

        MemberException exception = new MemberException(MemberErrorCode.MEMBER_NOT_FOUND);
        doThrow(exception).when(emailVerificationService)
                .sendVerificationCode(anyString(), anyString(), any(CustomUserDetails.class));

        // when & then
        mockMvc.perform(post("/users/email/verification-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notFoundRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("resource_not_found"))
                .andExpect(jsonPath("$.message").value("해당 회원을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.field").value("userId"));

        verify(emailVerificationService).sendVerificationCode(anyString(), anyString(), any(CustomUserDetails.class));
    }

    @Test
    @DisplayName("이메일 인증 코드 확인 - 여러 번 실패 후 성공")
    void verifyEmail_MultipleFailuresThenSuccess() throws Exception {
        // given
        MemberException invalidCodeException = new MemberException(MemberErrorCode.EMAIL_CODE_INVALID);

        // 첫 번째와 두 번째 호출에서는 실패, 세 번째에서는 성공
        doThrow(invalidCodeException)
                .doThrow(invalidCodeException)
                .doNothing()
                .when(emailVerificationService).verifyCode(anyString(), anyString());

        // when & then - 첫 번째 실패
        mockMvc.perform(post("/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmailVerificationCheck)))
                .andExpect(status().isBadRequest());

        // 두 번째 실패
        mockMvc.perform(post("/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmailVerificationCheck)))
                .andExpect(status().isBadRequest());

        // 세 번째 성공
        mockMvc.perform(post("/users/email/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmailVerificationCheck)))
                .andExpect(status().isOk());

        verify(emailVerificationService, times(3)).verifyCode(anyString(), anyString());
    }

    // ========== Content-Type 관련 테스트 ==========

    @Test
    @DisplayName("회원가입 실패 - Content-Type이 application/json이 아닌 경우")
    void signUp_WrongContentType_ShouldReturn415() throws Exception {
        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(validKbtSignUpRequest)))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

        verify(memberService, never()).signUp(any());
    }

    @Test
    @DisplayName("이메일 인증 코드 요청 실패 - Content-Type이 없는 경우")
    void requestEmailVerification_NoContentType_ShouldReturn400() throws Exception {
        // when & then
        mockMvc.perform(post("/users/email/verification-requests")
                        .content(objectMapper.writeValueAsString(validEmailVerificationRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(emailVerificationService, never()).sendVerificationCode(anyString(), anyString(), any());
    }


    // ========== 대용량 요청 테스트 ==========

    @Test
    @DisplayName("회원가입 실패 - 매우 긴 요청 데이터")
    void signUp_VeryLongRequestData_ShouldReturn400() throws Exception {
        // given - 매우 긴 문자열 생성
        String veryLongString = "a".repeat(10000);
        MemberRequestDto.SignUp veryLongRequest = new MemberRequestDto.SignUp(
                MEMBER_EMAIL,
                VALID_PASSWORD,
                veryLongString, // 매우 긴 이름
                MEMBER_NICKNAME,
                KBT_MEMBER_CLASS_NAME.name(),
                MEMBER_GITHUB_URL
        );

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(veryLongRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_format"));

        verify(memberService, never()).signUp(any());
    }
}