package com.kakaobase.snsapp.domain.chat.service.communication;

import com.kakaobase.snsapp.annotation.ServiceTest;
import com.kakaobase.snsapp.config.ChatTestConfig;
import com.kakaobase.snsapp.domain.chat.dto.SimpTimeData;
import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiStreamData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatErrorData;
import com.kakaobase.snsapp.domain.chat.dto.response.StreamData;
import com.kakaobase.snsapp.domain.chat.dto.response.StreamStartData;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.StreamErrorCode;
import com.kakaobase.snsapp.domain.chat.util.ChatEventType;
import com.kakaobase.snsapp.fixture.chat.ChatFixture;
import com.kakaobase.snsapp.global.common.entity.WebSocketPacketImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * ChatWebSocketService 단위 테스트
 * 
 * 테스트 대상:
 * - WebSocket 메시지 전송 로직
 * - 로딩 상태 알림
 * - 스트림 시작/진행/종료 알림
 * - 에러 메시지 전송
 * - 이벤트 타입별 패킷 생성
 */
@ServiceTest
@Import(ChatTestConfig.class)
@DisplayName("ChatWebSocketService 단위 테스트")
class ChatWebSocketServiceTest {
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @InjectMocks
    private ChatWebSocketService chatWebSocketService;
    
    @Captor
    private ArgumentCaptor<WebSocketPacketImpl<?>> packetCaptor;
    
    private static final String CHAT_QUEUE_DESTINATION = "/queue/chat";
    
    // === 로딩 상태 전송 테스트 ===
    
    @Test
    @DisplayName("로딩 상태 전송 성공")
    void sendLoadingToUser_Success() {
        // given
        Long userId = 1L;
        
        // when
        chatWebSocketService.sendLoadingToUser(userId);
        
        // then
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        WebSocketPacketImpl<?> packet = packetCaptor.getValue();
        assertThat(packet.event).isEqualTo(ChatEventType.CHAT_STREAM_LOADING.getEvent());
        assertThat(packet.data).isInstanceOf(SimpTimeData.class);
    }
    
    @Test
    @DisplayName("로딩 상태 전송 - 다양한 사용자 ID")
    void sendLoadingToUser_VariousUserIds() {
        // given
        Long[] userIds = {1L, 999L, 12345L};
        
        for (Long userId : userIds) {
            // when
            chatWebSocketService.sendLoadingToUser(userId);
            
            // then
            verify(messagingTemplate).convertAndSendToUser(
                eq(userId.toString()),
                eq(CHAT_QUEUE_DESTINATION),
                packetCaptor.capture()
            );
            
            WebSocketPacketImpl<?> packet = packetCaptor.getValue();
            assertThat(packet.event).isEqualTo(ChatEventType.CHAT_STREAM_LOADING.getEvent());
        }
    }
    
    // === 스트림 시작 알림 테스트 ===
    
    @Test
    @DisplayName("스트림 시작 알림 전송 성공")
    void sendStreamStartToUser_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        
        // when
        chatWebSocketService.sendStreamStartToUser(userId, streamId);
        
        // then
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        WebSocketPacketImpl<?> packet = packetCaptor.getValue();
        assertThat(packet.event).isEqualTo(ChatEventType.CHAT_STREAM_START.getEvent());
        assertThat(packet.data).isInstanceOf(StreamStartData.class);
        
