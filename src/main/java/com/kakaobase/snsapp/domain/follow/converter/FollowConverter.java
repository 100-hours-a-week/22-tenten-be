package com.kakaobase.snsapp.domain.follow.converter;

import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.members.entity.Member;
import org.springframework.stereotype.Component;

@Component
public class FollowConverter {


    public Follow toFollowEntity(Member follower, Member following){
        return Follow.builder()
                .followerUser(follower)
                .followingUser(following)
                .build();
    }
}
