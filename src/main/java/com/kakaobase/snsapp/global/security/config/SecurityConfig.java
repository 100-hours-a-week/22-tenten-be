package com.kakaobase.snsapp.global.security.config;

import com.kakaobase.snsapp.global.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * Spring Security 설정을 담당하는 클래스입니다.
 * JWT 인증, 인가 정책, CORS/CSRF 설정 등을 구성합니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CorsFilter corsFilter;

    /**
     * Spring Security 필터 체인을 구성합니다.
     *
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain
     * @throws Exception 보안 설정 중 예외 발생 시
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 보호 비활성화 (REST API는 상태를 저장하지 않으므로 CSRF 공격으로부터 안전)
                .csrf(AbstractHttpConfigurer::disable)
                // 세션 관리 - STATELESS로 설정하여 세션을 사용하지 않음
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // HTTP 기본 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // 폼 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                //cors설정
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
                // URL 기반 인가 설정
                .authorizeHttpRequests(auth -> auth
                        //Swagger관련 경로들
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        // 인증 없이 접근 가능한 경로들
                        .requestMatchers("/auth/tokens", "/auth/tokens/refresh").permitAll()
                        .requestMatchers("/users/email/verification-requests").permitAll()
                        .requestMatchers("/users/email/verification").permitAll()
                        .requestMatchers("/users").permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                // 예외 처리 설정
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)  // 인증 실패 처리
                        .accessDeniedHandler(accessDeniedHandler)  // 인가 실패 처리
                )
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 전에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * AuthenticationManager 빈을 제공합니다.
     * 로그인 API에서 사용자 자격 증명을 검증할 때 사용됩니다.
     *
     * @param configuration AuthenticationConfiguration 객체
     * @return AuthenticationManager 인스턴스
     * @throws Exception 구성 중 예외 발생 시
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * 비밀번호 인코더를 제공합니다.
     * BCrypt 알고리즘을 사용하여 비밀번호를 안전하게 해시합니다.
     *
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}