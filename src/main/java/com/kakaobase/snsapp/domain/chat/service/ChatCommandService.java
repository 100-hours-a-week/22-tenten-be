package com.kakaobase.snsapp.domain.chat.service;

import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.dto.SimpTimeData;
import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiStreamData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatErrorData;
import com.kakaobase.snsapp.domain.chat.dto.response.StreamData;
import com.kakaobase.snsapp.domain.chat.dto.response.StreamStartData;
import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoom;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoomMember;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.StreamErrorCode;
import com.kakaobase.snsapp.domain.chat.repository.ChatMessageRepository;
import com.kakaobase.snsapp.domain.chat.repository.ChatRoomMemberRepository;
import com.kakaobase.snsapp.domain.chat.repository.ChatRoomRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.util.ChatEventType;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCommandService {

    private final EntityManager em;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatConverter chatConverter;
    
    @Qualifier("webFluxClient")
    private final WebClient webClient;
    
    private final AiServerSseManager aiServerSseManager;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${ai.server.chat.endpoint:/api/ai/chat}")
    private String aiChatEndpoint;

    private static final String CHAT_QUEUE_DESTINATION = "/queue/chat";


    /**
     * 사용자에게 로딩 상태 전송
     */
    @Async
    public void sendLoadingToUser(Long userId) {
        SimpTimeData data = new SimpTimeData(LocalDateTime.now());
        WebSocketPacketImpl<SimpTimeData> packet = new WebSocketPacketImpl<>(ChatEventType.CHAT_STREAM_LOADING.getEvent(), data);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }

    /**
     * 사용자에게 Stream 시작 패킷 전송
     */
    @Async
    public void sendStratToUser(Long userId, String streamId) {
        StreamStartData data = new StreamStartData(streamId, LocalDateTime.now());
        WebSocketPacketImpl<StreamStartData> packet = new WebSocketPacketImpl<>(ChatEventType.CHAT_STREAM_START.getEvent(), data);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }

    /**
     * 사용자에게 Stream 패킷 전송
     */
    @Async
    public void sendStreamDataToUser(Long userId, String content) {
        StreamData streamData = new StreamData(content, LocalDateTime.now());
        WebSocketPacketImpl<StreamData> packet = new WebSocketPacketImpl<>(ChatEventType.CHAT_STREAM.getEvent(), streamData);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }
    
    /**
     * 사용자에게 커스텀 이벤트로 Stream 패킷 전송
     */
    @Async
    public void sendStreamDataToUser(Long userId, String eventType, AiStreamData streamData) {
        WebSocketPacketImpl<AiStreamData> packet = new WebSocketPacketImpl<>(eventType, streamData);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }

    /**
     * 사용자에게 Stream 종료 패킷 전송
     */
    @Async
    public void sendEndToUser(Long userId) {
        SimpTimeData data = new SimpTimeData(LocalDateTime.now());
        WebSocketPacketImpl<SimpTimeData> packet = new WebSocketPacketImpl<>(ChatEventType.CHAT_STREAM_END.getEvent(), data);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }
    
    /**
     * 사용자에게 에러 메시지 전송
     */
    @Async
    public void sendErrorToUser(Long userId, ChatErrorCode errorCode) {
        ChatErrorData data = new ChatErrorData(errorCode.getError(), errorCode.getMessage(), LocalDateTime.now());
        WebSocketPacketImpl<ChatErrorData> packet = new WebSocketPacketImpl<>(ChatEventType.CHAT_STREAM_ERROR.getEvent(), data);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }

    /**
     * 사용자에게 에러 메시지 전송
     */
    @Async
    public void sendStreamErrorToUser(Long userId, StreamErrorCode errorCode) {
        ChatErrorData data = new ChatErrorData(errorCode.getError(), errorCode.getMessage(), LocalDateTime.now());
        WebSocketPacketImpl<ChatErrorData> packet = new WebSocketPacketImpl<>(ChatEventType.CHAT_STREAM_ERROR.getEvent(), data);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }
    
    // ====== 외부 API 호출 관련 메서드들 ======

    /**
     * AI 서버로 채팅 메시지 전송 (SSE Manager 통합)
     */
    @Async
    public void sendMessageToAiServer(Long userId, String message) {
        log.info("AI 서버로 메시지 전송: userId={}, message={}", userId, message);
        
        try {
            // SSE Manager를 통해 메시지 전송 및 StreamId 생성
            String streamId = aiServerSseManager.sendMessageToAiServer(userId, message);
            log.info("AI 서버 메시지 전송 완료: userId={}, streamId={}", userId, streamId);
            
        } catch (Exception e) {
            log.error("AI 서버 메시지 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
            // 비동기 실행에서는 예외를 던지지 않고 로깅만 수행
            // 필요시 AiServerException 생성 가능: new AiServerException(AiServerErrorCode.AI_SERVER_MESSAGE_SEND_FAIL, userId)
        }
    }
    
    /**
     * AI 서버로 채팅 메시지 전송 및 StreamId 반환 (동기 버전)
     */
    public String sendMessageToAiServerSync(Long userId, String message) {
        log.info("AI 서버로 메시지 전송 (동기): userId={}, message={}", userId, message);
        
        try {
            // SSE Manager를 통해 메시지 전송 및 StreamId 생성
            String streamId = aiServerSseManager.sendMessageToAiServer(userId, message);
            log.info("AI 서버 메시지 전송 완료: userId={}, streamId={}", userId, streamId);
            
            return streamId;
            
        } catch (Exception e) {
            log.error("AI 서버 메시지 전송 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new ChatException(ChatErrorCode.AI_SERVER_CONNECTION_FAIL, userId);
        }
    }

    // ====== 데이터베이스 트랜잭션 관련 메서드들 ======

    /**
     * 새 채팅방 생성
     */
    @Transactional
    public Long createBotChatRoom(Long userId) {

        // 새로운 채팅방 생성
        ChatRoom newChatRoom = new ChatRoom(userId);
        ChatRoom savedChatRoom = chatRoomRepository.save(newChatRoom);

        // 사용자와 봇을 채팅방 멤버로 추가
        Member proxyUser = em.getReference(Member.class, userId);
        Member proxyBot = em.getReference(Member.class, BotConstants.BOT_MEMBER_ID);

        // 채팅방 멤버 추가
        ChatRoomMember userMember = new ChatRoomMember(proxyUser, savedChatRoom);
        ChatRoomMember botRoomMember = new ChatRoomMember(proxyBot, savedChatRoom);

        chatRoomMemberRepository.save(userMember);
        chatRoomMemberRepository.save(botRoomMember);

        return savedChatRoom.getId();
    }

    /**
     * 채팅 메시지 저장
     */
    @Async
    @Transactional
    public Long saveChatMessage(Long senderId, String content) {
        // 메시지 엔티티 생성 및 저장
        ChatMessage message = chatConverter.toChatMessage(senderId, content);
        ChatMessage savedMessage = chatMessageRepository.save(message);
        return savedMessage.getId();
    }

    /**
     * 메시지 상태 업데이트 (읽음, 삭제 등)
     */
    @Transactional
    public void updateMessageStatus(Long chatId, Long userId) {
        // TODO: 메시지 상태 읽음 업데이트 로직
        log.info("메시지 상태 업데이트: messageId={}, userId={}", chatId, userId);
    }

    /**
     * AI 응답 메시지 저장
     */
    @Transactional
    public void saveBotMessage(Long userId, String botResponse) {
        log.info("AI 응답 메시지 저장: userId={}", userId);
        
        try {
            // AI 응답 메시지 생성 및 저장
            ChatMessage botMessage = chatConverter.toBotMessage(userId, botResponse);
            chatMessageRepository.save(botMessage);
            
            log.info("AI 응답 메시지 저장 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.error("AI 응답 메시지 저장 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new ChatException(ChatErrorCode.MESSAGE_SAVE_FAIL, userId);
        }
    }
}
