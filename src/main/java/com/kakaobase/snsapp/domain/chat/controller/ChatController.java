package com.kakaobase.snsapp.domain.chat.controller;


import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiStreamData;
import com.kakaobase.snsapp.domain.chat.dto.request.ChatData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatList;
import com.kakaobase.snsapp.domain.chat.dto.websocket.ChatAckData;
import com.kakaobase.snsapp.domain.chat.service.ChatCommandService;
import com.kakaobase.snsapp.domain.chat.service.ChatService;
import com.kakaobase.snsapp.domain.chat.service.TypingSessionManager;
import com.kakaobase.snsapp.domain.chat.util.ChatEventType;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;
import com.kakaobase.snsapp.global.common.response.CustomResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import java.security.Principal;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private static final String CHAT_QUEUE_DESTINATION = "/queue/chat";
    
    private final ChatService chatService;
    private final ChatCommandService chatCommandService;
    private final TypingSessionManager typingSessionManager;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/api/chat/bot")
    public CustomResponse<ChatList> getChatList(
            @Parameter(description = "한 번에 불러올 댓글 수 (기본값: 40)") @RequestParam(required = false, defaultValue = "40") Integer limit,
            @Parameter(description = "페이지네이션 커서 (이전 응답의 next_cursor)") @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        ChatList result = chatService.getChatingWithBot(userDetails, limit, cursor);

        return CustomResponse.success("채팅 조회에 성공하였습니다.", result);
    }

    @MessageMapping("/chat.send")
    public void handleChatSend(@Payload WebSocketPacket<ChatData> packet, Principal principal) {
        String event = packet.event;
        Long userId = getUserIdFromPrincipal(principal);
        
        log.info("채팅 이벤트 수신: event={}, userId={}", event, userId);
        
        switch (event) {
            case "chat.send" -> handleChatMessage(packet, userId);
            case "chat.typing" -> handleChatTyping(userId);
            case "chat.stop" -> handleChatStop(userId);
            case "chat.stream.end.ack" -> handleChatStreamEndAck(userId);
            case "chat.stream.end.nack" -> handleChatStreamEndNack(userId);
            default -> {
                log.warn("알 수 없는 채팅 이벤트: {}", event);
                sendErrorToUser(userId, "Unknown event type: " + event);
            }
        }
    }
    
    private void handleChatMessage(WebSocketPacket<ChatData> packet, Long userId) {
        log.info("채팅 메시지 전송 처리: userId={}, message={}", userId, packet.data.content());
        
        try {
            // 1. 타이핑 세션에 메시지 추가 (버퍼링)
            typingSessionManager.addMessage(userId, packet.data.content(), this::processBufferedMessage);
            
            log.debug("메시지가 타이핑 버퍼에 추가됨: userId={}", userId);
            
        } catch (Exception e) {
            log.error("채팅 메시지 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
            sendErrorToUser(userId, "메시지 처리 중 오류가 발생했습니다.");
        }
    }
    
    private void handleChatTyping(Long userId) {
        log.info("타이핑 상태 처리: userId={}", userId);
        
        try {
            // 타이핑 세션 활동 갱신 (1초 타이머 리셋)
            typingSessionManager.handleTypingEvent(userId, this::processBufferedMessage);
            
            log.debug("타이핑 세션 활동 갱신: userId={}", userId);
            
        } catch (Exception e) {
            log.error("타이핑 상태 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    private void handleChatStop(Long userId) {
        log.info("채팅 중단 처리: userId={}", userId);
        
        try {
            // AI 스트리밍 중단 처리
            // TODO: AI 서버 스트리밍 중단 로직 구현
            sendStopToUser(userId);
            
        } catch (Exception e) {
            log.error("채팅 중단 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
            sendErrorToUser(userId, "채팅 중단 처리 중 오류가 발생했습니다.");
        }
    }
    
    private void handleChatStreamEndAck(Long userId) {
        log.info("스트림 종료 ACK 처리: userId={}", userId);
        
        try {
            // 채팅방의 읽지 않은 메시지를 읽음 처리
            chatService.markMessagesAsRead(userId, userId, null);
            log.info("스트림 종료 ACK 처리 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.error("스트림 종료 ACK 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    private void handleChatStreamEndNack(Long userId) {
        log.info("스트림 종료 NACK 처리: userId={}", userId);
        
        try {
            // NACK 처리 - 재전송 또는 에러 처리
            sendErrorToUser(userId, "스트림 수신에 실패했습니다.");
            log.info("스트림 종료 NACK 처리 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.error("스트림 종료 NACK 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal instanceof CustomUserDetails userDetails) {
            return Long.valueOf(userDetails.getId());
        }
        return 0L;
    }
    
    private void sendLoadingToUser(Long userId) {
        ChatAckData data = new ChatAckData(null, "Message received, processing...", java.time.LocalDateTime.now());
        WebSocketPacketImpl<ChatAckData> packet = new WebSocketPacketImpl<>(ChatEventType.CHAT_MESSAGE_LOADING.getEvent(), data);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }
    
    private void sendStopToUser(Long userId) {
        ChatAckData data = new ChatAckData(null, "Chat stopped", java.time.LocalDateTime.now());
        WebSocketPacketImpl<ChatAckData> packet = new WebSocketPacketImpl<>(ChatEventType.CHAT_MESSAGE_LOADING.getEvent(), data);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }
    
    private void sendErrorToUser(Long userId, String message) {
        ChatAckData data = new ChatAckData(null, message, java.time.LocalDateTime.now());
        WebSocketPacketImpl<ChatAckData> packet = new WebSocketPacketImpl<>(ChatEventType.CHAT_MESSAGE_STREAM_ERROR.getEvent(), data);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }
    
    private void sendStreamDataToUser(Long userId, AiStreamData streamData) {
        ChatAckData data = new ChatAckData(null, streamData.message(), streamData.timestamp());
        
        String eventType = streamData.isComplete() 
            ? ChatEventType.CHAT_MESSAGE_STREAM_END.getEvent()
            : ChatEventType.CHAT_MESSAGE_STREAM.getEvent();
            
        WebSocketPacketImpl<ChatAckData> packet = new WebSocketPacketImpl<>(eventType, data);
        messagingTemplate.convertAndSendToUser(userId.toString(), CHAT_QUEUE_DESTINATION, packet);
    }
    
    /**
     * 1초 타이머 만료 시 버퍼된 메시지 처리
     */
    private void processBufferedMessage(com.kakaobase.snsapp.domain.chat.model.TypingSession session) {
        Long userId = session.getUserId();
        String bufferedContent = session.consumeBuffer();
        
        log.info("버퍼된 메시지 처리: userId={}, content={}", userId, bufferedContent);
        
        if (bufferedContent != null && !bufferedContent.trim().isEmpty()) {
            try {
                // 1. 사용자 메시지 저장
                com.kakaobase.snsapp.domain.chat.dto.request.ChatData chatData = 
                    new com.kakaobase.snsapp.domain.chat.dto.request.ChatData(bufferedContent, java.time.LocalDateTime.now());
                chatService.saveUserMessage(userId, chatData);
                
                // 2. 로딩 상태 전송
                sendLoadingToUser(userId);
                
                // 3. AI 서버로 메시지 전송 (비동기)
                chatCommandService.sendMessageToAiServer(userId, bufferedContent)
                        .thenAccept(streamFlux -> {
                            if (streamFlux != null) {
                                streamFlux.subscribe(
                                        streamData -> {
                                            // 스트림 데이터를 클라이언트로 전송
                                            sendStreamDataToUser(userId, streamData);
                                            
                                            // 스트림 완료 시 AI 응답 저장
                                            if (streamData.isComplete()) {
                                                chatService.saveAiMessage(userId, streamData.message(), null);
                                            }
                                        },
                                        error -> {
                                            log.error("AI 스트리밍 오류: userId={}, error={}", userId, error.getMessage());
                                            sendErrorToUser(userId, "AI 응답 처리 중 오류가 발생했습니다.");
                                        }
                                );
                            }
                        });
                
            } catch (Exception e) {
                log.error("버퍼된 메시지 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
                sendErrorToUser(userId, "메시지 처리 중 오류가 발생했습니다.");
            }
        }
    }
}
