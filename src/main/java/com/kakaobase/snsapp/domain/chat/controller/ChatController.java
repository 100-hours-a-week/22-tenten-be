package com.kakaobase.snsapp.domain.chat.controller;


import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.chat.dto.request.ChatData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatList;
import com.kakaobase.snsapp.domain.chat.dto.websocket.ChatAckData;
import com.kakaobase.snsapp.domain.chat.service.ChatService;
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
        // TODO: AI 서버로 메시지 전송 로직
        sendLoadingToUser(userId);
    }
    
    private void handleChatTyping(Long userId) {
        log.info("타이핑 상태 처리: userId={}", userId);
        // TODO: 타이핑 상태 처리 로직 (응답 없음)
    }
    
    private void handleChatStop(Long userId) {
        log.info("채팅 중단 처리: userId={}", userId);
        // TODO: 채팅 중단 처리 로직
        sendStopToUser(userId);
    }
    
    private void handleChatStreamEndAck(Long userId) {
        log.info("스트림 종료 ACK 처리: userId={}", userId);
        // TODO: 스트림 종료 ACK 처리 로직
    }
    
    private void handleChatStreamEndNack(Long userId) {
        log.info("스트림 종료 NACK 처리: userId={}", userId);
        // TODO: 스트림 종료 NACK 처리 로직  
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
}
