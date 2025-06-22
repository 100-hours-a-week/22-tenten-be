package com.kakaobase.snsapp.domain.follow.repository.custom;

import com.kakaobase.snsapp.domain.follow.dto.FollowCount;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;

import java.util.List;
import java.util.Set;

public interface FollowCustomRepository {

    /**
     * 특정 사용자가 팔로우하고 있는 사용자 ID 목록을 일괄 조회
     */
    Set<Long> findFollowingMemberIdsByFollowerAndTargets(Long followerId, List<Long> followingIds);

    /**
     * 팔로워 목록을 커서 기반 페이징으로 조회
     */
    List<MemberResponseDto.UserInfo> findFollowersByFollowingUserWithCursor(
            Long followingId, Integer limit, Long cursor);

    /**
     * 팔로잉 목록을 커서 기반 페이징으로 조회
     */
    List<MemberResponseDto.UserInfo> findFollowingsByFollowerUserWithCursor(
            Long followerId, Integer limit, Long cursor);

    /**
     * 특정 사용자가 팔로우하는 모든 사용자 ID 조회
     */
    Set<Long> findFollowingUserIdsByFollowerUserId(Long followerUserId);

    /**
     * 모든 사용자의 팔로잉 수 조회
     */
    List<FollowCount> findFollowingCounts();

    /**
     * 모든 사용자의 팔로워 수 조회
     */
    List<FollowCount> findFollowerCounts();

    /**
     * 특정 사용자의 팔로워 수 조회
     */
    Long countFollowersByFollowingUserId(Long followingUserId);

    /**
     * 특정 사용자의 팔로잉 수 조회
     */
    Long countFollowingsByFollowerUserId(Long followerUserId);
}