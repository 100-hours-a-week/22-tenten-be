package com.kakaobase.snsapp.domain.follow.service;


import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.follow.converter.FollowConverter;
import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.follow.exception.FollowErrorCode;
import com.kakaobase.snsapp.domain.follow.exception.FollowException;
import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.converter.MemberConverter;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import com.kakaobase.snsapp.domain.notification.service.NotificationService;
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
    private final NotificationService notifService;
    private final MemberConverter memberConverter;


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
        Follow savedFollow = followRepository.save(follow);

        // 팔로우 알림 전송 - 팔로우를 받은 사용자에게 팔로우한 사용자의 정보와 함께 알림 전송
        // followingUser가 followerUser를 팔로우하고 있는지 확인 (상호 팔로우 여부)
        boolean isFollowingBack = followRepository.existsByFollowerUserAndFollowingUser(followingUser, followerUser);
        MemberResponseDto.UserInfoWithFollowing userInfoWithFollowing = memberConverter.toUserInfoWithFollowing(followerUser, isFollowingBack);
        notifService.sendFollowingCreatedNotification(
                targetUserId, 
                savedFollow.getId(), 
                userInfoWithFollowing
        );
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
