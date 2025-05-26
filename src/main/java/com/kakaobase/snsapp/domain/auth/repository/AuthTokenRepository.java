package com.kakaobase.snsapp.domain.auth.repository;

import com.kakaobase.snsapp.domain.auth.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    /**
     * RefreshToken 해시값으로 AuthToken 조회
     *
     * @param refreshTokenHash 해시된 리프레시 토큰
     * @return 해당 토큰이 존재하면 Optional 반환
     */
    Optional<AuthToken> findByRefreshTokenHash(String refreshTokenHash);


    boolean existsByRefreshTokenHash(String refreshTokenHash);

    /**
     * 디바이스 ID와 사용자 ID로 AuthToken 조회 (디바이스 기반 로그아웃용)
     *
     * @param deviceId 디바이스 식별자
     * @param memberId 사용자 ID
     * @return 해당 토큰이 존재하면 Optional 반환
     */
    Optional<AuthToken> findByDeviceIdAndMemberId(String deviceId, Long memberId);

    /**
     * 사용자 ID로 발급된 모든 AuthToken 조회
     *
     * @param memberId 사용자 ID
     * @return 해당 사용자의 모든 토큰 리스트
     */
    List<AuthToken> findAllByMemberId(Long memberId);

    /**
     * 특정 디바이스와 사용자에 해당하는 토큰 삭제 (디바이스 로그아웃)
     *
     * @param deviceId 디바이스 ID
     * @param memberId 사용자 ID
     * @return 삭제된 행 수
     */
    @Modifying
    int deleteByDeviceIdAndMemberId(String deviceId, Long memberId);

    /**
     * 특정 만료 시간 이전에 만료된 토큰들을 삭제합니다.
     * 데이터베이스 정리 작업에 사용됩니다.
     *
     * @param expirationTime 기준 만료 시간
     * @return 삭제된 행 수
     */
    @Modifying
    @Query("DELETE FROM AuthToken a WHERE a.expiresAt < :expirationTime")
    int deleteAllExpiredTokensBefore(@Param("expirationTime") LocalDateTime expirationTime);

}
