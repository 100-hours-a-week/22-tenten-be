package com.kakaobase.snsapp.domain.auth.principal;

import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import com.kakaobase.snsapp.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security의 UserDetailsService 인터페이스를 구현한 서비스 클래스입니다.
 * 사용자 인증 시 DB에서 사용자 정보를 조회하여 인증 객체로 변환합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * 이메일로 사용자를 조회하여 UserDetails 객체로 반환합니다.
     *
     * @param email 사용자 이메일
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우 발생
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("사용자 인증 정보 조회: {}", email);

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("이메일로 사용자를 찾을 수 없습니다: {}", email);
                    throw new UsernameNotFoundException("이메일로 사용자를 찾을 수 없습니다: " + email);
                });

        // 로그인 시 사용하는 생성자 사용 (이메일, 비밀번호 포함)
        return new CustomUserDetails(
                member.getEmail(),
                member.getPassword(),
                member.getNickname(),
                member.getId().toString(),
                member.getRole(),
                member.getClassName(),
                member.getProfileImgUrl(),
                member.isEnabled()
        );
    }

    /**
     * 사용자 ID로 사용자를 조회하여 UserDetails 객체로 반환합니다.
     * 주로 JWT 인증 시 사용됩니다.
     *
     * @param id 사용자 ID
     * @return UserDetails 객체
     * @throws CustomException 사용자를 찾을 수 없는 경우 발생
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(String id) {
        log.debug("ID로 사용자 인증 정보 조회: {}", id);

        Long memberId;
        try {
            memberId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            log.debug("잘못된 사용자 ID 형식: {}", id);
            throw new CustomException(GeneralErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 사용자 ID 형식입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.debug("ID로 사용자를 찾을 수 없습니다: {}", id);
                    throw new CustomException(GeneralErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다.");
                });


        return new CustomUserDetails(
                member.getId().toString(),
                member.getRole(),
                member.getClassName(),
                member.isEnabled());
    }
}