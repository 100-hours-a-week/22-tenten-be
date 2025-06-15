package com.kakaobase.snsapp.domain.members.service;

import com.kakaobase.snsapp.domain.auth.entity.AuthToken;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.auth.repository.AuthTokenRepository;
import com.kakaobase.snsapp.domain.auth.service.AuthCacheService;
import com.kakaobase.snsapp.domain.auth.service.SecurityTokenManager;
import com.kakaobase.snsapp.domain.follow.dto.FollowCount;
import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.converter.MemberConverter;
import com.kakaobase.snsapp.domain.members.dto.MemberRequestDto;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.exception.MemberErrorCode;
import com.kakaobase.snsapp.domain.members.exception.MemberException;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.domain.posts.repository.PostRepository;
import com.kakaobase.snsapp.global.common.email.service.EmailVerificationService;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 회원 관련 비즈니스 로직을 처리하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberConverter memberConverter;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;
    private final EntityManager em;
    private final AuthTokenRepository authTokenRepository;
    private final AuthCacheService authCacheService;

    /**
     * 회원 가입 처리
     *
     * @param request 회원가입 요청 DTO
     * @throws MemberException 중복 이메일, 닉네임 또는 인증 미완료 시
     */
    @Transactional
    public void signUp(MemberRequestDto.SignUp request) {
        log.info("회원가입 처리 시작: {}", request.email());

        // 이메일 중복 검사
        if (memberRepository.existsByEmail(request.email())) {
            throw new MemberException(GeneralErrorCode.RESOURCE_ALREADY_EXISTS, "email");
        }

        // 이메일 인증 확인
        if (!emailVerificationService.isEmailVerified(request.email())) {
            throw new MemberException(MemberErrorCode.EMAIL_VERIFICATION_FAILED);
        }

        // Member 엔티티 생성 및 저장
        Member member = memberConverter.toEntity(request);
        memberRepository.save(member);

        log.info("회원가입 완료: {} (ID: {})", request.email(), member.getId());
    }

    /**
     * 회원 ID로 회원 정보를 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 회원 정보 (닉네임, 프로필 이미지)
     * @throws MemberException 회원을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public Map<String, String> getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND, "memberId"));

        Map<String, String> memberInfo = new HashMap<>();
        memberInfo.put("nickname", member.getNickname());
        memberInfo.put("imageUrl", member.getProfileImgUrl());

        return memberInfo;
    }

    /**
     * 여러 회원 ID에 대한 회원 정보를 일괄 조회합니다.
     *
     * @param memberIds 회원 ID 목록
     * @return 회원 ID를 키로 하고 회원 정보(닉네임, 프로필 이미지)를 값으로 하는 맵
     */
    @Transactional(readOnly = true)
    public Map<Long, Map<String, String>> getMemberInfoMapByIds(List<Long> memberIds) {
        List<Member> members = memberRepository.findAllByIdIn(memberIds);

        return members.stream()
                .collect(Collectors.toMap(
                        Member::getId,
                        member -> {
                            Map<String, String> info = new HashMap<>();
                            info.put("nickname", member.getNickname());
                            info.put("imageUrl", member.getProfileImgUrl());
                            return info;
                        }
                ));
    }


    @Transactional
    public void unregister() {
        log.debug("회원탈퇴 처리 시작");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        Member member = memberRepository.findById(Long.valueOf(userDetails.getId()))
                .orElseThrow(() -> new MemberException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));

        String email = member.getEmail();

        // 이메일 인증 확인
        if (!emailVerificationService.isEmailVerified(email)) {
            throw new MemberException(MemberErrorCode.EMAIL_VERIFICATION_FAILED);
        }

        // Member 엔티티 삭제
        member.softDelete();

    }

    @Transactional
    public void changePassword(MemberRequestDto.PasswordChange request) {
        log.debug("비밀번호 수정 시작");

        String email = request.email();
        String newPassword = request.NewPassword();

        // 이메일 인증 확인
        if (!emailVerificationService.isEmailVerified(email)) {
            throw new MemberException(MemberErrorCode.EMAIL_VERIFICATION_FAILED);
        }

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));

        member.updatePassword(passwordEncoder.encode(newPassword));

    }

    @Transactional
    public void changGithubUrl(MemberRequestDto.@Valid GithubUrlChange request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        Member member = memberRepository.findById(Long.valueOf(userDetails.getId()))
                .orElseThrow(() -> new MemberException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));

        member.updateGithubUrl(request.githubUrl());
    }

    @Transactional
    public MemberResponseDto.ProfileImageChange changProfileImageUrl(MemberRequestDto.@Valid ProfileImageChange request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        Member member = memberRepository.findById(Long.valueOf(userDetails.getId()))
                .orElseThrow(() -> new MemberException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));

        member.updateProfile(request.imageUrl());

        updateAuthCacheUserImage(member.getId(), request.imageUrl());

        return new MemberResponseDto.ProfileImageChange(request.imageUrl());
    }

    private void updateAuthCacheUserImage(Long memberId, String newImageUrl) {
        List<AuthToken> refreshTokens = authTokenRepository.findAllByMemberId(memberId);

        if (refreshTokens.isEmpty()) {
            log.debug("업데이트할 RefreshToken이 없음: memberId={}", memberId);
            return;
        }

        for (AuthToken authToken : refreshTokens) {
            try {
                String refreshTokenHash = authToken.getRefreshTokenHash();

                boolean updateResult = authCacheService.updateRefershCacheImage(refreshTokenHash, newImageUrl);

                if (updateResult) {
                    log.debug("캐시 이미지 업데이트 성공: memberId={}, tokenHash={}", memberId, refreshTokenHash);
                } else {
                    log.debug("캐시 이미지 업데이트 실패 혹은 해당 캐시 없음: memberId={}, tokenHash={}", memberId, refreshTokenHash);
                }
            } catch (Exception e) {
                log.error("개별 토큰 캐시 업데이트 중 예외 발생: authTokenId={}, memberId={}, error={}",
                        authToken.getId(), memberId, e.getMessage(), e);
            }
        }
    }

    @Transactional(readOnly = true)
    public MemberResponseDto.Mypage getMypageInfo(Long userId) {

        Member tagetMember = memberRepository.findById(userId)
                .orElseThrow(() -> new MemberException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));

        Long postCount = postRepository.countByMemberId(tagetMember.getId());

        Long currentUserId = getCurrentUserId();
        Member currentMember = em.getReference(Member.class, currentUserId);

        boolean isMine = false;
        boolean isFollowing = false;

        if (currentUserId.equals(tagetMember.getId())) {
            isMine = true;
        } else {
            isFollowing = followRepository.existsByFollowerUserAndFollowingUser(currentMember, tagetMember);
        }

        return memberConverter.toMypage(tagetMember, postCount, isMine, isFollowing);
    }

    @PostConstruct
    public void initializeFollowCounts() {
        try {
            syncMemberFollowCount();
            System.out.println("✅ 팔로우 카운트 동기화 완료");
        } catch (Exception e) {
            System.err.println("❌ 팔로우 카운트 동기화 실패: " + e.getMessage());
            // 실패해도 애플리케이션은 정상 시작되도록 예외를 삼킴
        }
    }

    @Transactional
    public void syncMemberFollowCount() {
        List<Member> members = memberRepository.findAll();

        // 배치 조회 (Record 방식)
        Map<Long, Long> followingCounts = followRepository.findFollowingCounts()
                .stream()
                .collect(Collectors.toMap(
                        FollowCount::memberId,
                        FollowCount::count
                ));

        Map<Long, Long> followerCounts = followRepository.findFollowerCounts()
                .stream()
                .collect(Collectors.toMap(
                        FollowCount::memberId,
                        FollowCount::count
                ));

        // 업데이트
        members.forEach(member -> {
            Long id = member.getId();

            // 팔로잉 카운트 동기화
            Integer newFollowingCount = followingCounts.getOrDefault(id, 0L).intValue();
            if (!member.getFollowingCount().equals(newFollowingCount)) {
                member.updateFollowingCount(newFollowingCount);
            }

            // 팔로워 카운트 동기화
            Integer newFollowerCount = followerCounts.getOrDefault(id, 0L).intValue();
            if (!member.getFollowerCount().equals(newFollowerCount)) {
                member.updateFollowerCount(newFollowerCount);
            }
        });
    }



    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return Long.valueOf(userDetails.getId());
    }
}