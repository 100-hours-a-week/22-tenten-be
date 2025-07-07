package com.kakaobase.snsapp.domain.notification.service;

import com.kakaobase.snsapp.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.kakaobase.snsapp.domain.notification.entity.QNotification;
import com.kakaobase.snsapp.domain.members.entity.QMember;
import com.kakaobase.snsapp.domain.comments.entity.QComment;
import com.kakaobase.snsapp.domain.comments.entity.QRecomment;
import com.kakaobase.snsapp.domain.posts.entity.QPost;
import com.kakaobase.snsapp.domain.follow.entity.QFollow;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCleanupService {

    private final NotificationRepository notificationRepository;
    private final JPAQueryFactory queryFactory;

    /**
     * 무효한 알림들을 정리하는 메서드
     * - 삭제된 사용자를 참조하는 알림
     * - 삭제된 댓글/대댓글을 참조하는 알림
     * - 삭제된 게시글을 참조하는 알림
     */
    @Transactional
    public void cleanupInvalidNotifications() {
        log.info("무효한 알림 정리 작업 시작");
        
        long startTime = System.currentTimeMillis();
        int totalCleaned = 0;
        
        // 1. 삭제된 발신자를 참조하는 알림 정리
        int deletedSenderNotifications = cleanupNotificationsWithDeletedSenders();
        totalCleaned += deletedSenderNotifications;
        
        // 2. 삭제된 댓글을 참조하는 알림 정리  
        int deletedCommentNotifications = cleanupNotificationsWithDeletedComments();
        totalCleaned += deletedCommentNotifications;
        
        // 3. 삭제된 대댓글을 참조하는 알림 정리
        int deletedRecommentNotifications = cleanupNotificationsWithDeletedRecomments();
        totalCleaned += deletedRecommentNotifications;
        
        // 4. 삭제된 게시글을 참조하는 알림 정리
        int deletedPostNotifications = cleanupNotificationsWithDeletedPosts();
        totalCleaned += deletedPostNotifications;
        
        // 5. 삭제된 팔로우를 참조하는 알림 정리
        int deletedFollowNotifications = cleanupNotificationsWithDeletedFollows();
        totalCleaned += deletedFollowNotifications;
        
        long endTime = System.currentTimeMillis();
        log.info("무효한 알림 정리 작업 완료 - 총 {}개 정리됨, 소요시간: {}ms", 
                totalCleaned, (endTime - startTime));
    }

    /**
     * 삭제된 발신자(Member)를 참조하는 알림 정리
     */
    private int cleanupNotificationsWithDeletedSenders() {
        log.debug("삭제된 발신자를 참조하는 알림 정리 시작");
        
        QNotification notification = QNotification.notification;
        QMember member = QMember.member;
        
        // 삭제된 발신자를 참조하는 알림 ID 조회
        List<Long> invalidNotificationIds = queryFactory
                .select(notification.id)
                .from(notification)
                .leftJoin(member).on(notification.senderId.eq(member.id))
                .where(member.id.isNull())
                .fetch();
        
        if (invalidNotificationIds.isEmpty()) {
            log.debug("삭제된 발신자를 참조하는 알림이 없습니다");
            return 0;
        }
        
        // 알림 삭제
        long deletedCount = queryFactory
                .delete(notification)
                .where(notification.id.in(invalidNotificationIds))
                .execute();
        
        log.info("삭제된 발신자를 참조하는 알림 {}개 정리 완료", deletedCount);
        return (int) deletedCount;
    }

    /**
     * 삭제된 댓글을 참조하는 알림 정리
     */
    private int cleanupNotificationsWithDeletedComments() {
        log.debug("삭제된 댓글을 참조하는 알림 정리 시작");
        
        QNotification notification = QNotification.notification;
        QComment comment = QComment.comment;
        
        // 삭제된 댓글을 참조하는 알림 ID 조회
        List<Long> invalidNotificationIds = queryFactory
                .select(notification.id)
                .from(notification)
                .leftJoin(comment).on(notification.targetId.eq(comment.id))
                .where(notification.notificationType.stringValue().in("COMMENT_CREATED", "COMMENT_LIKE_CREATED")
                       .and(comment.id.isNull()))
                .fetch();
        
        if (invalidNotificationIds.isEmpty()) {
            log.debug("삭제된 댓글을 참조하는 알림이 없습니다");
            return 0;
        }
        
        // 알림 삭제
        long deletedCount = queryFactory
                .delete(notification)
                .where(notification.id.in(invalidNotificationIds))
                .execute();
        
        log.info("삭제된 댓글을 참조하는 알림 {}개 정리 완료", deletedCount);
        return (int) deletedCount;
    }

    /**
     * 삭제된 대댓글을 참조하는 알림 정리
     */
    private int cleanupNotificationsWithDeletedRecomments() {
        log.debug("삭제된 대댓글을 참조하는 알림 정리 시작");
        
        QNotification notification = QNotification.notification;
        QRecomment recomment = QRecomment.recomment;
        
        // 삭제된 대댓글을 참조하는 알림 ID 조회
        List<Long> invalidNotificationIds = queryFactory
                .select(notification.id)
                .from(notification)
                .leftJoin(recomment).on(notification.targetId.eq(recomment.id))
                .where(notification.notificationType.stringValue().in("RECOMMENT_CREATED", "RECOMMENT_LIKE_CREATED")
                       .and(recomment.id.isNull()))
                .fetch();
        
        if (invalidNotificationIds.isEmpty()) {
            log.debug("삭제된 대댓글을 참조하는 알림이 없습니다");
            return 0;
        }
        
        // 알림 삭제
        long deletedCount = queryFactory
                .delete(notification)
                .where(notification.id.in(invalidNotificationIds))
                .execute();
        
        log.info("삭제된 대댓글을 참조하는 알림 {}개 정리 완료", deletedCount);
        return (int) deletedCount;
    }

    /**
     * 삭제된 게시글을 참조하는 알림 정리
     */
    private int cleanupNotificationsWithDeletedPosts() {
        log.debug("삭제된 게시글을 참조하는 알림 정리 시작");
        
        QNotification notification = QNotification.notification;
        QPost post = QPost.post;
        
        // 삭제된 게시글을 참조하는 알림 ID 조회
        List<Long> invalidNotificationIds = queryFactory
                .select(notification.id)
                .from(notification)
                .leftJoin(post).on(notification.targetId.eq(post.id))
                .where(notification.notificationType.stringValue().eq("POST_LIKE_CREATED")
                       .and(post.id.isNull()))
                .fetch();
        
        if (invalidNotificationIds.isEmpty()) {
            log.debug("삭제된 게시글을 참조하는 알림이 없습니다");
            return 0;
        }
        
        // 알림 삭제
        long deletedCount = queryFactory
                .delete(notification)
                .where(notification.id.in(invalidNotificationIds))
                .execute();
        
        log.info("삭제된 게시글을 참조하는 알림 {}개 정리 완료", deletedCount);
        return (int) deletedCount;
    }

    /**
     * 삭제된 팔로우를 참조하는 알림 정리
     */
    private int cleanupNotificationsWithDeletedFollows() {
        log.debug("삭제된 팔로우를 참조하는 알림 정리 시작");
        
        QNotification notification = QNotification.notification;
        QFollow follow = QFollow.follow;
        
        // 삭제된 팔로우를 참조하는 알림 ID 조회
        List<Long> invalidNotificationIds = queryFactory
                .select(notification.id)
                .from(notification)
                .leftJoin(follow).on(notification.targetId.eq(follow.id))
                .where(notification.notificationType.stringValue().eq("FOLLOWING_CREATED")
                       .and(follow.id.isNull()))
                .fetch();
        
        if (invalidNotificationIds.isEmpty()) {
            log.debug("삭제된 팔로우를 참조하는 알림이 없습니다");
            return 0;
        }
        
        // 알림 삭제
        long deletedCount = queryFactory
                .delete(notification)
                .where(notification.id.in(invalidNotificationIds))
                .execute();
        
        log.info("삭제된 팔로우를 참조하는 알림 {}개 정리 완료", deletedCount);
        return (int) deletedCount;
    }

    /**
     * 오래된 알림 정리 (선택적 기능)
     * @param daysOld 며칠 이전의 알림을 삭제할지
     */
    @Transactional
    public int cleanupOldNotifications(int daysOld) {
        log.info("{}일 이전의 오래된 알림 정리 시작", daysOld);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        QNotification notification = QNotification.notification;
        
        // 지정된 일수보다 오래된 알림 삭제
        long deletedCount = queryFactory
                .delete(notification)
                .where(notification.createdAt.before(cutoffDate))
                .execute();
        
        log.info("{}일 이전의 오래된 알림 {}개 정리 완료", daysOld, deletedCount);
        return (int) deletedCount;
    }

    /**
     * 특정 알림 ID 목록을 기반으로 알림 삭제
     * @param notificationIds 삭제할 알림 ID 목록
     * @return 삭제된 알림 개수
     */
    @Transactional
    public int deleteNotificationsByIds(List<Long> notificationIds) {
        if (notificationIds.isEmpty()) {
            return 0;
        }
        
        log.debug("알림 ID {}개를 기반으로 일괄 삭제 시작", notificationIds.size());
        
        QNotification notification = QNotification.notification;
        
        long deletedCount = queryFactory
                .delete(notification)
                .where(notification.id.in(notificationIds))
                .execute();
        
        log.info("알림 ID 기반 일괄 삭제 완료 - {}개 삭제", deletedCount);
        return (int) deletedCount;
    }
}