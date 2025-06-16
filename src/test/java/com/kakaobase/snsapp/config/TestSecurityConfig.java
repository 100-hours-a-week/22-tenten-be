package com.kakaobase.snsapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 테스트 환경용 Security 설정 클래스
 *
 * 특징:
 * - @PreAuthorize 어노테이션은 활성화 상태 유지 (@EnableMethodSecurity)
 * - 모든 HTTP 요청을 permitAll()로 설정하여 인증 절차 우회
 * - 실제 운영 환경과 동일한 메서드 레벨 보안 테스트 가능
 * - JWT 필터 체인 없이도 Security Context 설정 가능
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize 활성화
public class TestSecurityConfig {

    /**
     * 테스트용 Security Filter Chain 설정
     *
     * - 모든 요청을 인증 없이 허용
     * - CSRF, 세션, 기본 인증 비활성화
     * - @PreAuthorize는 별도 테스트에서 확인
     */
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 보호 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 관리 - STATELESS로 설정
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // HTTP 기본 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)

                // 폼 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)

                // 모든 요청 허용 (테스트 환경)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                .build();
    }

    /**
     * 테스트용 AuthenticationManager
     *
     * - 모든 인증 요청을 성공으로 처리
     * - 실제 인증 로직 우회
     */
    @Bean
    @Primary
    public AuthenticationManager testAuthenticationManager() {
        return authentication -> {
            // 테스트 환경에서는 모든 인증을 성공으로 처리
            return new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(),
                    authentication.getCredentials(),
                    authentication.getAuthorities()
            );
        };
    }

    /**
     * 테스트용 PasswordEncoder
     *
     * - 실제 운영 환경과 동일한 BCrypt 사용
     * - 비밀번호 암호화/검증 테스트에 필요
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}