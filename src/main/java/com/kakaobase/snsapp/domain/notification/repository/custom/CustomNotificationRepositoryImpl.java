package com.kakaobase.snsapp.domain.notification.repository.custom;

import com.kakaobase.snsapp.domain.comments.entity.QComment;
import com.kakaobase.snsapp.domain.comments.entity.QCommentLike;
import com.kakaobase.snsapp.domain.comments.entity.QRecomment;
import com.kakaobase.snsapp.domain.comments.entity.QRecommentLike;
import com.kakaobase.snsapp.domain.follow.entity.QFollow;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.QMember;
import com.kakaobase.snsapp.domain.notification.converter.NotificationConverter;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationFollowingData;
import com.kakaobase.snsapp.domain.notification.entity.Notification;
import com.kakaobase.snsapp.domain.notification.entity.QNotification;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import com.kakaobase.snsapp.domain.posts.entity.QPostLike;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

    private final JPAQueryFactory queryFactory;
    private final NotificationConverter notificationConverter;

    @Override
    public List<WebSocketPacket<?>> findAllNotificationsByUserId(Long userId) {
        QNotification notification = QNotification.notification;

        // 알림 기본 조회
        List<Notification> notifications = queryFactory
                .selectFrom(notification)
                .where(notification.receiverId.eq(userId))
                .orderBy(notification.createdAt.desc())
                .fetch();

        // NotificationType별로 그룹화
        Map<NotificationType, List<Notification>> notificationsByType = notifications.stream()
                .collect(Collectors.groupingBy(Notification::getNotificationType));

        List<WebSocketPacket<?>> result = new ArrayList<>();
        
        // 각 타입별로 일괄 조회 및 처리
        for (Map.Entry<NotificationType, List<Notification>> entry : notificationsByType.entrySet()) {
            NotificationType type = entry.getKey();
            List<Notification> typeNotifications = entry.getValue();
            
            if (type == NotificationType.FOLLOWING_CREATED) {
                // 팔로우 알림 일괄 처리
                result.addAll(processFollowingNotifications(typeNotifications));
            } else {
                // 일반 알림 일괄 처리 (댓글, 좋아요 등)
                result.addAll(processGeneralNotifications(typeNotifications, type));
            }
        }
        
        // 원래 시간 순서대로 재정렬 (timestamp만 비교)
        return result.stream()
                .sorted((p1, p2) -> {
                    LocalDateTime time1 = getTimestamp(p1.data);
                    LocalDateTime time2 = getTimestamp(p2.data);
                    return time2.compareTo(time1); // 최신순
                })
                .toList();
    }

    /**
     * WebSocketPacket에서 timestamp 추출
     */
    private LocalDateTime getTimestamp(Object data) {
        if (data instanceof NotificationData notificationData) {
            return notificationData.timestamp();
        } else if (data instanceof NotificationFollowingData followingData) {
            return followingData.timestamp();
        }
        return LocalDateTime.MIN; // 예외 상황
    }

    /**
     * 팔로우 알림들을 일괄 처리
     */
    private List<WebSocketPacket<?>> processFollowingNotifications(List<Notification> notifications) {
        List<WebSocketPacket<?>> result = new ArrayList<>();
        
        if (notifications.isEmpty()) {
            return result;
        }
        
        // Follow ID 목록 추출
        List<Long> followIds = notifications.stream()
                .map(Notification::getTargetId)
                .toList();
        
        // 팔로우 정보 일괄 조회
        QMember followerMember = QMember.member;
        QFollow follow = QFollow.follow;
        
        Map<Long, MemberResponseDto.UserInfoWithFollowing> senderMap = queryFactory
                .select(com.querydsl.core.types.Projections.constructor(
                        MemberResponseDto.UserInfoWithFollowing.class,
                        followerMember.id,
                        followerMember.nickname,
                        followerMember.profileImgUrl,
                        com.querydsl.core.types.dsl.Expressions.constant(true)
                ))
                .from(follow)
                .leftJoin(follow.followerUser, followerMember)
                .where(follow.id.in(followIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        MemberResponseDto.UserInfoWithFollowing::id,
                        userInfo -> userInfo,
                        (existing, replacement) -> existing
                ));
        
        // 각 알림에 대해 패킷 생성
        for (Notification notification : notifications) {
            MemberResponseDto.UserInfoWithFollowing sender = senderMap.get(notification.getTargetId());
            
            WebSocketPacket<?> packet = notificationConverter.toPacket(
                    notification.getId(),
                    notification.getNotificationType(),
                    sender,
                    notification.getCreatedAt(),
                    notification.getIsRead()
            );
            result.add(packet);
        }
        
        return result;
    }

    /**
     * 일반 알림들을 일괄 처리 (댓글, 좋아요 등)
     */
    private List<WebSocketPacket<?>> processGeneralNotifications(List<Notification> notifications, NotificationType type) {
        List<WebSocketPacket<?>> result = new ArrayList<>();
        
        if (notifications.isEmpty()) {
            return result;
        }
        
        // 타입별 발신자 정보와 컨텐츠 일괄 조회
        Map<Long, MemberResponseDto.UserInfo> senderMap = getSenderInfoBatch(notifications, type);
        Map<Long, String> contentMap = getContentBatch(notifications, type);
        Map<Long, Long> postIdMap = getPostIdBatch(notifications, type);
        
        // 각 알림에 대해 패킷 생성
        for (Notification notification : notifications) {
            MemberResponseDto.UserInfo sender = senderMap.get(notification.getTargetId());
            String content = contentMap.get(notification.getTargetId());
            Long postId = postIdMap.get(notification.getId());
            
            WebSocketPacket<?> packet = notificationConverter.toPacket(
                    notification.getId(),
                    notification.getNotificationType(),
                    postId,
                    content,
                    sender,
                    notification.getCreatedAt(),
                    notification.getIsRead()
            );
            result.add(packet);
        }
        
        return result;
    }

    /**
     * 타입별 발신자 정보 일괄 조회
     */
    private Map<Long, MemberResponseDto.UserInfo> getSenderInfoBatch(List<Notification> notifications, NotificationType type) {
        List<Long> targetIds = notifications.stream()
                .map(Notification::getTargetId)
                .toList();
        
        QMember senderMember = QMember.member;
        QComment comment = QComment.comment;
        QRecomment recomment = QRecomment.recomment;
        QPostLike postLike = QPostLike.postLike;
        QCommentLike commentLike = QCommentLike.commentLike;
        QRecommentLike recommentLike = QRecommentLike.recommentLike;
        
        return switch (type) {
            case COMMENT_CREATED -> queryFactory
                    .select(com.querydsl.core.types.Projections.constructor(
                            MemberResponseDto.UserInfo.class,
                            senderMember.id,
                            senderMember.name,
                            senderMember.nickname,
                            senderMember.profileImgUrl
                    ))
                    .from(comment)
                    .leftJoin(comment.member, senderMember)
                    .where(comment.id.in(targetIds))
                    .fetch()
                    .stream()
                    .collect(Collectors.toMap(
                            MemberResponseDto.UserInfo::id,
                            userInfo -> userInfo,
                            (existing, replacement) -> existing
                    ));
                    
            case RECOMMENT_CREATED -> queryFactory
                    .select(com.querydsl.core.types.Projections.constructor(
                            MemberResponseDto.UserInfo.class,
                            senderMember.id,
                            senderMember.name,
                            senderMember.nickname,
                            senderMember.profileImgUrl
                    ))
                    .from(recomment)
                    .leftJoin(recomment.member, senderMember)
                    .where(recomment.id.in(targetIds))
                    .fetch()
                    .stream()
                    .collect(Collectors.toMap(
                            MemberResponseDto.UserInfo::id,
                            userInfo -> userInfo,
                            (existing, replacement) -> existing
                    ));
                    
            case POST_LIKE_CREATED -> queryFactory
                    .select(com.querydsl.core.types.Projections.constructor(
                            MemberResponseDto.UserInfo.class,
                            senderMember.id,
                            senderMember.name,
                            senderMember.nickname,
                            senderMember.profileImgUrl
                    ))
                    .from(postLike)
                    .leftJoin(postLike.member, senderMember)
                    .where(postLike.id.postId.in(targetIds))
                    .fetch()
                    .stream()
                    .collect(Collectors.toMap(
                            MemberResponseDto.UserInfo::id,
                            userInfo -> userInfo,
                            (existing, replacement) -> existing
                    ));
                    
            case COMMENT_LIKE_CREATED -> queryFactory
                    .select(com.querydsl.core.types.Projections.constructor(
                            MemberResponseDto.UserInfo.class,
                            senderMember.id,
                            senderMember.name,
                            senderMember.nickname,
                            senderMember.profileImgUrl
                    ))
                    .from(commentLike)
                    .leftJoin(commentLike.member, senderMember)
                    .where(commentLike.comment.id.in(targetIds))
                    .fetch()
                    .stream()
                    .collect(Collectors.toMap(
                            MemberResponseDto.UserInfo::id,
                            userInfo -> userInfo,
                            (existing, replacement) -> existing
                    ));
                    
            case RECOMMENT_LIKE_CREATED -> queryFactory
                    .select(com.querydsl.core.types.Projections.constructor(
                            MemberResponseDto.UserInfo.class,
                            senderMember.id,
                            senderMember.name,
                            senderMember.nickname,
                            senderMember.profileImgUrl
                    ))
                    .from(recommentLike)
                    .leftJoin(recommentLike.member, senderMember)
                    .where(recommentLike.recomment.id.in(targetIds))
                    .fetch()
                    .stream()
                    .collect(Collectors.toMap(
                            MemberResponseDto.UserInfo::id,
                            userInfo -> userInfo,
                            (existing, replacement) -> existing
                    ));
                    
            default -> Map.of();
        };
    }

    /**
     * 타입별 컨텐츠 일괄 조회
     */
    private Map<Long, String> getContentBatch(List<Notification> notifications, NotificationType type) {
        List<Long> targetIds = notifications.stream()
                .map(Notification::getTargetId)
                .toList();
        
        QComment comment = QComment.comment;
        QRecomment recomment = QRecomment.recomment;
        
        return switch (type) {
            case COMMENT_CREATED -> {
                List<com.querydsl.core.Tuple> results = queryFactory
                        .select(comment.id, comment.content)
                        .from(comment)
                        .where(comment.id.in(targetIds))
                        .fetch();
                yield results.stream()
                        .collect(Collectors.toMap(
                                tuple -> tuple.get(comment.id),
                                tuple -> tuple.get(comment.content)
                        ));
            }
            
            case RECOMMENT_CREATED -> {
                List<com.querydsl.core.Tuple> results = queryFactory
                        .select(recomment.id, recomment.content)
                        .from(recomment)
                        .where(recomment.id.in(targetIds))
                        .fetch();
                yield results.stream()
                        .collect(Collectors.toMap(
                                tuple -> tuple.get(recomment.id),
                                tuple -> tuple.get(recomment.content)
                        ));
            }
            
            default -> notifications.stream()
                    .collect(Collectors.toMap(
                            Notification::getTargetId,
                            n -> null
                    ));
        };
    }

    /**
     * 타입별 PostId 일괄 조회 (notificationId -> postId 매핑)
     */
    private Map<Long, Long> getPostIdBatch(List<Notification> notifications, NotificationType type) {
        QComment comment = QComment.comment;
        QRecomment recomment = QRecomment.recomment;
        
        // targetId로 PostId를 조회한 후, notificationId와 매핑
        Map<Long, Long> targetToPostMap = switch (type) {
            case COMMENT_CREATED -> {
                List<Long> targetIds = notifications.stream()
                        .map(Notification::getTargetId)
                        .toList();
                List<com.querydsl.core.Tuple> results = queryFactory
                        .select(comment.id, comment.post.id)
                        .from(comment)
                        .where(comment.id.in(targetIds))
                        .fetch();
                yield results.stream()
                        .collect(Collectors.toMap(
                                tuple -> tuple.get(comment.id),
                                tuple -> tuple.get(comment.post.id)
                        ));
            }

            case RECOMMENT_CREATED -> {
                List<Long> targetIds = notifications.stream()
                        .map(Notification::getTargetId)
                        .toList();
                List<com.querydsl.core.Tuple> results = queryFactory
                        .select(recomment.id, comment.post.id)
                        .from(recomment)
                        .join(recomment.comment, comment)
                        .where(recomment.id.in(targetIds))
                        .fetch();
                yield results.stream()
                        .collect(Collectors.toMap(
                                tuple -> tuple.get(recomment.id),
                                tuple -> tuple.get(comment.post.id)
                        ));
            }

            case COMMENT_LIKE_CREATED -> {
                List<Long> targetIds = notifications.stream()
                        .map(Notification::getTargetId)
                        .toList();
                QCommentLike commentLike = QCommentLike.commentLike;
                List<com.querydsl.core.Tuple> results = queryFactory
                        .select(commentLike.comment.id, comment.post.id)
                        .from(commentLike)
                        .join(commentLike.comment, comment)
                        .where(commentLike.comment.id.in(targetIds))
                        .fetch();
                yield results.stream()
                        .collect(Collectors.toMap(
                                tuple -> tuple.get(commentLike.comment.id),
                                tuple -> tuple.get(comment.post.id)
                        ));
            }

            case RECOMMENT_LIKE_CREATED -> {
                List<Long> targetIds = notifications.stream()
                        .map(Notification::getTargetId)
                        .toList();
                QRecommentLike recommentLike = QRecommentLike.recommentLike;
                List<com.querydsl.core.Tuple> results = queryFactory
                        .select(recommentLike.recomment.id, comment.post.id)
                        .from(recommentLike)
                        .join(recommentLike.recomment, recomment)
                        .join(recomment.comment, comment)
                        .where(recommentLike.recomment.id.in(targetIds))
                        .fetch();
                yield results.stream()
                        .collect(Collectors.toMap(
                                tuple -> tuple.get(recommentLike.recomment.id),
                                tuple -> tuple.get(comment.post.id)
                        ));
            }

            case POST_LIKE_CREATED -> notifications.stream()
                    .collect(Collectors.toMap(
                            Notification::getTargetId,
                            Notification::getTargetId
                    ));

            default -> notifications.stream()
                    .collect(Collectors.toMap(
                            Notification::getTargetId,
                            Notification::getTargetId
                    ));
        };
        
        // notificationId -> postId로 변환
        return notifications.stream()
                .collect(Collectors.toMap(
                        Notification::getId,
                        notification -> targetToPostMap.get(notification.getTargetId())
                ));
    }
}