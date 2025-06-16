package com.kakaobase.snsapp.domain.follow.converter;

import com.kakaobase.snsapp.domain.follow.dto.FollowResponse;
import com.kakaobase.snsapp.domain.follow.entity.Follow;
import com.kakaobase.snsapp.domain.members.entity.Member;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FollowConverter {


    public Follow toFollowEntity(Member follower, Member following){
        return Follow.builder()
                .followerUser(follower)
                .followingUser(following)
                .build();
    }

    public List<FollowResponse.UserInfo> toUserInfoList(List<Object[]> rawDataList) {
        return rawDataList.stream()
                .map(row -> new FollowResponse.UserInfo(
                        ((Number) row[0]).longValue(), // id
                        (String) row[1],               // nickname
                        (String) row[2],               // name
                        (String) row[3]                // profile_image
                ))
                .collect(Collectors.toList());
    }
}
