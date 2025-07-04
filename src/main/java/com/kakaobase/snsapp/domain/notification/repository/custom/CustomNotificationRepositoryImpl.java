package com.kakaobase.snsapp.domain.notification.repository.custom;

import com.kakaobase.snsapp.domain.comments.entity.QComment;
import com.kakaobase.snsapp.domain.comments.entity.QCommentLike;
import com.kakaobase.snsapp.domain.comments.entity.QRecomment;
import com.kakaobase.snsapp.domain.comments.entity.QRecommentLike;
import com.kakaobase.snsapp.domain.follow.entity.QFollow;
import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.members.entity.QMember;
import com.kakaobase.snsapp.domain.notification.converter.NotificationConverter;
import com.kakaobase.snsapp.domain.notification.entity.Notification;
import com.kakaobase.snsapp.domain.notification.entity.QNotification;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import com.kakaobase.snsapp.domain.posts.entity.QPostLike;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

    private static final String UNKNOWN_USER = "알 수 없는 사용자";

    private final JPAQueryFactory queryFactory;
    private final NotificationConverter notificationConverter;

    @Override
    @Transactional
    public List<WebSocketPacket<?>> findAllNotificationsByUserId(Long userId) {
        QNotification notification = QNotification.notification;

        // 알림 기본 조회
        List<Notification> notifications = queryFactory
                .selectFrom(notification)
                .where(notification.receiverId.eq(userId))
                .orderBy(notification.createdAt.desc())
                .fetch();

        List<WebSocketPacket<?>> result = new ArrayList<>();
        List<Long> invalidNotificationIds = new ArrayList<>();
        
        for (Notification notif : notifications) {
            // 알림이 유효한지 검증
            if (!isNotificationValid(notif)) {
                invalidNotificationIds.add(notif.getId());
                log.info("무효한 알림 감지 - ID: {}, 타입: {}, 타겟ID: {}", 
                        notif.getId(), notif.getNotificationType(), notif.getTargetId());
                continue; // 무효한 알림은 결과에 포함하지 않음
            }
            
            String content = getContentByNotification(notif);
            
            if (notif.getNotificationType() == NotificationType.FOLLOWING_CREATED) {
                // 팔로우 알림 처리
                MemberResponseDto.UserInfoWithFollowing sender = getSenderInfoWithFollowing(notif);
                if (sender != null && sender.id() != 0L) { // 유효한 sender인 경우만
                    WebSocketPacket<?> packet = notificationConverter.toNewPacket(
                            notif.getId(),
                            notif.getNotificationType(),
                            sender,
                            notif.getCreatedAt(),
                            notif.getIsRead()
                    );
                    result.add(packet);
                } else {
                    invalidNotificationIds.add(notif.getId());
                }
            } else {
                // 일반 알림 처리 (댓글, 좋아요 등) - target_id는 PostId로 변환
                MemberResponseDto.UserInfo sender = getSenderInfo(notif);
                if (sender != null && sender.id() != 0L) { // 유효한 sender인 경우만
                    Long postId = getPostIdFromNotification(notif);
                    WebSocketPacket<?> packet = notificationConverter.toNewPacket(
                            notif.getId(),
                            notif.getNotificationType(),
                            postId,
                            content,
                            sender,
                            notif.getCreatedAt(),
                            notif.getIsRead()
                    );
                    result.add(packet);
                } else {
                    invalidNotificationIds.add(notif.getId());
                }
            }
        }
        
        // 무효한 알림들 일괄 삭제
        if (!invalidNotificationIds.isEmpty()) {
            deleteInvalidNotifications(invalidNotificationIds);
            log.info("무효한 알림 {}개 삭제 완료", invalidNotificationIds.size());
        }
        
        return result;
    }

    /**
     * 알림 타입별 실제 엔티티의 content 조회
     */
    private String getContentByNotification(Notification notification) {
        QComment comment = QComment.comment;
        QRecomment recomment = QRecomment.recomment;
        
        return switch (notification.getNotificationType()) {
            case COMMENT_CREATED -> queryFactory
                    .select(comment.content)
                    .from(comment)
                    .where(comment.id.eq(notification.getTargetId()))
                    .fetchOne();
                    
            case RECOMMENT_CREATED -> queryFactory
                    .select(recomment.content)
                    .from(recomment)
                    .where(recomment.id.eq(notification.getTargetId()))
                    .fetchOne();
                    
            case POST_LIKE_CREATED -> null;  // 좋아요 알림은 content null
            case COMMENT_LIKE_CREATED -> null;  // 댓글 좋아요 알림은 content null
            case RECOMMENT_LIKE_CREATED -> null;  // 답글 좋아요 알림은 content null
            case FOLLOWING_CREATED -> null;  // 팔로우 알림은 content null
            default -> null;
        };
    }

    /**
     * 알림 발신자 정보 조회 - NotificationType에 따라 실제 발신자 조회
     */
    private MemberResponseDto.UserInfo getSenderInfo(Notification notification) {
        QMember senderMember = QMember.member;
        QComment comment = QComment.comment;
        QRecomment recomment = QRecomment.recomment;
        QPostLike postLike = QPostLike.postLike;
        QCommentLike commentLike = QCommentLike.commentLike;
        QRecommentLike recommentLike = QRecommentLike.recommentLike;
        
        MemberResponseDto.UserInfo result = switch (notification.getNotificationType()) {
            case COMMENT_CREATED -> queryFactory
                    .select(com.querydsl.core.types.Projections.constructor(
                            MemberResponseDto.UserInfo.class,
                            senderMember.id,
                            senderMember.name,        // 실제 이름
                            senderMember.nickname,    // 닉네임
                            senderMember.profileImgUrl // 프로필 이미지
                    ))
                    .from(comment)
                    .join(comment.member, senderMember)
                    .where(comment.id.eq(notification.getTargetId()))
                    .fetchOne();
                    
            case RECOMMENT_CREATED -> queryFactory
                    .select(com.querydsl.core.types.Projections.constructor(
                            MemberResponseDto.UserInfo.class,
                            senderMember.id,
                            senderMember.name,
                            senderMember.nickname,
                            senderMember.profileImgUrl
                    ))
                    .from(recomment)
                    .join(recomment.member, senderMember)
                    .where(recomment.id.eq(notification.getTargetId()))
                    .fetchOne();
                    
            case POST_LIKE_CREATED -> queryFactory
                    .select(com.querydsl.core.types.Projections.constructor(
                            MemberResponseDto.UserInfo.class,
                            senderMember.id,
                            senderMember.name,
                            senderMember.nickname,
                            senderMember.profileImgUrl
                    ))
                    .from(postLike)
                    .join(postLike.member, senderMember)
                    .where(postLike.id.postId.eq(notification.getTargetId()))
                    .fetchOne();
                    
            case COMMENT_LIKE_CREATED -> queryFactory
                    .select(com.querydsl.core.types.Projections.constructor(
                            MemberResponseDto.UserInfo.class,
                            senderMember.id,
                            senderMember.name,
                            senderMember.nickname,
                            senderMember.profileImgUrl
                    ))
                    .from(commentLike)
                    .join(commentLike.member, senderMember)
                    .where(commentLike.comment.id.eq(notification.getTargetId()))
                    .fetchOne();
                    
            case RECOMMENT_LIKE_CREATED -> queryFactory
                    .select(com.querydsl.core.types.Projections.constructor(
                            MemberResponseDto.UserInfo.class,
                            senderMember.id,
                            senderMember.name,
                            senderMember.nickname,
                            senderMember.profileImgUrl
                    ))
                    .from(recommentLike)
                    .join(recommentLike.member, senderMember)
                    .where(recommentLike.recomment.id.eq(notification.getTargetId()))
                    .fetchOne();
                    
            default -> MemberResponseDto.UserInfo.builder()
                    .id(0L)
                    .name(UNKNOWN_USER)
                    .nickname(UNKNOWN_USER)
                    .imageUrl("")
                    .build();
        };
        
        // null인 경우 기본값 반환
        if (result == null) {
            log.warn("알림 ID {}, 타입 {}, 타겟 ID {}에 대한 발신자 정보를 찾을 수 없습니다.", 
                    notification.getId(), notification.getNotificationType(), notification.getTargetId());
            return MemberResponseDto.UserInfo.builder()
                    .id(0L)
                    .name(UNKNOWN_USER)
                    .nickname(UNKNOWN_USER)
                    .imageUrl("")
                    .build();
        }
        
        return result;
    }

    /**
     * 팔로우 알림 발신자 정보 조회
     */
    private MemberResponseDto.UserInfoWithFollowing getSenderInfoWithFollowing(Notification notification) {
        QMember followerMember = QMember.member;
        QFollow follow = QFollow.follow;
        
        if (notification.getNotificationType() == NotificationType.FOLLOWING_CREATED) {
            // 팔로우한 사람 정보 조회 (follow.followerUser)
            return queryFactory
                    .select(com.querydsl.core.types.Projections.constructor(
                            MemberResponseDto.UserInfoWithFollowing.class,
                            followerMember.id,
                            followerMember.nickname,
                            followerMember.profileImgUrl,
                            com.querydsl.core.types.dsl.Expressions.constant(true) // 팔로우 상태
                    ))
                    .from(follow)
                    .join(follow.followerUser, followerMember)
                    .where(follow.id.eq(notification.getTargetId()))
                    .fetchOne();
        }
        
        return MemberResponseDto.UserInfoWithFollowing.builder()
                .id(0L)
                .nickname("알 수 없음")
                .imageUrl("")
                .isFollowed(false)
                .build();
    }

    /**
     * 알림의 target_id를 PostId로 변환
     */
    private Long getPostIdFromNotification(Notification notification) {
        QComment comment = QComment.comment;
        QRecomment recomment = QRecomment.recomment;
        
        return switch (notification.getNotificationType()) {
            case COMMENT_CREATED -> queryFactory
                    .select(comment.post.id)
                    .from(comment)
                    .where(comment.id.eq(notification.getTargetId()))
                    .fetchOne();
                    
            case RECOMMENT_CREATED -> queryFactory
                    .select(comment.post.id)
                    .from(recomment)
                    .join(recomment.comment, comment)
                    .where(recomment.id.eq(notification.getTargetId()))
                    .fetchOne();
                    
            case POST_LIKE_CREATED -> notification.getTargetId();
            
            case COMMENT_LIKE_CREATED -> queryFactory
                    .select(comment.post.id)
                    .from(comment)
                    .where(comment.id.eq(notification.getTargetId()))
                    .fetchOne();
                    
            case RECOMMENT_LIKE_CREATED -> queryFactory
                    .select(comment.post.id)
                    .from(recomment)
                    .join(recomment.comment, comment)
                    .where(recomment.id.eq(notification.getTargetId()))
                    .fetchOne();
                    
            default -> notification.getTargetId();
        };
    }

    /**
     * 알림이 유효한지 검증 (대상 엔티티가 존재하는지 확인)
     */
    private boolean isNotificationValid(Notification notification) {
        QComment comment = QComment.comment;
        QRecomment recomment = QRecomment.recomment;
        QPostLike postLike = QPostLike.postLike;
        QCommentLike commentLike = QCommentLike.commentLike;
        QRecommentLike recommentLike = QRecommentLike.recommentLike;
        QFollow follow = QFollow.follow;
        
        return switch (notification.getNotificationType()) {
            case COMMENT_CREATED -> queryFactory
                    .selectOne()
                    .from(comment)
                    .where(comment.id.eq(notification.getTargetId()))
                    .fetchFirst() != null;
                    
            case RECOMMENT_CREATED -> queryFactory
                    .selectOne()
                    .from(recomment)
                    .where(recomment.id.eq(notification.getTargetId()))
                    .fetchFirst() != null;
                    
            case POST_LIKE_CREATED -> queryFactory
                    .selectOne()
                    .from(postLike)
                    .where(postLike.id.postId.eq(notification.getTargetId()))
                    .fetchFirst() != null;
                    
            case COMMENT_LIKE_CREATED -> queryFactory
                    .selectOne()
                    .from(commentLike)
                    .where(commentLike.comment.id.eq(notification.getTargetId()))
                    .fetchFirst() != null;
                    
            case RECOMMENT_LIKE_CREATED -> queryFactory
                    .selectOne()
                    .from(recommentLike)
                    .where(recommentLike.recomment.id.eq(notification.getTargetId()))
                    .fetchFirst() != null;
                    
            case FOLLOWING_CREATED -> queryFactory
                    .selectOne()
                    .from(follow)
                    .where(follow.id.eq(notification.getTargetId()))
                    .fetchFirst() != null;
                    
            default -> true; // 알 수 없는 타입은 일단 유효하다고 가정
        };
    }

    /**
     * 무효한 알림들을 일괄 삭제
     */
    private void deleteInvalidNotifications(List<Long> notificationIds) {
        QNotification notification = QNotification.notification;
        
        long deletedCount = queryFactory
                .delete(notification)
                .where(notification.id.in(notificationIds))
                .execute();
                
        log.debug("무효한 알림 {}개 삭제됨", deletedCount);
    }
}