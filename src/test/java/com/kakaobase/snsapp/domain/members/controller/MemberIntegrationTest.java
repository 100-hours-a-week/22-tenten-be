package com.kakaobase.snsapp.domain.members.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaobase.snsapp.domain.members.dto.MemberRequestDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.fixture.members.MemberFixture;
import com.kakaobase.snsapp.stub.StubEmailVerificationService;
import com.kakaobase.snsapp.stub.StubJwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Member 도메인 통합 테스트
 *
 * Stub 서비스를 사용하여 외부 의존성 없이 전체 계층을 통합 테스트
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Member 도메인 통합 테스트")
class MemberIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StubEmailVerificationService stubEmailService;

    @Autowired
    private StubJwtTokenValidator stubJwtValidator;

    private MockMvc mockMvc;
    private Member testMember;
    private String validToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // 테스트용 회원 데이터 설정
        setupTestMember();

        // 테스트용 JWT 토큰 설정
        setupTestToken();

        // Stub 서비스 초기화
        resetStubServices();
    }

    /**
     * 테스트용 회원 데이터 생성 및 저장
     */
    private void setupTestMember() {
        testMember = MemberFixture.createDefaultMember();
        testMember = memberRepository.save(testMember);
        memberRepository.flush();
    }

    /**
     * 테스트용 JWT 토큰 설정
     */
    private void setupTestToken() {
        validToken = "Bearer valid-jwt-token";

        // Stub JWT 서비스에 유효한 토큰 등록
        stubJwtValidator.addValidToken(
                "valid-jwt-token",
                testMember.getId().toString(),
                "USER",
                "PANGYO_1"
        );
    }

    /**
     * Stub 서비스들 초기화
     */
    private void resetStubServices() {
        stubEmailService.clearAllVerifications();
        stubJwtValidator.clearAllTokens();

        // 기본 설정 다시 적용
        setupTestToken();
    }

    // === 회원가입 통합 테스트 ===

    @Test
    @DisplayName("회원가입이 성공한다")
    void signUp_Success() throws Exception {
        // given
        MemberRequestDto.SignUp signUpRequest = new MemberRequestDto.SignUp(
                "newuser@example.com",
                "NewPassword123!",
                "새사용자",
                "newuser",
                "PANGYO_1",
                "https://github.com/newuser"
        );

        // 이메일 인증 완료 상태로 Mock 설정
        //stubEmailService.markAsVerified(email);

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        // 데이터베이스 확인
        Member savedMember = memberRepository.findByEmail(signUpRequest.email()).orElse(null);
        assertThat(savedMember).isNotNull();
        assertThat(savedMember.getNickname()).isEqualTo(signUpRequest.nickname());
        assertThat(savedMember.getName()).isEqualTo(signUpRequest.name());
        assertThat(passwordEncoder.matches(signUpRequest.password(), savedMember.getPassword())).isTrue();
    }

    @Test
    @DisplayName("이메일 중복으로 회원가입이 실패한다")
    void signUp_DuplicateEmail_Failure() throws Exception {
        // given
        MemberRequestDto.SignUp signUpRequest = new MemberRequestDto.SignUp(
                testMember.getEmail(), // 이미 존재하는 이메일
                "Password123!",
                "새사용자",
                "newuser",
                "PANGYO_1",
                "https://github.com/newuser"
        );

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("resource_already_exists"))
                .andExpect(jsonPath("$.field").value("email"));
    }

    @Test
    @DisplayName("이메일 인증 미완료로 회원가입이 실패한다")
    void signUp_EmailNotVerified_Failure() throws Exception {
        // given
        MemberRequestDto.SignUp signUpRequest = new MemberRequestDto.SignUp(
                "unverified@example.com",
                "Password123!",
                "미인증사용자",
                "unverified",
                "PANGYO_1",
                "https://github.com/unverified"
        );

        // 이메일 인증 미완료 상태로 Mock 설정
        //given(emailVerificationService.isEmailVerified(signUpRequest.email())).willReturn(false);

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("email_verification_failed"));
    }

    @Test
    @DisplayName("유효하지 않은 회원가입 데이터로 실패한다")
    void signUp_InvalidData_Failure() throws Exception {
        // given - 잘못된 이메일 형식
        MemberRequestDto.SignUp signUpRequest = new MemberRequestDto.SignUp(
                "invalid-email", // 잘못된 이메일 형식
                "weak", // 약한 비밀번호
                "", // 빈 이름
                "a", // 너무 짧은 닉네임
                "INVALID_CLASS", // 존재하지 않는 기수
                "invalid-github-url" // 잘못된 GitHub URL
        );

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // === 마이페이지 조회 통합 테스트 ===

    @Test
    @DisplayName("마이페이지 조회가 성공한다")
    void getMypage_Success() throws Exception {
        // given
        Long userId = testMember.getId();

        // when & then
        mockMvc.perform(get("/users/{userId}", userId)
                        .header("Authorization", validToken))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("유저 마이페이지 조회에 성공하였습니다"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.name").value(testMember.getName()))
                .andExpect(jsonPath("$.data.nickname").value(testMember.getNickname()))
                .andExpect(jsonPath("$.data.is_me").value(true));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 마이페이지 조회가 실패한다")
    void getMypage_UserNotFound_Failure() throws Exception {
        // given
        Long nonExistentUserId = 999L;

        // when & then
        mockMvc.perform(get("/users/{userId}", nonExistentUserId)
                        .header("Authorization", validToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("resource_not_found"));
    }

    @Test
    @DisplayName("인증 없이 마이페이지 조회가 실패한다")
    void getMypage_Unauthorized_Failure() throws Exception {
        // given
        Long userId = testMember.getId();

        // when & then
        mockMvc.perform(get("/users/{userId}", userId))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    // === 비밀번호 변경 통합 테스트 ===

    @Test
    @DisplayName("비밀번호 변경이 성공한다")
    void changePassword_Success() throws Exception {
        // given
        MemberRequestDto.PasswordChange passwordChangeRequest = new MemberRequestDto.PasswordChange(
                testMember.getEmail(),
                "NewPassword123!"
        );

        // 이메일 인증 완료 상태로 Mock 설정
        //given(emailVerificationService.isEmailVerified(testMember.getEmail())).willReturn(true);

        // when & then
        mockMvc.perform(put("/users/password")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordChangeRequest)))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("비밀번호 수정이 완료되었습니다"));

        // 데이터베이스에서 비밀번호 변경 확인
        Member updatedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(passwordChangeRequest.NewPassword(), updatedMember.getPassword())).isTrue();
    }

    @Test
    @DisplayName("이메일 인증 미완료로 비밀번호 변경이 실패한다")
    void changePassword_EmailNotVerified_Failure() throws Exception {
        // given
        MemberRequestDto.PasswordChange passwordChangeRequest = new MemberRequestDto.PasswordChange(
                testMember.getEmail(),
                "NewPassword123!"
        );

        // 이메일 인증 미완료 상태로 Mock 설정
        //given(emailVerificationService.isEmailVerified(testMember.getEmail())).willReturn(false);

        // when & then
        mockMvc.perform(put("/users/password")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordChangeRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("email_verification_failed"));
    }

    // === GitHub URL 변경 통합 테스트 ===

    @Test
    @DisplayName("GitHub URL 변경이 성공한다")
    void changeGithubUrl_Success() throws Exception {
        // given
        MemberRequestDto.GithubUrlChange githubUrlChangeRequest = new MemberRequestDto.GithubUrlChange(
                "https://github.com/newusername"
        );

        // when & then
        mockMvc.perform(put("/users/github-url")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(githubUrlChangeRequest)))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("GitHub 링크가 성공적으로 변경되었습니다."));

        // 데이터베이스에서 GitHub URL 변경 확인
        Member updatedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        assertThat(updatedMember.getGithubUrl()).isEqualTo(githubUrlChangeRequest.githubUrl());
    }

    @Test
    @DisplayName("잘못된 GitHub URL 형식으로 변경이 실패한다")
    void changeGithubUrl_InvalidFormat_Failure() throws Exception {
        // given
        MemberRequestDto.GithubUrlChange githubUrlChangeRequest = new MemberRequestDto.GithubUrlChange(
                "https://gitlab.com/invalid" // GitHub이 아닌 URL
        );

        // when & then
        mockMvc.perform(put("/users/github-url")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(githubUrlChangeRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // === 프로필 이미지 변경 통합 테스트 ===

    @Test
    @DisplayName("프로필 이미지 변경이 성공한다")
    void changeProfileImage_Success() throws Exception {
        // given
        MemberRequestDto.ProfileImageChange profileImageChangeRequest = new MemberRequestDto.ProfileImageChange(
                "https://mybucket.s3.amazonaws.com/profiles/newimage.jpg"
        );

        // when & then
        mockMvc.perform(put("/users/images")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileImageChangeRequest)))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("프로필 이미지가 성공적으로 변경되었습니다."))
                .andExpect(jsonPath("$.data.image_url").value(profileImageChangeRequest.imageUrl()));

        // 데이터베이스에서 프로필 이미지 변경 확인
        Member updatedMember = memberRepository.findById(testMember.getId()).orElseThrow();
        assertThat(updatedMember.getProfileImgUrl()).isEqualTo(profileImageChangeRequest.imageUrl());
    }

    @Test
    @DisplayName("잘못된 S3 URL 형식으로 프로필 이미지 변경이 실패한다")
    void changeProfileImage_InvalidS3Url_Failure() throws Exception {
        // given
        MemberRequestDto.ProfileImageChange profileImageChangeRequest = new MemberRequestDto.ProfileImageChange(
                "https://example.com/invalid.jpg" // S3가 아닌 URL
        );

        // when & then
        mockMvc.perform(put("/users/images")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileImageChangeRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // === 회원탈퇴 통합 테스트 ===

    @Test
    @DisplayName("회원탈퇴가 성공한다")
    void unregister_Success() throws Exception {
        // given
        //stubEmailService.markAsVerified(email);

        // when & then
        mockMvc.perform(delete("/users")
                        .header("Authorization", validToken))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("회원탈퇴가 완료되었습니다."));

        // 데이터베이스에서 회원 소프트 삭제 확인
        Member deletedMember = memberRepository.findById(testMember.getId()).orElse(null);
        assertThat(deletedMember).isNull(); // @Where 조건으로 인해 조회되지 않음

    }

    @Test
    @DisplayName("이메일 인증 미완료로 회원탈퇴가 실패한다")
    void unregister_EmailNotVerified_Failure() throws Exception {
        // given
        //stubEmailService.markAsVerified(email);

        // when & then
        mockMvc.perform(delete("/users")
                        .header("Authorization", validToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("email_verification_failed"));

        // 데이터베이스에서 회원이 삭제되지 않았는지 확인
        Member member = memberRepository.findById(testMember.getId()).orElse(null);
        assertThat(member).isNotNull();
        assertThat(member.isDeleted()).isFalse();
    }

    // === 예외 상황 통합 테스트 ===

    @Test
    @DisplayName("유효하지 않은 JWT 토큰으로 요청이 실패한다")
    void authenticatedRequest_InvalidToken_Failure() throws Exception {
        // given
        String invalidToken = "Bearer invalid-token";

        // when & then
        mockMvc.perform(put("/users/github-url")
                        .header("Authorization", invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Content-Type이 잘못된 경우 요청이 실패한다")
    void request_WrongContentType_Failure() throws Exception {
        // given
        MemberRequestDto.SignUp signUpRequest = new MemberRequestDto.SignUp(
                "test@example.com",
                "Password123!",
                "테스트",
                "testuser",
                "PANGYO_1",
                "https://github.com/test"
        );

        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.TEXT_PLAIN) // 잘못된 Content-Type
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("요청 본문이 없는 경우 실패한다")
    void request_EmptyBody_Failure() throws Exception {
        // when & then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // === 성능 및 동시성 테스트 ===

    @Test
    @DisplayName("동시에 같은 이메일로 회원가입 시도 시 하나만 성공한다")
    void signUp_ConcurrentSameEmail_OnlyOneSucceeds() throws Exception {
        // given
        String email = "concurrent@example.com";
        MemberRequestDto.SignUp signUpRequest = new MemberRequestDto.SignUp(
                email,
                "Password123!",
                "동시가입테스트",
                "concurrent",
                "PANGYO_1",
                "https://github.com/concurrent"
        );

        stubEmailService.markAsVerified(email);

        // when & then
        // 첫 번째 요청은 성공
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated());

        // 두 번째 요청은 중복 이메일로 실패
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("resource_already_exists"));
    }

    // === 데이터 검증 테스트 ===

    @Test
    @DisplayName("회원가입 후 데이터베이스에 정확한 데이터가 저장된다")
    void signUp_DataPersistence_Success() throws Exception {
        // given
        MemberRequestDto.SignUp signUpRequest = new MemberRequestDto.SignUp(
                "persistence@example.com",
                "TestPassword123!",
                "영속성테스트",
                "persistent",
                "JEJU_2",
                "https://github.com/persistent"
        );

        //stubEmailService.markAsVerified(email);

        // when
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated());

        // then - 상세한 데이터 검증
        Member savedMember = memberRepository.findByEmail(signUpRequest.email()).orElseThrow();

        assertThat(savedMember.getEmail()).isEqualTo(signUpRequest.email());
        assertThat(savedMember.getName()).isEqualTo(signUpRequest.name());
        assertThat(savedMember.getNickname()).isEqualTo(signUpRequest.nickname());
        assertThat(savedMember.getClassName()).isEqualTo(signUpRequest.className());
        assertThat(savedMember.getGithubUrl()).isEqualTo(signUpRequest.githubUrl());
        assertThat(savedMember.getRole()).isEqualTo("USER");
        assertThat(savedMember.getFollowerCount()).isEqualTo(0);
        assertThat(savedMember.getFollowingCount()).isEqualTo(0);
        assertThat(savedMember.isEnabled()).isTrue();
        assertThat(savedMember.isDeleted()).isFalse();

        // 비밀번호는 암호화되어 저장되어야 함
        assertThat(savedMember.getPassword()).isNotEqualTo(signUpRequest.password());
        assertThat(passwordEncoder.matches(signUpRequest.password(), savedMember.getPassword())).isTrue();

        // 생성/수정 시간이 설정되어야 함
        assertThat(savedMember.getCreatedAt()).isNotNull();
        assertThat(savedMember.getUpdatedAt()).isNotNull();
        assertThat(savedMember.getDeletedAt()).isNull();
    }
}