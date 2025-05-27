package com.kakaobase.snsapp.domain.auth.controller;

import com.kakaobase.snsapp.domain.auth.repository.AuthTokenRepository;
import com.kakaobase.snsapp.domain.auth.repository.RevokedRefreshTokenRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.factory.TestMemberFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private RevokedRefreshTokenRepository revokedRefreshTokenRepository;

    @Autowired
    private TestMemberFactory memberFactory;

    private Member ktbMember;
    private Member nonKtbMember;
    private Member bannedMember;

    @BeforeEach
    void setUp() {
        // TestMemberFactory 사용하여 테스트 멤버들 생성
        ktbMember = memberRepository.save(memberFactory.createKtbMember());
        nonKtbMember = memberRepository.save(memberFactory.createNonKtbMember());
        bannedMember = memberRepository.save(memberFactory.createBannedMember());
    }

    @Test
    @DisplayName("로그인 성공 - KBT 멤버")
    void loginSuccessKbtMember() throws Exception {
        // Given
        String loginRequest = """
            {
                "email": "test@example.com",
                "password": "Test1234!"
            }
            """;

        // When
        MvcResult result = mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest)
                        .header("User-Agent", "Test-Browser/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인에 성공하였습니다"))
                .andExpect(jsonPath("$.data.member_id").value(ktbMember.getId()))
                .andExpect(jsonPath("$.data.nickname").value("testuser"))
                .andExpect(jsonPath("$.data.class_name").value("PANGYO_1"))
                .andExpect(jsonPath("$.data.access_token").exists())
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        // Then: Set-Cookie 헤더에서 RefreshToken 쿠키 확인
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader)
                .contains("kakaobase_refresh_token=")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .contains("Path=/auth/tokens");

        // DB에 AuthToken이 저장되었는지 확인
        assertThat(authTokenRepository.findAllByMemberId(ktbMember.getId())).hasSize(1);
    }

    @Test
    @DisplayName("로그인 성공 - Non-KBT 멤버")
    void loginSuccessNonKbtMember() throws Exception {
        // Given
        String loginRequest = """
            {
                "email": "nonkbt@example.com",
                "password": "Test1234!"
            }
            """;

        // When & Then
        mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest)
                        .header("User-Agent", "Test-Browser/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.class_name").value("ALL"));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void loginFailWithInvalidEmail() throws Exception {
        // Given
        String loginRequest = """
            {
                "email": "notfound@example.com",
                "password": "Test1234!"
            }
            """;

        // When & Then
        mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest)
                        .header("User-Agent", "Test-Browser/1.0"))
                .andExpect(status().isNotFound());

        // DB에 AuthToken이 저장되지 않았는지 확인
        assertThat(authTokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void loginFailWithWrongPassword() throws Exception {
        // Given
        String loginRequest = """
            {
                "email": "test@example.com",
                "password": "WrongPassword!"
            }
            """;

        // When & Then
        mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest)
                        .header("User-Agent", "Test-Browser/1.0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인 실패 - 밴된 사용자")
    void loginFailWithBannedUser() throws Exception {
        // Given
        String loginRequest = """
            {
                "email": "banned@example.com",
                "password": "Test1234!"
            }
            """;

        // When & Then
        mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest)
                        .header("User-Agent", "Test-Browser/1.0"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("로그인 시 기존 RefreshToken 쿠키가 있으면 무효화")
    void loginWithExistingRefreshToken() throws Exception {
        // Given: 먼저 로그인하여 RefreshToken 생성
        String loginRequest = """
            {
                "email": "test@example.com",
                "password": "Test1234!"
            }
            """;

        MvcResult firstLogin = mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest)
                        .header("User-Agent", "Test-Browser/1.0"))
                .andExpect(status().isOk())
                .andReturn();

        String firstRefreshToken = extractRefreshTokenFromSetCookie(firstLogin);
        assertThat(authTokenRepository.findAllByMemberId(ktbMember.getId())).hasSize(1);

        // When: 기존 RefreshToken 쿠키와 함께 재로그인
        MvcResult secondLogin = mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest)
                        .header("User-Agent", "Test-Browser/1.0")
                        .cookie(new Cookie("kakaobase_refresh_token", firstRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        String secondRefreshToken = extractRefreshTokenFromSetCookie(secondLogin);

        // Then: 새로운 토큰이 발급되었고, 기존 토큰은 무효화됨
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

        // 기존 토큰은 RevokedRefreshToken에 저장되어야 함
        assertThat(revokedRefreshTokenRepository.findAll()).hasSize(1);

        // 새로운 토큰은 AuthToken에 저장되어야 함
        assertThat(authTokenRepository.findAllByMemberId(ktbMember.getId())).hasSize(1);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void refreshTokenSuccess() throws Exception {
        // Given: 로그인하여 RefreshToken 획득
        String loginRequest = """
            {
                "email": "test@example.com",
                "password": "Test1234!"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest)
                        .header("User-Agent", "Test-Browser/1.0"))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = extractRefreshTokenFromSetCookie(loginResult);

        // When: RefreshToken으로 새 AccessToken 요청
        mockMvc.perform(post("/auth/tokens/refresh")
                        .cookie(new Cookie("kakaobase_refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Access Token이 재발급되었습니다."))
                .andExpect(jsonPath("$.data.access_token").exists());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - RefreshToken 없음")
    void refreshTokenFailWithoutCookie() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/tokens/refresh"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 빈 RefreshToken")
    void refreshTokenFailWithEmptyToken() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/tokens/refresh")
                        .cookie(new Cookie("kakaobase_refresh_token", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 너무 짧은 RefreshToken")
    void refreshTokenFailWithShortToken() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/tokens/refresh")
                        .cookie(new Cookie("kakaobase_refresh_token", "short")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logoutSuccess() throws Exception {
        // Given: 로그인하여 RefreshToken 획득
        String loginRequest = """
            {
                "email": "test@example.com",
                "password": "Test1234!"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest)
                        .header("User-Agent", "Test-Browser/1.0"))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = extractRefreshTokenFromSetCookie(loginResult);
        assertThat(authTokenRepository.findAllByMemberId(ktbMember.getId())).hasSize(1);

        // When: 로그아웃
        MvcResult logoutResult = mockMvc.perform(delete("/auth/tokens")
                        .cookie(new Cookie("kakaobase_refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("정상적으로 로그아웃되었습니다."))
                .andExpect(header().exists("Set-Cookie"))
                .andReturn();

        // Then: 응답에 빈 쿠키가 설정되었는지 확인
        String setCookieHeader = logoutResult.getResponse().getHeader("Set-Cookie");
        assertThat(setCookieHeader)
                .contains("kakaobase_refresh_token=")
                .contains("Max-Age=0"); // 즉시 만료

        // DB에서 AuthToken이 삭제되고 RevokedToken에 추가되었는지 확인
        assertThat(authTokenRepository.findAllByMemberId(ktbMember.getId())).isEmpty();
        assertThat(revokedRefreshTokenRepository.findAll()).hasSize(1);

        // 로그아웃 후 같은 토큰으로 재발급 시도하면 실패해야 함
        mockMvc.perform(post("/auth/tokens/refresh")
                        .cookie(new Cookie("kakaobase_refresh_token", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃 - RefreshToken 없이도 성공 (graceful handling)")
    void logoutWithoutRefreshToken() throws Exception {
        // When & Then: RefreshToken 없이도 로그아웃은 성공해야 함
        mockMvc.perform(delete("/auth/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("로그아웃 - 짧은 RefreshToken으로도 성공")
    void logoutWithShortRefreshToken() throws Exception {
        // When & Then
        mockMvc.perform(delete("/auth/tokens")
                        .cookie(new Cookie("kakaobase_refresh_token", "short")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("User-Agent 헤더 없이 로그인 시도 - 실패")
    void loginWithoutUserAgent() throws Exception {
        // Given
        String loginRequest = """
            {
                "email": "test@example.com",
                "password": "Test1234!"
            }
            """;

        // When & Then: User-Agent 헤더가 required=true이므로 실패해야 함
        mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효하지 않은 요청 데이터로 로그인 시도")
    void loginWithInvalidData() throws Exception {
        // Given - 이메일 형식 오류
        String invalidEmailRequest = """
            {
                "email": "invalid-email-format",
                "password": "Test1234!"
            }
            """;

        // When & Then
        mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEmailRequest)
                        .header("User-Agent", "Test-Browser/1.0"))
                .andExpect(status().isBadRequest());

        // Given - 빈 비밀번호
        String emptyPasswordRequest = """
            {
                "email": "test@example.com",
                "password": ""
            }
            """;

        mockMvc.perform(post("/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyPasswordRequest)
                        .header("User-Agent", "Test-Browser/1.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("여러 멤버 생성 팩토리 테스트")
    void multipleMembers() {
        // Given & When: TestMemberFactory로 여러 멤버 생성
        List<Member> members = memberFactory.createMultipleMembers(5);
        memberRepository.saveAll(members);

        // Then: 모든 멤버가 다른 정보를 가지고 있는지 확인
        assertThat(members).hasSize(5);

        // 이메일이 모두 다른지 확인
        Set<String> emails = members.stream()
                .map(Member::getEmail)
                .collect(Collectors.toSet());
        assertThat(emails).hasSize(5);

        // 닉네임이 모두 다른지 확인
        Set<String> nicknames = members.stream()
                .map(Member::getNickname)
                .collect(Collectors.toSet());
        assertThat(nicknames).hasSize(5);
    }

    // ===================================
    // 헬퍼 메서드들
    // ===================================

    /**
     * Set-Cookie 헤더에서 RefreshToken 값 추출
     */
    private String extractRefreshTokenFromSetCookie(MvcResult result) {
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        if (setCookieHeader != null && setCookieHeader.contains("kakaobase_refresh_token=")) {
            // "kakaobase_refresh_token=VALUE; other-attributes" 형태에서 VALUE 추출
            String[] parts = setCookieHeader.split(";");
            for (String part : parts) {
                if (part.trim().startsWith("kakaobase_refresh_token=")) {
                    return part.trim().substring("kakaobase_refresh_token=".length());
                }
            }
        }
        return null;
    }

    /**
     * ResponseCookie 형태의 쿠키에서 값 추출 (필요시 사용)
     */
    private String extractTokenValueFromResponseCookie(String cookieString) {
        // ResponseCookie.toString() 결과에서 값 추출
        if (cookieString.contains("kakaobase_refresh_token=")) {
            int start = cookieString.indexOf("kakaobase_refresh_token=") + "kakaobase_refresh_token=".length();
            int end = cookieString.indexOf(";", start);
            if (end == -1) end = cookieString.length();
            return cookieString.substring(start, end);
        }
        return null;
    }
}