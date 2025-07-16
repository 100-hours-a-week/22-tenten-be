package com.kakaobase.snsapp.domain.notification.repository.custom;

import com.kakaobase.snsapp.domain.comments.entity.QComment;
import com.kakaobase.snsapp.domain.comments.entity.QRecomment;
import com.kakaobase.snsapp.domain.follow.entity.QFollow;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.QMember;
import com.kakaobase.snsapp.domain.notification.converter.NotificationConverter;
import com.kakaobase.snsapp.domain.notification.dto.records.ContentNotification;
import com.kakaobase.snsapp.domain.notification.dto.records.FollowingNotificationData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationResponse;
import com.kakaobase.snsapp.domain.notification.entity.Notification;
import com.kakaobase.snsapp.domain.notification.entity.QNotification;
import com.kakaobase.snsapp.domain.notification.util.InvalidNotificationCacheUtil;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
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
import com.querydsl.core.BooleanBuilder;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

    private final JPAQueryFactory queryFactory;
    private final NotificationConverter notificationConverter;
    private final InvalidNotificationCacheUtil invalidNotificationCacheUtil;

    @Override
    public List<WebSocketPacket<NotificationResponse>> findAllNotificationsByUserId(Long userId) {
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

        List<WebSocketPacket<NotificationResponse>> result = new ArrayList<>();
        
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
        List<WebSocketPacket<NotificationResponse>> sortedResult = result.stream()
                .sorted((p1, p2) -> {
                    LocalDateTime time1 = getTimestamp(p1.data);
                    LocalDateTime time2 = getTimestamp(p2.data);
                    return time2.compareTo(time1); // 최신순
                })
                .toList();
        
        // WebSocketPacket 그대로 반환
        return sortedResult;
    }

    @Override
    public List<WebSocketPacket<NotificationResponse>> findNotificationsWithCursor(Long userId, Integer limit, Long cursor) {
        QNotification notification = QNotification.notification;
        BooleanBuilder whereClause = new BooleanBuilder();

        // 기본 조건: 해당 사용자의 알림
        whereClause.and(notification.receiverId.eq(userId));

        // cursor 조건: cursor보다 작은 ID (오래된 알림)
        if (cursor != null) {
            whereClause.and(notification.id.lt(cursor));
        }

        // 알림 기본 조회 (cursor 기반 페이지네이션)
        List<Notification> notifications = queryFactory
                .selectFrom(notification)
                .where(whereClause)
                .orderBy(notification.createdAt.desc())
                .limit(limit)
                .fetch();

        // NotificationType별로 그룹화
        Map<NotificationType, List<Notification>> notificationsByType = notifications.stream()
                .collect(Collectors.groupingBy(Notification::getNotificationType));

        List<WebSocketPacket<NotificationResponse>> result = new ArrayList<>();

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
        List<WebSocketPacket<NotificationResponse>> sortedResult = result.stream()
                .sorted((p1, p2) -> {
                    LocalDateTime time1 = getTimestamp(p1.data);
                    LocalDateTime time2 = getTimestamp(p2.data);
                    return time2.compareTo(time1); // 최신순
                })
                .toList();

        // WebSocketPacket 그대로 반환
        return sortedResult;
    }

    /**
     * WebSocketPacket에서 timestamp 추출
     */
    private LocalDateTime getTimestamp(Object data) {
        if (data instanceof ContentNotification contentNotification) {
            return contentNotification.timestamp();
        } else if (data instanceof FollowingNotificationData followingData) {
            return followingData.timestamp();
        }
        return LocalDateTime.MIN; // 예외 상황
    }

    /**
     * 팔로우 알림들을 일괄 처리
     */
    private List<WebSocketPacket<NotificationResponse>> processFollowingNotifications(List<Notification> notifications) {
        List<WebSocketPacket<NotificationResponse>> result = new ArrayList<>();
        
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

        // 실제 팔로우 알림 패킷 생성
        for (Notification notification : notifications) {
            MemberResponseDto.UserInfoWithFollowing sender = senderMap.get(notification.getSenderId());
            if (sender != null) {
                WebSocketPacket<NotificationResponse> packet = notificationConverter.toFollowingResponsePacket(
                        notification.getId(),
                        notification.getNotificationType(),
                        sender,
                        notification.getCreatedAt(),
                        notification.getIsRead()
                );
                result.add(packet);
            } else {
                // 무효한 팔로우 알림은 Redis 캐시에 추가
                invalidNotificationCacheUtil.addInvalidNotificationId(notification.getId());
            }
        }

        return result;
    }

    /**
     * 일반 알림들을 일괄 처리 (댓글, 좋아요 등)
     */
    private List<WebSocketPacket<NotificationResponse>> processGeneralNotifications(List<Notification> notifications, NotificationType type) {
        return processNotifications(notifications, type);
    }
    
    /**
     * 알림을 처리하여 유효한 알림만 WebSocketPacket으로 변환
     * 무효한 알림은 즉시 필터링하고 스케줄러가 DB에서 정리하도록 함
     */
    private List<WebSocketPacket<NotificationResponse>> processNotifications(List<Notification> notifications, NotificationType type) {
        List<WebSocketPacket<NotificationResponse>> result = new ArrayList<>();
        
        if (notifications.isEmpty()) {
            return result;
        }
        
        // 타입별 발신자 정보와 컨텐츠 일괄 조회
        Map<Long, MemberResponseDto.UserInfo> senderMap = getSenderInfoBySenderId(notifications);
        Map<Long, String> contentMap = getContentBatch(notifications, type);
        Map<Long, Long> postIdMap = getPostIdBatch(notifications, type);
        
        // 각 알림에 대해 패킷 생성 (유효한 알림만 처리)
        for (Notification notification : notifications) {
            MemberResponseDto.UserInfo sender = senderMap.get(notification.getSenderId());
            String content = contentMap.get(notification.getId());
            Long postId = postIdMap.get(notification.getId());
            
            // 무효한 알림은 Redis 캐시에 추가하고 즉시 필터링 (스케줄러가 DB에서 정리)
            if (sender == null || postId == null || postId.equals(-1L)) {
                invalidNotificationCacheUtil.addInvalidNotificationId(notification.getId());
                continue;
            }
            
            WebSocketPacket<NotificationResponse> packet = notificationConverter.toNotificationResponsePacket(
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
     * senderId를 기반으로 발신자 정보 일괄 조회
     */
    private Map<Long, MemberResponseDto.UserInfo> getSenderInfoBySenderId(List<Notification> notifications) {
        List<Long> senderIds = notifications.stream()
                .map(Notification::getSenderId)
                .distinct()
                .toList();

        QMember member = QMember.member;
        
        List<com.querydsl.core.Tuple> results = queryFactory
                .select(member.id, member.name, member.nickname, member.profileImgUrl)
                .from(member)
                .where(member.id.in(senderIds))
                .fetch();
        
        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(member.id),
                        tuple -> new MemberResponseDto.UserInfo(
                                tuple.get(member.id),
                                tuple.get(member.name),
                                tuple.get(member.nickname),
                                tuple.get(member.profileImgUrl)
                        )
                ));
    }

    /**
     * 타입별 컨텐츠 일괄 조회 (notificationId -> content 매핑)
     */
    private Map<Long, String> getContentBatch(List<Notification> notifications, NotificationType type) {
        List<Long> targetIds = notifications.stream()
                .map(Notification::getTargetId)
                .toList();
        
        QComment comment = QComment.comment;
        QRecomment recomment = QRecomment.recomment;
        
        // 먼저 targetId -> content 매핑을 구한 후, notificationId -> content로 변환
        Map<Long, String> targetToContentMap = switch (type) {
            case COMMENT_CREATED -> {
                List<com.querydsl.core.Tuple> results = queryFactory
                        .select(comment.id, comment.content)
                        .from(comment)
                        .where(comment.id.in(targetIds))
                        .fetch();
                yield results.stream()
                        .collect(Collectors.toMap(
                                tuple -> tuple.get(comment.id),
                                tuple -> tuple.get(comment.content),
                                (existing, replacement) -> existing
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
                                tuple -> tuple.get(recomment.content),
                                (existing, replacement) -> existing
                        ));
            }
            
            // 좋아요 알림들은 content가 필요 없음 (빈 문자열로 처리)
            default -> notifications.stream()
                    .collect(Collectors.toMap(
                            Notification::getTargetId,
                            n -> "",  // 좋아요 알림은 content가 없음
                            (existing, replacement) -> existing
                    ));
        };
        
        // notificationId -> content로 변환
        return notifications.stream()
                .collect(Collectors.toMap(
                        Notification::getId,
                        notification -> targetToContentMap.getOrDefault(notification.getTargetId(), ""),
                        (existing, replacement) -> existing
                ));
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
                                tuple -> tuple.get(comment.post.id),
                                (existing, replacement) -> existing
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
                                tuple -> tuple.get(comment.post.id),
                                (existing, replacement) -> existing
                        ));
            }

            case COMMENT_LIKE_CREATED -> {
                List<Long> commentIds = notifications.stream()
                        .map(Notification::getTargetId)
                        .toList();
                List<com.querydsl.core.Tuple> results = queryFactory
                        .select(comment.id, comment.post.id)
                        .from(comment)
                        .where(comment.id.in(commentIds))
                        .fetch();
                yield results.stream()
                        .collect(Collectors.toMap(
                                tuple -> tuple.get(comment.id),
                                tuple -> tuple.get(comment.post.id),
                                (existing, replacement) -> existing
                        ));
            }

            case RECOMMENT_LIKE_CREATED -> {
                List<Long> recommentIds = notifications.stream()
                        .map(Notification::getTargetId)
                        .toList();
                List<com.querydsl.core.Tuple> results = queryFactory
                        .select(recomment.id, comment.post.id)
                        .from(recomment)
                        .join(recomment.comment, comment)
                        .where(recomment.id.in(recommentIds))
                        .fetch();
                yield results.stream()
                        .collect(Collectors.toMap(
                                tuple -> tuple.get(recomment.id),
                                tuple -> tuple.get(comment.post.id),
                                (existing, replacement) -> existing
                        ));
            }

            case POST_LIKE_CREATED -> notifications.stream()
                    .collect(Collectors.toMap(
                            Notification::getTargetId,
                            Notification::getTargetId,
                            (existing, replacement) -> existing
                    ));

            default -> notifications.stream()
                    .collect(Collectors.toMap(
                            Notification::getTargetId,
                            Notification::getTargetId,
                            (existing, replacement) -> existing
                    ));
        };
        
        // notificationId -> postId로 변환
        return notifications.stream()
                .collect(Collectors.toMap(
                        Notification::getId,
                        notification -> {
                            Long postId = targetToPostMap.get(notification.getTargetId());
                            if (postId != null) {
                                return postId;
                            }
                            // POST_LIKE_CREATED의 경우 targetId가 이미 postId
                            if (type == NotificationType.POST_LIKE_CREATED) {
                                return notification.getTargetId();
                            }
                            // 삭제된 엔티티의 경우 -1L 반환 (null 대신)
                            return -1L;
                        },
                        (existing, replacement) -> existing
                ));
    }
}