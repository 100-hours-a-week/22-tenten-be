package com.kakaobase.snsapp.global.security.jwt;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetailsService;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
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
            "/api/auth/tokens",
            "/api/auth/tokens/refresh",
            "/api/users/email/verification-requests",
            "/api/users/email/verification",
            "/api/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**"
    );

    @Data
    @AllArgsConstructor
    private static class PathMethodPattern {
        private String pathPattern;
        private String method;

        public boolean matches(String path, String method, AntPathMatcher matcher) {
            return this.method.equals(method) && matcher.match(this.pathPattern, path);
        }
    }

    // 익명 사용자도 접근 가능한 경로 (임의 인증 객체 생성)
    private final List<PathMethodPattern> anonymousAccessiblePatterns = List.of(
            new PathMethodPattern("/api/comments/**", "GET"),
            new PathMethodPattern("/api/comments/*/likes", "GET"),
            new PathMethodPattern("/api/posts/*/comments", "GET"),
            new PathMethodPattern("/api/recomments/*/likes", "GET"),
            new PathMethodPattern("/api/comments/*/recomments", "GET"),
            new PathMethodPattern("/api/posts/**", "GET"),
            new PathMethodPattern("/api/users/**", "GET")
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        if (pathMatcher.match("/users", path) && method.equals("POST")) {
            return true;
        }

        return excludedPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("JwtAuthenticationFilter 실행 - URI: {}, 메서드: {}", request.getRequestURI(), request.getMethod());

        String path = request.getServletPath();
        String method = request.getMethod();

        // 익명 접근 가능한 경로인지 확인
        boolean isAnonymousAccessible = anonymousAccessiblePatterns.stream()
                .anyMatch(pattern -> pattern.matches(path, method, pathMatcher));

        String token = jwtUtil.resolveTokenFromCookie(request);

        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 토큰이 있으면 정상적인 인증 처리
                if (jwtTokenValidator.validateToken(token)) {
                    String userId = jwtUtil.getSubject(token);
                    CustomUserDetails userDetails = userDetailsService.loadUserById(userId);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT 인증 성공: {}", userId);
                }
            } catch (CustomException e) {
                log.error("JWT 인증 실패: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                return;
            }
        } else if (isAnonymousAccessible && !StringUtils.hasText(token) &&
                SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("토큰이 없는 익명 사용자 - 임시 인증 객체 생성");
            createAnonymousAuthentication();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 익명 사용자를 위한 임시 인증 객체 생성
     */
    private void createAnonymousAuthentication() {
        // 익명 사용자용 CustomUserDetails 생성
        CustomUserDetails anonymousUser = userDetailsService.loadUserById(BotConstants.BOT_MEMBER_ID.toString());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                anonymousUser,
                null,
                anonymousUser.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("익명 사용자 인증 객체 생성");
    }
}