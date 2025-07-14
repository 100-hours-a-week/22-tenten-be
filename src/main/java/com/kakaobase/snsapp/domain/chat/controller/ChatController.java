package com.kakaobase.snsapp.domain.chat.controller;


import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.dto.SimpTimeData;
import com.kakaobase.snsapp.domain.chat.dto.request.ChatData;
import com.kakaobase.snsapp.domain.chat.dto.request.StreamAckData;
import com.kakaobase.snsapp.domain.chat.dto.request.StreamStopData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatErrorData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatList;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.StreamException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.service.ChatService;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacket;
import com.kakaobase.snsapp.global.common.response.CustomResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import java.security.Principal;
import org.springframework.messaging.simp.annotation.SendToUser;
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
    private final ChatConverter chatConveter;

    @GetMapping("/api/chat/bot")
    public CustomResponse<ChatList> getChatList(
            @Parameter(description = "한 번에 불러올 댓글 수 (기본값: 40)") @RequestParam(required = false, defaultValue = "40") Integer limit,
            @Parameter(description = "페이지네이션 커서 (이전 응답의 next_cursor)") @RequestParam(required = false) Long cursor,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        ChatList result = chatService.getChatMessages(userDetails, limit, cursor);

        return CustomResponse.success("채팅 조회에 성공하였습니다.", result);
    }


    @MessageMapping("/chat")
    public void handleChatSend(@Payload WebSocketPacket<?> packet, Principal principal) {
        String event = packet.event;
        Long userId;
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new ChatException(ChatErrorCode.CHAT_INVALID, null);
        }
        userId = Long.valueOf(userDetails.getId());
        
        log.info("채팅 이벤트 수신: event={}, userId={}", event, userId);
        
        switch (event) {
            case "chat.send" -> chatService.handleSendEvent(userId, (ChatData) packet.data);
            case "chat.typing" -> handleChatTyping((SimpTimeData) packet.data, userId);
            case "chat.stop" -> handleChatStop(userId, (StreamStopData) packet.data);
            case "chat.stream.end.ack" -> handleChatStreamEndAck(userId, (StreamAckData) packet.data);
            case "chat.stream.end.nack" -> handleChatStreamEndNack(userId, (StreamAckData) packet.data);
            default -> {
                log.warn("알 수 없는 채팅 이벤트: event={}, userId={}", event, userId);
                throw new ChatException(ChatErrorCode.CHAT_INVALID, userId);
            }
        }
    }


    @MessageExceptionHandler(ChatException.class)
    @SendToUser("/queue/chatbot")
    public WebSocketPacket<ChatErrorData> handleChatException(ChatException e, Principal principal) {
        return chatConveter.toErrorPacket(e);
    }

    @MessageExceptionHandler(ChatException.class)
    @SendToUser("/queue/chatbot")
    public WebSocketPacket<ChatErrorData> handleStreamException(StreamException e, Principal principal) {
        return chatConveter.toErrorPacket(e);
    }
    
    private void handleChatTyping(SimpTimeData data, Long userId) {
        log.info("타이핑 상태 처리: userId={}, timestamp={}", userId, data.timestamp());
        
        try {
            // 채팅 버퍼 활동 갱신 (TTL 연장)
            chatService.handleTypingEvent(userId);
            
            log.debug("채팅 버퍼 활동 갱신: userId={}", userId);
            
        } catch (Exception e) {
            log.error("타이핑 상태 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    private void handleChatStop(Long userId, StreamStopData data) {
        log.info("채팅 중지 처리: userId={}", userId);
        
        try {
            // 채팅 중지 처리 로직
            chatService.handleStopEvent(userId, data);
            
            log.debug("채팅 중지 처리 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.error("채팅 중지 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    private void handleChatStreamEndAck(Long userId, StreamAckData data) {
        log.info("스트림 종료 ACK 처리: userId={}", userId);
        
        try {
            // 스트림 종료 ACK 처리 로직
            chatService.handleStreamEndAck(userId, data);
            
            log.debug("스트림 종료 ACK 처리 완료: userId={}", userId);
            
        } catch (Exception e) {
            log.error("스트림 종료 ACK 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    private void handleChatStreamEndNack(Long userId, StreamAckData data) {
        log.info("스트림 종료 NACK 처리: userId={}", userId);
        
        try {
            // 스트림 종료 NACK 처리 로직
            chatService.handleStreamEndNack(userId, data);
            
        } catch (Exception e) {
            log.error("스트림 종료 NACK 처리 실패: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
}
