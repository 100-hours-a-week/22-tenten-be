package com.kakaobase.snsapp.domain.follow.service;


import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.follow.converter.FollowConverter;
import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.follow.exception.FollowErrorCode;
import com.kakaobase.snsapp.domain.follow.exception.FollowException;
import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.global.common.redis.error.CacheException;
import com.kakaobase.snsapp.global.error.code.GeneralErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final FollowConverter followConverter;
    private final MemberRepository memberRepository;
    private final FollowCacheService followCacheService;
    private final EntityManager em;


    @Transactional
    public void addFollowing(Long targetUserId, CustomUserDetails userDetails) {

        Long currentUserId = Long.valueOf(userDetails.getId());

        if(targetUserId.equals(currentUserId)) {
            throw new FollowException(GeneralErrorCode.INVALID_FORMAT, "스스로를 팔로잉 할 수 없습니다");
        }

        if(!memberRepository.existsById(targetUserId)) {
            throw new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND,"userId");
        }

        //팔로잉 신청한 사람
        Member followerUser = em.getReference(Member.class, currentUserId);
        //팔로우 신청 받은 사람
        Member followingUser = em.getReference(Member.class, targetUserId);
        
        if(followRepository.existsByFollowerUserAndFollowingUser(followerUser, followingUser)){
            throw new FollowException(FollowErrorCode.ALREADY_FOLLOWING);
        }

        //캐싱 시도
        try{
            followCacheService.incrementFollowingCount(currentUserId);
            followCacheService.incrementFollowerCount(targetUserId);
        } catch (CacheException e) {
            log.error(e.getMessage());
            followerUser.incrementFollowingCount();
            followingUser.incrementFollowerCount();
        }

        Follow follow = followConverter.toFollowEntity(followerUser, followingUser);
        followRepository.save(follow);
    }

    @Transactional
    public void removeFollowing(Long targetUserId, CustomUserDetails userDetails){

        Long currentUserId = Long.valueOf(userDetails.getId());

        if(targetUserId.equals(currentUserId)) {
            throw new FollowException(GeneralErrorCode.INVALID_FORMAT, "스스로를 언팔로잉 할 수 없습니다");
        }

        if(!memberRepository.existsById(targetUserId)) {
            throw new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND,"userId");
        }

        //팔로잉 신청한 사람
        Member followerUser = em.getReference(Member.class, currentUserId);
        //팔로우 신청 받은 사람
        Member followingUser = em.getReference(Member.class, targetUserId);

        Follow follow = followRepository.findByFollowerUserAndFollowingUser(followerUser, followingUser)
                .orElseThrow(()-> new FollowException(FollowErrorCode.ALREADY_UNFOLLOWING));

        //캐싱 시도
        try{
            followCacheService.decrementFollowingCount(currentUserId);
            followCacheService.decrementFollowerCount(targetUserId);
        } catch (CacheException e) {
            log.error(e.getMessage());
            followerUser.decrementFollowingCount();
            followingUser.decrementFollowerCount();
        }

        followRepository.delete(follow);
    }


    public List<MemberResponseDto.UserInfo> getFollowers(Long userId, Integer limit, Long cursor) {
        if(!memberRepository.existsById(userId)){
            throw new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId");
        }

        return followRepository.findFollowersByFollowingUserWithCursor(userId, limit, cursor);
    }

    public List<MemberResponseDto.UserInfo> getFollowings(Long userId, Integer limit, Long cursor) {
        if(!memberRepository.existsById(userId)){
            throw new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId");
        }

        return followRepository.findFollowingsByFollowerUserWithCursor(userId, limit, cursor);
    }
}
