package com.kakaobase.snsapp.global.common.redis;

import lombok.Builder;

public class CacheRecord {

    /**
     * AccessToken재발급용 사용자 정보 DTO
     */
    @Builder
    public record UserAuthCache(
            Long memberId,
            String role,
            String className,
            String nickname,
            String imageUrl,
            boolean isEnabled
    ) {}

    @Builder
    public record PostStatsCache(
            Long postId,
            Long likeCount,
            Long commentCount
    ) {}

    @Builder
    public record CommentStatsCache(
            Long commentId,
            Long likeCount,
            Long recommentCount
    ) {}

    @Builder
    public record FollowStatsCache(
            Long memberId,
            Long followerCount,
            Long followingCount
    ) {}
}
