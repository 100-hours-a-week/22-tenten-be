package com.kakaobase.snsapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * 테스트 환경용 데이터베이스 설정 클래스
 *
 * H2 인메모리 데이터베이스 설정:
 * - 각 테스트 실행 시 새로운 데이터베이스 생성
 * - 테스트 종료 시 자동으로 데이터베이스 삭제
 * - MySQL 호환 모드로 실행하여 운영 환경과 유사한 환경 제공
 */
@TestConfiguration
@Profile("test")
public class TestDatabaseConfig {

    /**
     * 테스트용 H2 DataSource 설정
     *
     * 특징:
     * - 인메모리 데이터베이스로 빠른 테스트 실행
     * - MySQL 호환 모드로 실제 환경과 유사한 SQL 동작
     * - 각 테스트 클래스마다 독립적인 데이터베이스 인스턴스
     */
    @Bean
    @Primary
    @Profile("test")
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE")
                .build();
    }
}