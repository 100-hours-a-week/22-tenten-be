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

    /**
     * 특정 Member를 follower로 하는 모든 Follow 데이터 삭제
     * (해당 Member가 다른 사람들을 팔로우한 관계들을 모두 삭제)
     */
    Long deleteByFollowerUserId(Long followerUserId);

    /**
     * 특정 Member를 following으로 하는 모든 Follow 데이터 삭제
     * (해당 Member를 팔로우하는 관계들을 모두 삭제)
     */
    Long deleteByFollowingUserId(Long followingUserId);

    /**
     * 특정 유저를 팔로우한 모든 Member의 followingCount를 1씩 감소
     * (특정 유저가 탈퇴할 때, 그 유저를 팔로우했던 사람들의 팔로잉 카운트 감소)
     */
    Long decrementFollowingCountForFollowersOf(Long followingUserId);

    /**
     * 특정 유저가 팔로잉한 모든 Member의 followerCount를 1씩 감소
     * (특정 유저가 탈퇴할 때, 그 유저가 팔로우했던 사람들의 팔로워 카운트 감소)
     */
    Long decrementFollowerCountForFollowingsOf(Long followerUserId);

    /**
     * 회원 탈퇴 시 모든 팔로우 관계 정리 (4개 작업을 순차 실행)
     * 1. 해당 유저를 팔로우한 사람들의 팔로잉 카운트 감소
     * 2. 해당 유저가 팔로우한 사람들의 팔로워 카운트 감소
     * 3. 해당 유저의 팔로잉 관계 삭제
     * 4. 해당 유저의 팔로워 관계 삭제
     */
    void cleanupAllFollowRelationships(Long memberId);
}