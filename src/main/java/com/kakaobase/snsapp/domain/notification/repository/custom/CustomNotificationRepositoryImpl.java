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
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

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

        List<WebSocketPacket<?>> result = new ArrayList<>();
        
        for (Notification notif : notifications) {
            String content = getContentByType(notif.getNotificationType());
            
            if (notif.getNotificationType() == NotificationType.FOLLOWING_CREATED) {
                // 팔로우 알림 처리
                MemberResponseDto.UserInfoWithFollowing sender = getSenderInfoWithFollowing(notif);
                WebSocketPacket<?> packet = notificationConverter.toNewPacket(
                        notif.getId(),
                        notif.getNotificationType(),
                        sender,
                        notif.getCreatedAt(),
                        notif.getIsRead()
                );
                result.add(packet);
            } else {
                // 일반 알림 처리 (댓글, 좋아요 등) - target_id는 PostId로 변환
                MemberResponseDto.UserInfo sender = getSenderInfo(notif);
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
            }
        }
        
        return result;
    }

    private String getContentByType(NotificationType type) {
        return switch (type) {
            case COMMENT_CREATED -> "댓글을 작성했습니다";
            case RECOMMENT_CREATED -> "답글을 작성했습니다";
            case POST_LIKE_CREATED -> "게시글에 좋아요를 눌렀습니다";
            case COMMENT_LIKE_CREATED -> "댓글에 좋아요를 눌렀습니다";
            case RECOMMENT_LIKE_CREATED -> "답글에 좋아요를 눌렀습니다";
            case FOLLOWING_CREATED -> "팔로우했습니다";
            default -> "알림이 있습니다";
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
        
        return switch (notification.getNotificationType()) {
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
                    .where(commentLike.id.commentId.eq(notification.getTargetId()))
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
                    .where(recommentLike.id.recommentId.eq(notification.getTargetId()))
                    .fetchOne();
                    
            default -> MemberResponseDto.UserInfo.builder()
                    .id(0L)
                    .name("알 수 없음")
                    .nickname("알 수 없음")
                    .imageUrl("")
                    .build();
        };
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
}