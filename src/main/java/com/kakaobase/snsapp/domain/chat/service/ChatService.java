package com.kakaobase.snsapp.domain.chat.service;

import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.dto.SimpTimeData;
import com.kakaobase.snsapp.domain.chat.dto.request.ChatData;
import com.kakaobase.snsapp.domain.chat.dto.request.StreamAckData;
import com.kakaobase.snsapp.domain.chat.dto.request.StreamStopData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatList;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.repository.ChatMessageRepository;
import com.kakaobase.snsapp.domain.chat.repository.ChatRoomMemberRepository;
import com.kakaobase.snsapp.domain.chat.util.ChatBufferCacheUtil;
import com.kakaobase.snsapp.domain.chat.service.ai.AiServerSseManager;
import com.kakaobase.snsapp.domain.chat.service.ai.AiServerHttpClient;
import com.kakaobase.snsapp.domain.chat.service.streaming.StreamingSessionManager;
import com.kakaobase.snsapp.domain.chat.service.streaming.ChatTimerManager;
import com.kakaobase.snsapp.domain.chat.service.communication.ChatCommandService;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatCommandService chatCommandService;
    private final ChatBufferCacheUtil cacheUtil;
    private final StreamingSessionManager streamingSessionManager;
    private final ChatTimerManager chatTimerManager;
    private final AiServerSseManager aiServerSseManager;
    private final AiServerHttpClient aiServerHttpClient;

    // ====== 채팅 조회 관련 메서드들 ======

    /**
     * 봇과의 채팅 목록 조회
     */
    @Transactional(readOnly = true)
    public ChatList getChatMessages(CustomUserDetails userDetails, Integer limit, Long cursor) {
        Long userId = Long.valueOf(userDetails.getId());
        log.info("봇과의 채팅 목록 조회: userId={}", userId);

        //기존 채팅방이 없다면 채팅방 생성후 빈 값 전송
        if(!chatRoomMemberRepository.existsByChatRoomIdAndMemberId(userId, BotConstants.BOT_MEMBER_ID)) {
            chatCommandService.createBotChatRoom(userId);
            return new ChatList(
                    List.of(),
                    false
            );
        }
        
        return chatMessageRepository.findMessagesByChatRoomId(userId, limit, cursor);
    }

    // ====== 타이핑 버퍼 메시지 처리 ======
    
    /**
     * 타이핑 이벤트 처리
     */
    public void handleTypingEvent(Long userId) {
        log.info("타이핑 이벤트 처리: userId={}", userId);

        try {
            // Redis TTL 연장
            cacheUtil.extendTTL(userId);
            
            // AI 서버 전송 타이머 리셋 (1초)
            chatTimerManager.resetTimer(userId);
            
            log.debug("타이핑 이벤트 처리 완료: userId={}", userId);

        } catch (Exception e) {
            log.error("타이핑 이벤트 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new ChatException(ChatErrorCode.CHAT_BUFFER_EXTEND_FAIL, userId);
        }
    }

    public void handleSendEvent(Long userId, ChatData chatData) {
        log.debug("유저 채팅 송신 완료: userId={}, content={} time={}", userId, chatData.content(), chatData.timestamp());
        String message = chatData.content();

        try {

            if (message == null || message.isBlank()) {
                log.warn("빈 메시지 추가 시도: userId={}", userId);
                throw new ChatException(ChatErrorCode.CHAT_INVALID, userId);
            }

            // DB에 사용자 메시지 저장
            chatCommandService.saveChatMessage(userId, message);
            
            // AI 서버 상태 확인
            if (aiServerSseManager.getHealthStatus().isDisconnected()) {
                log.warn("AI 서버 연결 상태 불량으로 메시지 처리 중단: userId={}, status={}", 
                    userId, aiServerSseManager.getHealthStatus());
                throw new ChatException(ChatErrorCode.AI_SERVER_CONNECTION_FAIL, userId);
            }

            // 버퍼에 메시지 추가
            cacheUtil.appendMessage(userId, message);
            
            // AI 서버 전송 타이머 리셋 (1초)
            chatTimerManager.resetTimer(userId);
            
            log.debug("채팅 메시지 추가 완룀: userId={}", userId);

        } catch (ChatException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("타이핑 이벤트 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new ChatException(ChatErrorCode.CHAT_BUFFER_ADD_FAIL, userId);
        }
    }

    public void handleStopEvent(Long userId, StreamStopData data) {
        String streamId = data.streamId();
        log.info("채팅 중단 처리: userId={}, streamId={}", userId, streamId);

        try {
            // AI 스트리밍 세션 취소 처리 (StreamId 직접 사용)
            streamingSessionManager.cancelStreaming(streamId);
            log.info("스트리밍 세션 취소 완료: userId={}, streamId={}", userId, streamId);
            
            // AI 서버에 스트리밍 중지 요청 전송
            aiServerHttpClient.stopStream(streamId);
            log.info("AI 서버 스트리밍 중지 요청 전송 완료: streamId={}, userId={}", streamId, userId);

        } catch (Exception e) {
            log.error("채팅 중단 처리 실패: userId={}, streamId={}, error={}", userId, streamId, e.getMessage(), e);
            throw new ChatException(ChatErrorCode.AI_SERVER_CONNECTION_FAIL, userId);
        }
    }

    public void handleStreamEndAck(Long userId, StreamAckData data) {
        log.info("스트림 종료 ACK 처리: userId={}", userId);

        try {
            chatCommandService.updateMessageStatus(data.chatId(), userId);
            log.info("스트림 종료 ACK 처리 완료: userId={}", userId);

        } catch (Exception e) {
            log.error("스트림 종료 ACK 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    public void handleStreamEndNack(Long userId, StreamAckData data) {
        log.info("스트림 종료 후 NACK응답 확인: userId={}, chatId={}, Time: {}", userId, data.chatId(), data.timestamp());
    }
}