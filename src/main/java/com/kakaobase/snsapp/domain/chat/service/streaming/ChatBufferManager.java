package com.kakaobase.snsapp.domain.chat.service.streaming;

import com.kakaobase.snsapp.domain.chat.dto.ai.request.ChatBlockData;
import com.kakaobase.snsapp.domain.chat.event.LoadingEvent;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.service.ai.AiServerHttpClient;
import com.kakaobase.snsapp.domain.chat.service.ai.AiServerSseManager;
import com.kakaobase.snsapp.domain.chat.service.communication.ChatWebSocketService;
import com.kakaobase.snsapp.domain.chat.util.ChatBufferCacheUtil;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.members.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 채팅 버퍼 → AI 서버 전송 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBufferManager {
    
    private final ChatBufferCacheUtil cacheUtil;
    private final StreamingSessionManager streamingSessionManager;
    private final AiServerHttpClient aiServerHttpClient;
    private final MemberRepository memberRepository;
    private final AiServerSseManager aiServerSseManager;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatWebSocketService chatWebSocketService;

    /**
     * 채팅 버퍼를 AI 서버로 전송 (타이머에서 호출)
     */
    public void sendBufferToAiServer(Long userId) {
        log.info("AI 서버 자동 전송 시작: userId={}, 시작 시간={}", userId, System.currentTimeMillis());
        
        // 1. AI 서버 상태 확인
        if (aiServerSseManager.getHealthStatus().isDisconnected()) {
            log.warn("AI 서버 연결 상태 불량으로 자동 전송 중단: userId={}, status={}", 
                userId, aiServerSseManager.getHealthStatus());
            chatWebSocketService.sendChatErrorToUser(userId, ChatErrorCode.AI_SERVER_CONNECTION_FAIL);
            return;
        }
        log.info("AI 서버 헬스체크 통과: userId={}, status={}", userId, aiServerSseManager.getHealthStatus());
        
        // 2. 버퍼 존재 여부 확인
        if (!cacheUtil.hasBuffer(userId)) {
            log.info("버퍼가 없어서 전송 안함: userId={}", userId);
            chatWebSocketService.sendChatErrorToUser(userId, ChatErrorCode.CHAT_BUFFER_NOT_FOUND);
            return;
        }
        log.info("버퍼 존재 확인: userId={}", userId);
        
        // 3. 버퍼 내용 가져오기 및 삭제
        String bufferContent = cacheUtil.getAndDeleteBuffer(userId);
        if (bufferContent == null || bufferContent.trim().isEmpty()) {
            log.info("버퍼 내용이 비어서 전송 안함: userId={}", userId);
            chatWebSocketService.sendChatErrorToUser(userId, ChatErrorCode.CHAT_BUFFER_INVALID);
            return;
        }
        log.info("버퍼 내용 가져오기 완료: userId={}, contentLength={}", userId, bufferContent.length());
        
        // 4. 사용자 정보 조회
        Member member = memberRepository.findById(userId)
            .orElseThrow(() -> new ChatException(ChatErrorCode.USER_NOT_FOUND, userId));
        log.info("사용자 정보 조회 완료: userId={}, nickname={}", userId, member.getNickname());
        
        // 5. 스트리밍 세션 시작 및 StreamId 생성
        String streamId = streamingSessionManager.startStreaming(userId);
        log.info("스트리밍 세션 시작: userId={}, streamId={}", userId, streamId);
        
        // 6. ChatBlockData 생성
        ChatBlockData chatBlockData = ChatBlockData.builder()
            .streamId(streamId)
            .nickname(member.getNickname())
            .userId(member.getId())
            .className(member.getClassName())
            .content(bufferContent)
            .timestamp(LocalDateTime.now())
            .isRead(false)
            .build();
        log.info("ChatBlockData 생성 완료: userId={}, streamId={}", userId, streamId);
        
        // 7. AI 서버로 HTTP 전송
        aiServerHttpClient.sendChatBlock(chatBlockData);
        log.info("AI 서버 HTTP 전송 완료: userId={}, streamId={}", userId, streamId);
        
        // 8. 사용자에게 로딩 알림 (이벤트 발행)
        log.info("LoadingEvent 발행 시작: userId={}, streamId={}, 발행 시간={}", 
            userId, streamId, System.currentTimeMillis());
        eventPublisher.publishEvent(new LoadingEvent(userId, streamId, bufferContent));
        log.info("LoadingEvent 발행 완료: userId={}, streamId={}", userId, streamId);
        
        log.info("AI 서버 자동 전송 완료: userId={}, streamId={}, contentLength={}, 완료 시간={}", 
            userId, streamId, bufferContent.length(), System.currentTimeMillis());
    }
}