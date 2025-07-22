package com.kakaobase.snsapp.global.config;

import com.kakaobase.snsapp.interceptor.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 관련 설정을 담당하는 Configuration 클래스
 * 
 * <p>인터셉터, 리소스 핸들러, CORS 매핑 등의 MVC 설정을 관리합니다.</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    /**
     * 인터셉터를 등록합니다.
     * 
     * <p>LoggingInterceptor를 모든 URL 패턴에 적용하여 
     * HTTP 요청의 실행 시간과 쿼리 개수를 로깅합니다.</p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")  // 모든 URL 패턴에 적용
                .excludePathPatterns(
                        "/swagger-ui/**",    // Swagger UI 제외
                        "/v3/api-docs/**",   // OpenAPI 문서 제외
                        "/actuator/**"       // Actuator 엔드포인트 제외
                );
    }
}