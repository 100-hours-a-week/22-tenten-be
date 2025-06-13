package com.kakaobase.snsapp.domain.members.service;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
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

        return new MemberResponseDto.ProfileImageChange(request.imageUrl());
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

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return Long.valueOf(userDetails.getId());
    }
}