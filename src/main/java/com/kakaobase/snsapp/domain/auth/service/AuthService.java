package com.kakaobase.snsapp.domain.auth.service;

import com.kakaobase.snsapp.domain.auth.converter.AuthConverter;
import com.kakaobase.snsapp.domain.auth.dto.AuthRequestDto;
import com.kakaobase.snsapp.domain.auth.dto.AuthResponseDto;
import com.kakaobase.snsapp.domain.auth.entity.AuthToken;
import com.kakaobase.snsapp.domain.auth.exception.AuthErrorCode;
import com.kakaobase.snsapp.domain.auth.exception.AuthException;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetailsService;
import com.kakaobase.snsapp.domain.auth.repository.AuthTokenRepository;
import com.kakaobase.snsapp.domain.auth.util.CookieUtil;

import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 사용자 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SecurityTokenManager securityTokenManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthConverter authConverter;
    private final MemberRepository memberRepository;
    private final AuthTokenRepository authTokenRepository;

    /**
     * 사용자 로그인 처리 및 인증 토큰 발급
     */
    public AuthResponseDto.UserAuthInfo login(AuthRequestDto.Login request) {

        String email = request.email();
        String password = request.password();
        CustomUserDetails userDetails;

        //이메일 인증 겸 인증객체 생성
        try{
            userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(email);
        }catch (UsernameNotFoundException e){
            throw new AuthException(GeneralErrorCode.RESOURCE_NOT_FOUND, email);
        }

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }

        // 3. 모든 검증이 완료된 후에 인증 객체 저장
        setCustomUserDetails(userDetails);

        return authConverter.toUserAuthInfoDto(userDetails);
    }


    @Transactional
    public AuthResponseDto.UserAuthInfo getUserInfo(String oldRefreshToken) {

        validateRefreshToken(oldRefreshToken);

        AuthToken refreshToken = securityTokenManager.findByRefreshToken(oldRefreshToken);

        Long memberId = refreshToken.getMemberId();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));

        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(member.getEmail());

        setCustomUserDetails(userDetails);

        return authConverter.toUserAuthInfoDto(userDetails);
    }

    //ContextHolder를 사용하여 AccessCookie생성
    public ResponseCookie getAccessCookie() {

        CustomUserDetails userDetails = getCustomUserDetails();

        String accessToken = jwtTokenProvider.createAccessToken(userDetails);

        return cookieUtil.createTokenToAccessCookie(accessToken);
    }

    //refresh토큰 쿠키 생성
    @Transactional
    public ResponseCookie getRefreshCookie(Long userId, String oldRefreshToken, String userAgent){

        // 기존 리프레시 토큰이 있다면 기존 토큰 파기
        if (securityTokenManager.isExistRefreshToken(oldRefreshToken)){
            securityTokenManager.revokeRefreshToken(oldRefreshToken);
        }
        String refreshToken = securityTokenManager.createRefreshToken(
                userId,
                userAgent
        );
        return cookieUtil.createRefreshTokenToCookie(refreshToken);
    }

    /**
     * 로그아웃 처리
     */
    @Transactional
    public void logout(String oldRefreshToken) {
        // 토큰이 있다면 기존 토큰 파기
        if (securityTokenManager.isExistRefreshToken(oldRefreshToken)) {
            securityTokenManager.revokeRefreshToken(oldRefreshToken);
        }
    }

    private void setCustomUserDetails(CustomUserDetails userDetails) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private CustomUserDetails getCustomUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 인증 객체가 없거나, 익명 사용자일 경우 예외 처리
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new AuthException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
        return (CustomUserDetails) auth.getPrincipal();
    }


    /**
     * 리프레시 토큰 검증
     */
    private void validateRefreshToken(String rawToken) {

        // 1. 취소된 토큰인지 확인
        if (securityTokenManager.isRevokedToken(rawToken)) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_REVOKED);
        }

        // 2. 토큰 조회
        AuthToken refreshToken = securityTokenManager.findByRefreshToken(rawToken);

        // 3. 만료 확인
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            securityTokenManager.revokeRefreshToken(rawToken);
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }
    }
}