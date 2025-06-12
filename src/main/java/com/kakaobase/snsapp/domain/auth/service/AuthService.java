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
import com.kakaobase.snsapp.global.common.redis.CacheRecord;
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

import java.time.Duration;
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

    //AccessToken재발급 시 응답Dto반환 메서드
    @Transactional
    public AuthResponseDto.UserAuthInfo getUserInfo(String oldRefreshToken) {

        var cache = securityTokenManager.getUserAuthCache(oldRefreshToken);

        // 1차로 캐싱 조회
        if( cache != null) {
            return authConverter.toUserAuthInfoDto(cache);
        }

        // 없으면 유효성 검사 후 해당 AuthToken가져옴
        AuthToken refreshToken= securityTokenManager.validateRefreshToken(oldRefreshToken);

        // 유효하면 해당 refresh토큰 캐싱
        Long memberId = refreshToken.getMemberId();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));

        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(member.getEmail());

        securityTokenManager.cacheRefreshToken(oldRefreshToken, userDetails, refreshToken);
        setCustomUserDetails(userDetails);

        return authConverter.toUserAuthInfoDto(userDetails);
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

        CustomUserDetails userDetails = getCustomUserDetails();

        //refresh토큰 캐싱시도
        if(!securityTokenManager.cacheRefreshToken(refreshToken, userDetails)){
            log.error("리프래시 토큰 저장 실패. 유저이름: {}", userDetails.getName());
        }

        return cookieUtil.createRefreshTokenToCookie(refreshToken);
    }

    //ContextHolder를 사용하여 AccessCookie생성
    public ResponseCookie getAccessCookie(String refreshToken) {

        var cache = securityTokenManager.getUserAuthCache(refreshToken);
        // 캐시 조회
        if( cache != null) {
            String accessToken = jwtTokenProvider.createAccessToken(cache);
            return cookieUtil.createTokenToAccessCookie(accessToken);
        }

        // 캐싱 실패시 ContexHoler의 정보로 AccessToken 생성
        CustomUserDetails userDetails = getCustomUserDetails();

        String accessToken = jwtTokenProvider.createAccessToken(userDetails);

        return cookieUtil.createTokenToAccessCookie(accessToken);
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

}