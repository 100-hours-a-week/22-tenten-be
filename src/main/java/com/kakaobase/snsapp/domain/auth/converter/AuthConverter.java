package com.kakaobase.snsapp.domain.auth.converter;

import com.kakaobase.snsapp.domain.auth.dto.AuthRequestDto;
import com.kakaobase.snsapp.domain.auth.dto.AuthResponseDto;
import com.kakaobase.snsapp.domain.auth.entity.AuthToken;
import com.kakaobase.snsapp.domain.auth.entity.RevokedRefreshToken;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.members.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Auth 도메인의 객체 변환을 담당하는 컨버터입니다.
 * DTO와 Entity 사이의 변환을 처리합니다.
 */
@Component
@RequiredArgsConstructor
public class AuthConverter {

    /**
     * 로그인 정보와 생성된 토큰 정보로 AuthToken 엔티티를 생성합니다.
     *
     * @param memberId 회원 ID
     * @param refreshTokenHash 리프레시 토큰 해시 값
     * @param userAgent 사용자 브라우저 정보
     * @param expirationTime 토큰 만료 시간
     * @return 생성된 AuthToken 엔티티
     */
    public AuthToken toAuthTokenEntity(
            Long memberId,
            String refreshTokenHash,
            String userAgent,
            LocalDateTime expirationTime
    ) {
        // 기기 ID 생성 (UUID v4)
        String deviceId = UUID.randomUUID().toString();

        // AuthToken 엔티티 생성 및 반환
        return AuthToken.builder()
                .memberId(memberId)
                .refreshTokenHash(refreshTokenHash)
                .deviceId(deviceId)
                .userAgent(userAgent)
                .expiresAt(expirationTime)
                .build();
    }

    /**
     * 취소할 토큰 정보로 RevokedRefreshToken 엔티티를 생성합니다.
     *
     * @param refreshTokenHash 취소할 리프레시 토큰 해시 값
     * @param memberId 회원 ID
     * @return 생성된 RevokedRefreshToken 엔티티
     */
    public RevokedRefreshToken toRevokedTokenEntity(String refreshTokenHash, Long memberId) {
        return RevokedRefreshToken.builder()
                .refreshTokenHash(refreshTokenHash)
                .memberId(memberId)
                .build();
    }

    /**
     * 액세스 토큰과 리프레시 토큰으로 응답 DTO를 생성합니다.
     *
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰 (클라이언트에게 직접 보내지 않고 쿠키에 설정됨)
     * @return 토큰 응답 DTO
     */
    public AuthResponseDto.TokenResponse toTokenResponseDto(String accessToken, String refreshToken) {
        // refreshToken은 파라미터로 받지만 실제로는 쿠키에 설정되므로 응답 DTO에는 포함되지 않음
        return new AuthResponseDto.TokenResponse(accessToken);
    }

    /**
     * 액세스 토큰만으로 응답 DTO를 생성합니다.
     * 리프레시 토큰은 쿠키로 전송되므로 응답 본문에는 포함되지 않습니다.
     *
     * @param accessToken 액세스 토큰
     * @return 토큰 응답 DTO
     */
    public AuthResponseDto.TokenResponse toAccessTokenOnlyResponseDto(String accessToken) {
        return new AuthResponseDto.TokenResponse(accessToken);
    }

    public AuthResponseDto.LoginResponse toLoginResponseDto(CustomUserDetails userDetails, String accessToken) {
        return AuthResponseDto.LoginResponse
                .builder()
                .memberId(Long.valueOf(userDetails.getId()))
                .nickname(userDetails.getNickname())
                .className(userDetails.getClassName())
                .imageUrl(userDetails.getProfileImgUrl())
                .accessToken(accessToken)
                .build();

    }

    public AuthResponseDto.LoginResponse toLoginResponseDto(Member member, String accessToken) {
        return AuthResponseDto.LoginResponse
                .builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .className(member.getClassName())
                .imageUrl(member.getProfileImgUrl())
                .accessToken(accessToken)
                .build();

    }
}