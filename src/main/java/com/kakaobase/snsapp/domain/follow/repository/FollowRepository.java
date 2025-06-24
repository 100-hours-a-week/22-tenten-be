package com.kakaobase.snsapp.domain.follow.repository;

import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.follow.repository.custom.FollowCustomRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long>, FollowCustomRepository {


    /**
     * 팔로우 관계가 존재하는지 확인
     */
    boolean existsByFollowerUserAndFollowingUser(Member followerUser, Member followingUser);

    /**
     * 특정 팔로우 관계 조회
     */
    Optional<Follow> findByFollowerUserAndFollowingUser(Member followerUser, Member followingUser);

    /**
     * 특정 Member가 다른 사람들을 팔로우한 관계들을 모두 삭제
     */
    Long deleteByFollowerUserId(Long followerUserId);

    /**
     * 특정 Member를 팔로우하는 관계들을 모두 삭제)
     */
    Long deleteByFollowingUserId(Long followingUserId);
}