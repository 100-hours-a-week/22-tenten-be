package com.kakaobase.snsapp.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String jwtSchemeName = "JWT TOKEN";

        return new OpenAPI()
                .info(new Info()
                        .title("KakaoBase API")
                        .description("KakaoBase의 API 명세서입니다.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(jwtSchemeName))
                .components(
                        new Components()
                                .addSecuritySchemes(jwtSchemeName,
                                        new SecurityScheme()
                                                .name(jwtSchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}