package com.kakaobase.snsapp.domain.notification.util;

import lombok.Getter;

// Notification 종류 열거형
@Getter
public enum NotificationType {
    NOTIFICATION_FETCH("NOTIFICATION", "notification.fetch"),
    COMMENT_CREATED("COMMENT", "comment.created"),
    RECOMMENT_CREATED("RECOMMENT", "recomment.created"),
    FOLLOWING_CREATED("MEMBER", "following.created"),
    POST_LIKE_CREATED("POST", "post.like.created"),
    COMMENT_LIKE_CREATED("COMMENT", "comment.like.created"),
    RECOMMENT_LIKE_CREATED("RECOMMENT", "recomment.like.created");

    private final String targetType;
    private final String event;

    NotificationType(String targetType, String event) {
        this.targetType = targetType;
        this.event = event;
    }
}