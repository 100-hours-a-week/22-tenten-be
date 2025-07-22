package com.kakaobase.snsapp.global.config;

import com.kakaobase.snsapp.global.common.metrics.QueryCountInspector;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 관련 설정을 담당하는 설정 클래스입니다.
 * JPA Auditing 기능을 활성화하여 엔티티의 생성 시간과 수정 시간을 자동으로 관리합니다.
 */
@Configuration
@EnableJpaAuditing
@RequiredArgsConstructor
public class JpaConfig {

    private final QueryCountInspector queryCountInspector;

    /**
     * Hibernate Properties Customizer를 통해 QueryCountInspector를 등록합니다.
     * application.yml 설정 대신 Java Configuration으로 직접 Bean을 주입합니다.
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            hibernateProperties.put("hibernate.session_factory.statement_inspector", queryCountInspector);
        };
    }
}