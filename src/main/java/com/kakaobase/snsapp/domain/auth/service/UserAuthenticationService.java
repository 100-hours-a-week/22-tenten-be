package com.kakaobase.snsapp.domain.auth.service;

import com.kakaobase.snsapp.domain.auth.converter.AuthConverter;
import com.kakaobase.snsapp.domain.auth.dto.AuthResponseDto;
import com.kakaobase.snsapp.domain.auth.entity.AuthToken;
import com.kakaobase.snsapp.domain.auth.entity.RevokedRefreshToken;
import com.kakaobase.snsapp.domain.auth.exception.AuthErrorCode;
import com.kakaobase.snsapp.domain.auth.exception.AuthException;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetailsService;
import com.kakaobase.snsapp.domain.auth.repository.AuthTokenRepository;
import com.kakaobase.snsapp.domain.auth.repository.RevokedRefreshTokenRepository;
import com.kakaobase.snsapp.domain.auth.util.CookieUtil;

import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import com.kakaobase.snsapp.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final MemberRepository memberRepository;
    private final SecurityTokenManager securityTokenManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;

    /**
     * 사용자 로그인 처리 및 인증 토큰 발급
     */
    @Transactional
    public AuthResponseDto.LoginResponse login(String email, String password, String userAgent, String providedRefreshToken, HttpServletResponse response) {
        // 1. 이메일로 사용자 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(GeneralErrorCode.RESOURCE_NOT_FOUND, email));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }

        // 3. CustomUserDetailsService를 사용하여 인증 객체 생성
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserById(member.getId().toString());


        // 토큰이 있다면 기존 토큰 파기
        if (!providedRefreshToken.isBlank()) {
            securityTokenManager.revokeRefreshToken(providedRefreshToken);
        }

        // 5. Refresh Token 발급 및 저장
        String refreshToken = securityTokenManager.createRefreshToken(
                Long.parseLong(userDetails.getId()),
                userAgent
        );

        // 6. 액세스 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(userDetails);


        // 7. 쿠키에 RefreshToken주입
        cookieUtil.createRefreshTokenCookie(refreshToken, response);

        return new AuthResponseDto.LoginResponse(member.getId(), member.getNickname(),member.getClassName(),member.getProfileImgUrl(), accessToken);
    }

    /**
     * 리프레시 토큰을 사용해 새 액세스 토큰 발급
     */
    @Transactional
    public String refreshAuthentication(HttpServletRequest httpRequest) {

        // 1. 쿠키에서 리프레시 토큰 추출
        String refreshToken = cookieUtil.extractRefreshTokenFromCookie(httpRequest);

        // 토큰이 없으면 예외 발생
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_MISSING);
        }

        // 1. 리프레시 토큰 검증 및 사용자 ID 추출
        Long userId = securityTokenManager.validateRefreshTokenAndGetUserId(refreshToken);

        // 2. CustomUserDetailsService를 통해 인증 정보 로드
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserById(String.valueOf(userId));


        // 4. 새 액세스 토큰 발급
        return jwtTokenProvider.createAccessToken(userDetails);
    }

    /**
     * 로그아웃 처리
     */
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {

        log.info("로그아웃 리퀘스트: {}", request.toString());
        // 1. 쿠키에서 리프레시 토큰 추출
        String refreshToken = cookieUtil.extractRefreshTokenFromCookie(request);

        // 토큰이 없으면 예외 발생
        if (refreshToken == null) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_MISSING);
        }

        // 2. 토큰이 존재하면 무효화 처리
        securityTokenManager.revokeRefreshToken(refreshToken);

        // 3. 쿠키에서 리프레시 토큰 제거
        response.addCookie(cookieUtil.clearRefreshTokenCookie());

    }

    /**
     * 다른 모든 디바이스 로그아웃
     */
    @Transactional
    public void logoutOtherDevices(Long memberId, String currentRefreshToken) {
        if (currentRefreshToken == null) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_MISSING);
        }
        securityTokenManager.revokeAllTokensExcept(memberId, currentRefreshToken);
    }
}