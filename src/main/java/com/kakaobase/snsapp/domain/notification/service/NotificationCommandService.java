package com.kakaobase.snsapp.domain.notification.service;


import com.kakaobase.snsapp.domain.members.dto.MemberResponseDto;
import com.kakaobase.snsapp.domain.notification.converter.NotificationConverter;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationFetchData;
import com.kakaobase.snsapp.domain.notification.dto.records.NotificationFollowingData;
import com.kakaobase.snsapp.domain.notification.entity.Notification;
import com.kakaobase.snsapp.domain.notification.error.NotificationErrorCode;
import com.kakaobase.snsapp.domain.notification.error.NotificationException;
import com.kakaobase.snsapp.domain.notification.repository.NotificationRepository;
import com.kakaobase.snsapp.domain.notification.util.NotificationType;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;
import com.kakaobase.snsapp.global.error.code.ErrorPacketData;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationConverter notifConverter;
    private final NotificationRepository notifRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificationRepository notificationRepository;

    private static final String NOTIFY_SUBSCRIBE_PATH = "/queue/notification";

    @Transactional
    public Long createNotification(Long receiverId, Long senderId, NotificationType type, Long targetId) {
        Notification notification = notifConverter.toEntity(receiverId, senderId, type, targetId);
        notifRepository.save(notification);
        return notification.getId();
    }

    @Transactional
    public void updateNotificationRead(Long notifId) {
        Notification notification = notificationRepository.findById(notifId)
                .orElseThrow(()-> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
    }

    @Transactional
    public void deleteNotification(Long notifId) {
        Notification notification = notificationRepository.findById(notifId)
                .orElseThrow(()-> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notifRepository.delete(notification);
    }

    /**
     * 특정 타입, 타겟 ID, 수신자 ID로 알림 삭제 (오버로딩)
     */
    @Transactional
    public void deleteNotification(NotificationType notificationType, Long targetId, Long receiverId) {
        Optional<Notification> notification = notificationRepository.findByNotificationTypeAndTargetIdAndReceiverId(notificationType, targetId, receiverId);
        
        if (notification.isPresent()) {
            notifRepository.delete(notification.get());
            log.info("알림 삭제 완료 - 타입: {}, 타겟ID: {}, 수신자ID: {}", 
                    notificationType, targetId, receiverId);
        } else {
            log.debug("삭제할 알림이 없음 - 타입: {}, 타겟ID: {}, 수신자ID: {}", 
                    notificationType, targetId, receiverId);
        }
    }

    @Async
    public void sendNotification(Long receiverId, Long notifId, NotificationType type, String content, Long targetId, MemberResponseDto.UserInfo userInfo) {
        WebSocketPacket<NotificationData> packet = notifConverter.toNewPacket(notifId, type, targetId, content, userInfo);
        simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), NOTIFY_SUBSCRIBE_PATH, packet);
    }

    @Async
    public void sendNotification(Long receiverId, Long notifId, NotificationType type, MemberResponseDto.UserInfoWithFollowing userInfo) {
        WebSocketPacket<NotificationFollowingData> packet = notifConverter.toNewPacket(notifId, type, userInfo);
        simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), NOTIFY_SUBSCRIBE_PATH, packet);
    }


    @Transactional
    public List<WebSocketPacket<?>> getAllNotificationsToUser(Long userId) {
        log.info("사용자 {}의 모든 알림 조회 및 전송", userId);
        
        try {
            // 사용자의 모든 알림을 WebSocketPacket List로 조회
            return notificationRepository.findAllNotificationsByUserId(userId);

        } catch (Exception e) {
            log.error("사용자 {}의 모든 알림 전송 실패", userId, e);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_FETCH_FAIL);
        }
    }

    /**
     * 무효한 알림들을 비동기로 일괄 삭제
     */
    @Async
    @Transactional
    public void deleteInvalidNotifications(List<Long> invalidNotificationIds) {
        try {
            log.info("무효한 알림 {}개 비동기 삭제 시작", invalidNotificationIds.size());
            notifRepository.deleteAllById(invalidNotificationIds);
            log.info("무효한 알림 {}개 비동기 삭제 완료", invalidNotificationIds.size());
        } catch (Exception e) {
            log.error("무효한 알림 삭제 중 오류 발생: {}", invalidNotificationIds, e);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_DELETE_FAIL);
        }
    }

    /**
     * NotificationFetchData를 WebSocket으로 전송
     */
    @Async
    public void sendNotificationFetchData(Long userId, WebSocketPacketImpl<NotificationFetchData> packet) {
        try {
            simpMessagingTemplate.convertAndSendToUser(userId.toString(), NOTIFY_SUBSCRIBE_PATH, packet);
            log.info("사용자 {}에게 알림 데이터 전송 완료 (총 {}개, 읽지 않은 {}개)", 
                    userId, packet.data.notifications().size(), packet.data.unread_count());
        } catch (Exception e) {
            log.error("사용자 {}에게 알림 데이터 전송 실패", userId, e);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_FETCH_FAIL);
        }
    }

    @Async 
    void sendNotificationError(NotificationErrorCode errorCode, Long receiverId){
        WebSocketPacketImpl<ErrorPacketData> errorPacket = notifConverter.toErrorPacket(errorCode);
        simpMessagingTemplate.convertAndSendToUser(receiverId.toString(), NOTIFY_SUBSCRIBE_PATH, errorPacket);
    }

    // ====== 채팅 관련 알림 메서드들 ======

    /**
     * 채팅 메시지 전송 시 알림 생성
     * 봇과의 채팅이므로 특별한 알림 생성이 필요한지 판단 후 처리
     */
    @Async
    @Transactional
    public void createChatMessageNotification(Long userId, String message) {
        // TODO: 채팅 메시지 전송 시 알림 생성 로직
        // - 봇과의 채팅이므로 일반적인 알림과 다른 처리 필요
        // - 채팅 히스토리 저장을 위한 알림인지 판단
        // - 필요시 ChatMessage 엔티티와 연계하여 알림 생성
        log.info("채팅 메시지 알림 생성: userId={}, message={}", userId, message);
    }

    /**
     * 채팅 메시지 수신 완료 시 알림 업데이트
     * AI 서버로부터 응답이 완료되었을 때 호출
     */
    @Async
    @Transactional
    public void updateChatMessageStatus(Long userId, Long chatId, boolean success) {
        // TODO: 채팅 메시지 상태 업데이트 로직
        // - AI 서버 응답 성공/실패에 따른 상태 업데이트
        // - 필요시 사용자에게 처리 완료 알림 전송
        // - 채팅 히스토리 상태 변경
        log.info("채팅 메시지 상태 업데이트: userId={}, chatId={}, success={}", userId, chatId, success);
    }

    /**
     * 채팅 오류 발생 시 사용자에게 오류 알림 전송
     */
    @Async
    public void sendChatErrorNotification(Long userId, String errorMessage) {
        // TODO: 채팅 오류 알림 전송 로직
        // - AI 서버 연결 실패, 타임아웃 등의 오류 상황 처리
        // - 사용자에게 적절한 오류 메시지 전송
        // - 재시도 가능 여부 안내
        log.error("채팅 오류 알림 전송: userId={}, error={}", userId, errorMessage);
    }

    /**
     * 채팅 세션 시작 시 알림
     * 사용자가 새로운 채팅을 시작할 때 호출
     */
    @Async
    public void notifyChatSessionStart(Long userId) {
        // TODO: 채팅 세션 시작 알림 로직
        // - 새로운 채팅 세션 시작을 기록
        // - 필요시 사용자에게 환영 메시지 전송
        // - 채팅 룸 생성 및 초기화
        log.info("채팅 세션 시작 알림: userId={}", userId);
    }

    /**
     * 채팅 세션 종료 시 알림
     * 사용자가 채팅을 종료하거나 타임아웃 발생 시 호출
     */
    @Async
    public void notifyChatSessionEnd(Long userId, String reason) {
        // TODO: 채팅 세션 종료 알림 로직
        // - 채팅 세션 종료 이유 기록 (정상 종료, 타임아웃, 오류 등)
        // - 필요시 사용자에게 세션 종료 안내
        // - 채팅 룸 정리 및 리소스 해제
        log.info("채팅 세션 종료 알림: userId={}, reason={}", userId, reason);
    }

    /**
     * 채팅 타이핑 상태 변경 알림
     * 사용자의 타이핑 상태가 변경될 때 호출 (필요시)
     */
    @Async
    public void notifyChatTypingStatus(Long userId, boolean isTyping) {
        // TODO: 채팅 타이핑 상태 알림 로직
        // - 봇과의 채팅에서는 일반적으로 불필요하지만, 
        // - 사용자 활동 로깅이나 분석을 위해 사용 가능
        // - 실시간 상태 업데이트가 필요한 경우 구현
        log.debug("채팅 타이핑 상태 알림: userId={}, isTyping={}", userId, isTyping);
    }
}
