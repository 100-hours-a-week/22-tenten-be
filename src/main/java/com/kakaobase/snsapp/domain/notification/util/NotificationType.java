package com.kakaobase.snsapp.domain.notification.util;

import lombok.Getter;

// Notification 종류 열거형
@Getter
public enum NotificationType {
    COMMENT_CREATED("POST", "comment.created"),
    RECOMMENT_CREATED("POST", "recomment.created"),
    FOLLOWING_CREATED("MEMEBER", "following.created"),
    POST_LIKE_CREATED("POST", "post.like.created"),
    COMMENT_LIKE_CREATED("POST", "comment.like.created"),
    RECOMMENT_LIKE_CREATED("POST", "recomment.like.created");

    private final String targetType;
    private final String event;

    NotificationType(String targetType, String event) {
        this.targetType = targetType;
        this.event = event;
    }
}