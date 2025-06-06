package com.kakaobase.snsapp.domain.follow.service;


import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.follow.converter.FollowConverter;
import com.kakaobase.snsapp.domain.follow.dto.FollowResponse;
import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.follow.exception.FollowErrorCode;
import com.kakaobase.snsapp.domain.follow.exception.FollowException;
import com.kakaobase.snsapp.domain.follow.repository.FollowRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
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
    private final EntityManager entityManager;


    @Transactional
    public void addFollowing(Long targetUserId, CustomUserDetails userDetails) {

        Long currentUserId = Long.valueOf(userDetails.getId());

        if(targetUserId.equals(currentUserId)) {
            throw new FollowException(GeneralErrorCode.INVALID_FORMAT, "스스로를 팔로잉 할 수 없습니다");
        }

        //팔로잉 신청한 사람
        Member followerUser = memberRepository.findById(currentUserId)
                .orElseThrow(()-> new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));

        //팔로잉 요청 받은 사람
        Member followingUser = memberRepository.findById(targetUserId)
                .orElseThrow(()-> new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "targetUserId"));

        if(followRepository.existsByFollowerUserAndFollowingUser(followerUser, followingUser)){
            throw new FollowException(FollowErrorCode.ALREADY_FOLLOWING);
        }

        followerUser.incrementFollowingCount();
        followingUser.incrementFollowerCount();

        Follow follow = followConverter.toFollowEntity(followerUser, followingUser);
        followRepository.save(follow);
    }

    @Transactional
    public void removeFollowing(Long targetUserId, CustomUserDetails userDetails){

        //팔로잉 신청한 사람
        Member followerUser = memberRepository.findById(Long.valueOf(userDetails.getId()))
                .orElseThrow(()-> new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId"));
        //팔로잉 요청 받은 사람
        Member followingUser = memberRepository.findById(targetUserId)
                .orElseThrow(()-> new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "targetUserId"));

        Follow follow = followRepository.findByFollowerUserAndFollowingUser(followerUser, followingUser)
                .orElseThrow(()-> new FollowException(FollowErrorCode.ALREADY_UNFOLLOWING));

        followerUser.decrementFollowingCount();
        followingUser.decrementFollowerCount();

        followRepository.delete(follow);
    }


    public List<FollowResponse.UserInfo> getFollowers(Long userId, Integer limit, Long cursor) {
        if(!memberRepository.existsById(userId)){
            throw new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId");
        }

        return followConverter.toUserInfoList(followRepository.findFollowersByFollowingUserWithCursor(userId, limit, cursor));
    }

    public List<FollowResponse.UserInfo> getFollowings(Long userId, Integer limit, Long cursor) {
        if(!memberRepository.existsById(userId)){
            throw new FollowException(GeneralErrorCode.RESOURCE_NOT_FOUND, "userId");
        }

        return followConverter.toUserInfoList(followRepository.findFollowingsByFollowerUserWithCursor(userId, limit, cursor));
    }
}