        StreamStartData data = (StreamStartData) packet.data;
        assertThat(data.streamId()).isEqualTo(streamId);
        assertThat(data.timestamp()).isNotNull();
    }
    
    @Test
    @DisplayName("스트림 시작 알림 - 긴 스트림 ID")
    void sendStreamStartToUser_LongStreamId() {
        // given
        Long userId = 1L;
        String longStreamId = "very-long-stream-id-" + ChatFixture.generateRandomString(50);
        
        // when
        chatWebSocketService.sendStreamStartToUser(userId, longStreamId);
        
        // then
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        WebSocketPacketImpl<?> packet = packetCaptor.getValue();
        StreamStartData data = (StreamStartData) packet.data;
        assertThat(data.streamId()).isEqualTo(longStreamId);
    }
    
    // === 스트림 데이터 전송 테스트 ===
    
    @Test
    @DisplayName("스트림 데이터 전송 성공 - 일반 텍스트")
    void sendStreamDataToUser_Success() {
        // given
        Long userId = 1L;
        String content = "안녕하세요! AI 봇입니다.";
        
        // when
        chatWebSocketService.sendStreamDataToUser(userId, content);
        
        // then
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        WebSocketPacketImpl<?> packet = packetCaptor.getValue();
        assertThat(packet.event).isEqualTo(ChatEventType.CHAT_STREAM.getEvent());
        assertThat(packet.data).isInstanceOf(StreamData.class);
        
        StreamData data = (StreamData) packet.data;
        assertThat(data.content()).isEqualTo(content);
        assertThat(data.timestamp()).isNotNull();
    }
    
    @Test
    @DisplayName("스트림 데이터 전송 - 빈 문자열")
    void sendStreamDataToUser_EmptyContent() {
        // given
        Long userId = 1L;
        String content = "";
        
        // when
        chatWebSocketService.sendStreamDataToUser(userId, content);
        
        // then
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        WebSocketPacketImpl<?> packet = packetCaptor.getValue();
        StreamData data = (StreamData) packet.data;
        assertThat(data.content()).isEqualTo(content);
    }
    
    @Test
    @DisplayName("스트림 데이터 전송 - null 콘텐츠")
    void sendStreamDataToUser_NullContent() {
        // given
        Long userId = 1L;
        String content = null;
        
        // when
        chatWebSocketService.sendStreamDataToUser(userId, content);
        
        // then
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        WebSocketPacketImpl<?> packet = packetCaptor.getValue();
        StreamData data = (StreamData) packet.data;
        assertThat(data.content()).isNull();
    }
    
    @Test
    @DisplayName("스트림 데이터 전송 - 스트리밍 시나리오")
    void sendStreamDataToUser_StreamingScenario() {
        // given
        Long userId = 1L;
        ChatFixture.StreamingScenario scenario = ChatFixture.createDefaultStreamingScenario(userId);
        
        // when - 각 청크별로 전송
        for (AiStreamData chunk : scenario.getChunks()) {
            chatWebSocketService.sendStreamDataToUser(userId, chunk.message());
        }
        
        // then - 청크 수만큼 전송 검증
        verify(messagingTemplate, org.mockito.Mockito.times(scenario.getChunkCount()))
            .convertAndSendToUser(
                eq(userId.toString()),
                eq(CHAT_QUEUE_DESTINATION),
                packetCaptor.capture()
            );
    }
    
    // === 커스텀 이벤트 스트림 데이터 전송 테스트 ===
    
    @Test
    @DisplayName("커스텀 이벤트 스트림 데이터 전송 성공")
    void sendStreamDataToUser_CustomEvent_Success() {
        // given
        Long userId = 1L;
        String customEventType = "CUSTOM_STREAM_EVENT";
        AiStreamData streamData = ChatFixture.createDefaultStreamingScenario(userId).getFirstChunk();
        
        // when
        chatWebSocketService.sendStreamDataToUser(userId, customEventType, streamData);
        
        // then
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        WebSocketPacketImpl<?> packet = packetCaptor.getValue();
        assertThat(packet.event).isEqualTo(customEventType);
        assertThat(packet.data).isInstanceOf(AiStreamData.class);
        assertThat(packet.data).isEqualTo(streamData);
    }
    
    @Test
    @DisplayName("커스텀 이벤트 스트림 데이터 전송 - 스트림 종료 이벤트")
    void sendStreamDataToUser_StreamEndEvent() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        AiStreamData completeData = ChatFixture.createStreamCompleteData(streamId);
        
        // when
        chatWebSocketService.sendStreamDataToUser(
            userId, 
            ChatEventType.CHAT_STREAM_END.getEvent(), 
            completeData
        );
        
        // then
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        WebSocketPacketImpl<?> packet = packetCaptor.getValue();
        assertThat(packet.event).isEqualTo(ChatEventType.CHAT_STREAM_END.getEvent());
    }
    
    // === 스트림 종료 알림 테스트 ===
    
    @Test
    @DisplayName("스트림 종료 알림 전송 성공")
    void sendStreamEndToUser_Success() {
        // given
        Long userId = 1L;
        
        // when
        chatWebSocketService.sendStreamEndToUser(userId);
        
        // then
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        WebSocketPacketImpl<?> packet = packetCaptor.getValue();
        assertThat(packet.event).isEqualTo(ChatEventType.CHAT_STREAM_END.getEvent());
        assertThat(packet.data).isInstanceOf(SimpTimeData.class);
    }
    
    // === 채팅 에러 전송 테스트 ===
    
    @Test
    @DisplayName("채팅 에러 메시지 전송 성공")
    void sendChatErrorToUser_Success() {
        // given
        Long userId = 1L;
        ChatErrorCode errorCode = ChatErrorCode.AI_SERVER_CONNECTION_FAIL;
        
        // when
        chatWebSocketService.sendChatErrorToUser(userId, errorCode);
        
        // then
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        WebSocketPacketImpl<?> packet = packetCaptor.getValue();
        assertThat(packet.event).isEqualTo(ChatEventType.CHAT_STREAM_ERROR.getEvent());
        assertThat(packet.data).isInstanceOf(ChatErrorData.class);
        
        ChatErrorData data = (ChatErrorData) packet.data;
        assertThat(data.error()).isEqualTo(errorCode.getError());
        assertThat(data.message()).isEqualTo(errorCode.getMessage());
        assertThat(data.timestamp()).isNotNull();
    }
    
    @Test
    @DisplayName("채팅 에러 메시지 전송 - 다양한 에러 코드")
    void sendChatErrorToUser_VariousErrorCodes() {
        // given
        Long userId = 1L;
        ChatErrorCode[] errorCodes = {
            ChatErrorCode.AI_SERVER_CONNECTION_FAIL,
            ChatErrorCode.AI_SERVER_TIMEOUT,
            ChatErrorCode.MESSAGE_SAVE_FAIL,
            ChatErrorCode.CHAT_INVALID
        };
        
        // when - 모든 에러 코드로 메서드 호출
        for (ChatErrorCode errorCode : errorCodes) {
            chatWebSocketService.sendChatErrorToUser(userId, errorCode);
        }
        
        // then - 전체 호출 수 검증
        verify(messagingTemplate, times(errorCodes.length)).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        // 각 호출별 데이터 검증
        List<WebSocketPacketImpl<?>> capturedPackets = packetCaptor.getAllValues();
        for (int i = 0; i < errorCodes.length; i++) {
            WebSocketPacketImpl<?> packet = capturedPackets.get(i);
            ChatErrorData data = (ChatErrorData) packet.data;
            assertThat(data.error()).isEqualTo(errorCodes[i].getError());
            assertThat(data.message()).isEqualTo(errorCodes[i].getMessage());
        }
    }
    
    // === 스트림 에러 전송 테스트 ===
    
    @Test
    @DisplayName("스트림 에러 메시지 전송 성공")
    void sendStreamErrorToUser_Success() {
        // given
        Long userId = 1L;
        StreamErrorCode errorCode = StreamErrorCode.AI_SERVER_ERROR;
        
        // when
        chatWebSocketService.sendStreamErrorToUser(userId, errorCode);
        
        // then
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        WebSocketPacketImpl<?> packet = packetCaptor.getValue();
        assertThat(packet.event).isEqualTo(ChatEventType.CHAT_STREAM_ERROR.getEvent());
        assertThat(packet.data).isInstanceOf(ChatErrorData.class);
        
        ChatErrorData data = (ChatErrorData) packet.data;
        assertThat(data.error()).isEqualTo(errorCode.getError());
        assertThat(data.message()).isEqualTo(errorCode.getMessage());
        assertThat(data.timestamp()).isNotNull();
    }
    
    @Test
    @DisplayName("스트림 에러 메시지 전송 - 다양한 에러 코드")
    void sendStreamErrorToUser_VariousErrorCodes() {
        // given
        Long userId = 1L;
        StreamErrorCode[] errorCodes = {
            StreamErrorCode.AI_SERVER_ERROR,
            StreamErrorCode.STREAM_SESSION_NOT_FOUND,
            StreamErrorCode.AI_SERVER_MESSAGE_SEND_FAIL
        };
        
        // when - 모든 에러 코드로 메서드 호출
        for (StreamErrorCode errorCode : errorCodes) {
            chatWebSocketService.sendStreamErrorToUser(userId, errorCode);
        }
        
        // then - 전체 호출 수 검증
        verify(messagingTemplate, times(errorCodes.length)).convertAndSendToUser(
            eq(userId.toString()),
            eq(CHAT_QUEUE_DESTINATION),
            packetCaptor.capture()
        );
        
        // 각 호출별 데이터 검증
        List<WebSocketPacketImpl<?>> capturedPackets = packetCaptor.getAllValues();
        for (int i = 0; i < errorCodes.length; i++) {
            WebSocketPacketImpl<?> packet = capturedPackets.get(i);
            ChatErrorData data = (ChatErrorData) packet.data;
            assertThat(data.error()).isEqualTo(errorCodes[i].getError());
            assertThat(data.message()).isEqualTo(errorCodes[i].getMessage());
        }
    }
    
    // === 복합 시나리오 테스트 ===
    
    @Test
    @DisplayName("전체 스트리밍 플로우 시뮬레이션")
    void completeStreamingFlow_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        ChatFixture.StreamingScenario scenario = ChatFixture.createDefaultStreamingScenario(userId);
        
        // when - 1. 로딩 시작
        chatWebSocketService.sendLoadingToUser(userId);
        
        // when - 2. 스트림 시작
        chatWebSocketService.sendStreamStartToUser(userId, streamId);
        
        // when - 3. 스트림 데이터 전송
        for (AiStreamData chunk : scenario.getChunks()) {
            chatWebSocketService.sendStreamDataToUser(userId, chunk.message());
        }
        
        // when - 4. 스트림 종료
        chatWebSocketService.sendStreamEndToUser(userId);
        
        // then - 총 메시지 수 검증 (로딩 + 시작 + 청크들 + 종료)
        int expectedMessageCount = 1 + 1 + scenario.getChunkCount() + 1;
        verify(messagingTemplate, org.mockito.Mockito.times(expectedMessageCount))
            .convertAndSendToUser(
                eq(userId.toString()),
                eq(CHAT_QUEUE_DESTINATION),
                org.mockito.Mockito.any()
            );
    }
    
    @Test
    @DisplayName("에러 발생 스트리밍 플로우 시뮬레이션")
    void errorStreamingFlow_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        
        // when - 1. 로딩 시작
        chatWebSocketService.sendLoadingToUser(userId);
        
        // when - 2. 스트림 시작
        chatWebSocketService.sendStreamStartToUser(userId, streamId);
        
        // when - 3. 일부 데이터 전송 후 에러 발생
        chatWebSocketService.sendStreamDataToUser(userId, "안녕하세요");
        chatWebSocketService.sendChatErrorToUser(userId, ChatErrorCode.AI_SERVER_CONNECTION_FAIL);
        
        // then - 총 4개 메시지 전송 검증
        verify(messagingTemplate, org.mockito.Mockito.times(4))
            .convertAndSendToUser(
                eq(userId.toString()),
                eq(CHAT_QUEUE_DESTINATION),
                org.mockito.Mockito.any()
            );
    }
}