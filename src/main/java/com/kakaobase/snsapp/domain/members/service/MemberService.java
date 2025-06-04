package com.kakaobase.snsapp.domain.members.service;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.comments.dto.BotRecommentRequestDto;
import com.kakaobase.snsapp.domain.members.converter.MemberConverter;
import com.kakaobase.snsapp.domain.members.dto.MemberRequestDto;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.exception.MemberErrorCode;
import com.kakaobase.snsapp.domain.members.exception.MemberException;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.common.email.service.EmailVerificationService;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
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

    /**
     * 닉네임으로 회원을 검색합니다.
     *
     * @param nickname 검색할 닉네임 (부분 일치)
     * @param limit 최대 검색 결과 수
     * @return 검색된 회원 ID 목록
     */
    @Transactional(readOnly = true)
    public List<Long> searchMembersByNickname(String nickname, int limit) {
        return memberRepository.findByNicknameContainingLimit(nickname, limit)
                .stream()
                .map(Member::getId)
                .collect(Collectors.toList());
    }

    /**
     * 닉네임으로 회원을 조회합니다.
     *
     * @param nickname 닉네임 (정확히 일치)
     * @return 회원 ID (없으면 null)
     */
    @Transactional(readOnly = true)
    public Long findIdByNickname(String nickname) {
        return memberRepository.findByNickname(nickname)
                .map(Member::getId)
                .orElse(null);
    }

    /**
     * 여러 닉네임에 대한 회원 ID를 일괄 조회합니다.
     *
     * @param nicknames 닉네임 목록
     * @return 닉네임을 키로 하고 회원 ID를 값으로 하는 맵
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getMemberIdsByNicknames(List<String> nicknames) {
        List<Member> members = memberRepository.findAllByNicknameIn(nicknames);

        return members.stream()
                .collect(Collectors.toMap(
                        Member::getNickname,
                        Member::getId
                ));
    }

    /**
     * 회원의 className(기수)을 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 회원의 className
     * @throws MemberException 회원을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public String getMemberClassName(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND, "memberId"));

        return member.getClassName();
    }

    /**
     * 회원이 존재하는지 확인합니다.
     *
     * @param memberId 회원 ID
     * @return 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long memberId) {
        return memberRepository.existsById(memberId);
    }

    /**
     * 회원의 닉네임과 기수 정보만 조회합니다.
     *
     * @param memberId 회원 ID
     * @return BotUser DTO (닉네임, 기수)
     * @throws MemberException 회원을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public BotRecommentRequestDto.UserInfo getMemberBotInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND, "memberId"));

        return new BotRecommentRequestDto.UserInfo(
                member.getNickname(),
                member.getClassName()
        );
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

    public MemberResponseDto.ProfileImageChange changProfileImageUrl(MemberRequestDto.@Valid ProfileImageChange request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        Member member = memberRepository.findById(Long.valueOf(userDetails.getId()))
                .orElseThrow(() -> new MemberException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));

        member.updateProfile(request.imageUrl());

        return new MemberResponseDto.ProfileImageChange(request.imageUrl());
    }
}