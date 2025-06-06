package com.kakaobase.snsapp.domain.follow.entity;


import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.global.common.entity.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //팔로잉 요청건 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_user_id", nullable = false)
    private Member followerUser;

    //팔로잉 요청 받은 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private Member followingUser;

    @Builder
    public Follow(Long id, Member followerUser, Member followingUser) {
        this.followerUser = followerUser;
        this.followingUser = followingUser;
    }

}
