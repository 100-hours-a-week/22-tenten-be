package com.kakaobase.snsapp.global.security.jwt;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetailsService;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * HTTP 요청에서 JWT 토큰을 추출하고 검증하여 Security Context에 인증 정보를 설정하는 필터입니다.
 * Spring Security 필터 체인에서 작동하며, 인증이 필요한 요청에 대해 실행됩니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtTokenValidator jwtTokenValidator;
    private final CustomUserDetailsService userDetailsService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 필터를 적용하지 않을 경로 패턴 목록
    private final List<String> excludedPaths = List.of(
            "/auth/tokens",
            "/auth/tokens/refresh",
            "/users/email/verification-requests",
            "/users/email/verification",
            "/users",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    );

    /**
     * 특정 경로에 대해 이 필터를 적용하지 않아야 하는지 결정합니다.
     * 인증이 필요 없는 경로들(로그인, 토큰 갱신, 회원가입 등)에는 필터를 적용하지 않습니다.
     *
     * @param request 현재 요청
     * @return true이면 필터를 적용하지 않음, false이면 필터 적용
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return excludedPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * JWT 토큰을 검증하고 인증 정보를 설정하는 필터 메서드입니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException IO 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 요청 정보 로깅
        log.debug("JwtAuthenticationFilter 실행 - URI: {}, 메서드: {}", request.getRequestURI(), request.getMethod());

        // 요청에서 JWT 토큰 추출
        String token = jwtUtil.resolveToken(request);
        log.debug("추출된 토큰: {}", token != null ? "존재함 (길이: " + token.length() + ")" : "없음");

        // 토큰이 존재하고 현재 인증 정보가 없는 경우에만 처리
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 토큰 유효성 검증
                if (jwtTokenValidator.validateToken(token)) {
                    String userId = jwtUtil.getSubject(token);

                    // DB에서 CustomUserDetails 조회
                    CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserById(userId);

                    //CustomUserDetails기반으로 인증 객체 생성
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT 인증 성공: {}", userId);
                }
            } catch (CustomException e) {
                // 토큰 검증 실패 시 로깅
                log.error("JWT 인증 실패: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // 다음 필터 실행
        filterChain.doFilter(request, response);
    }
}