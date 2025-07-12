package com.kakaobase.snsapp.domain.chat.service.streaming;

import com.kakaobase.snsapp.annotation.ServiceTest;
import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.dto.ai.response.AiStreamData;
import com.kakaobase.snsapp.domain.chat.event.StreamStartEvent;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.StreamException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.StreamErrorCode;
import com.kakaobase.snsapp.domain.chat.service.communication.ChatCommandService;
import com.kakaobase.snsapp.domain.chat.service.communication.ChatWebSocketService;
import com.kakaobase.snsapp.domain.chat.util.ChatEventType;
import com.kakaobase.snsapp.domain.chat.util.StreamIdGenerator;
import com.kakaobase.snsapp.fixture.chat.ChatFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * StreamingSessionManager 단위 테스트 (Mock 기반)
 * 
 * 테스트 대상:
 * - 스트리밍 세션 생성 및 관리
 * - 스트림 데이터 처리
 * - 스트림 완료 처리
 * - 스트림 에러 처리
 * - Mock 호출 검증 중심
 */
@ServiceTest
@DisplayName("StreamingSessionManager 단위 테스트")
class StreamingSessionManagerTest {
    
    @Mock
    private StreamIdGenerator streamIdGenerator;
    
    @Mock
    private ChatCommandService chatCommandService;
    
    @Mock
    private ChatWebSocketService chatWebSocketService;
    
    @Mock
    private ChatConverter chatConverter;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @InjectMocks
    private StreamingSessionManager streamingSessionManager;
    
    @Captor
    private ArgumentCaptor<StreamStartEvent> streamStartEventCaptor;
    
    // === 스트리밍 세션 시작 테스트 ===
    
    @Test
    @DisplayName("스트리밍 세션 시작 성공")
    void startStreaming_Success() {
        // given
        Long userId = 1L;
        String expectedStreamId = ChatFixture.generateTestStreamId();
        
        given(streamIdGenerator.generate()).willReturn(expectedStreamId);
        
        // when
        String actualStreamId = streamingSessionManager.startStreaming(userId);
        
        // then
        assertThat(actualStreamId).isEqualTo(expectedStreamId);
        verify(streamIdGenerator).generate();
    }
    
    @ParameterizedTest
    @ValueSource(longs = {1L, 100L, 999L, 12345L})
    @DisplayName("다양한 사용자 ID로 스트리밍 세션 시작")
    void startStreaming_VariousUserIds(Long userId) {
        // given
        String streamId = ChatFixture.generateTestStreamId();
        given(streamIdGenerator.generate()).willReturn(streamId);
        
        // when
        String actualStreamId = streamingSessionManager.startStreaming(userId);
        
        // then
        assertThat(actualStreamId).isEqualTo(streamId);
        verify(streamIdGenerator).generate();
    }
    
    @Test
    @DisplayName("동일 사용자 다중 스트리밍 세션 시작")
    void startStreaming_MultipleSessionsForSameUser_Success() {
        // given
        Long userId = 1L;
        String firstStreamId = ChatFixture.generateTestStreamId();
        String secondStreamId = ChatFixture.generateTestStreamId() + "-2";
        
        given(streamIdGenerator.generate())
            .willReturn(firstStreamId)
            .willReturn(secondStreamId);
        
        // when
        String firstActualStreamId = streamingSessionManager.startStreaming(userId);
        String secondActualStreamId = streamingSessionManager.startStreaming(userId);
        
        // then
        assertThat(firstActualStreamId).isEqualTo(firstStreamId);
        assertThat(secondActualStreamId).isEqualTo(secondStreamId);
        verify(streamIdGenerator, times(2)).generate();
    }
    
    // === 스트림 데이터 처리 테스트 ===
    
    @Test
    @DisplayName("스트림 데이터 처리 성공 - 첫 번째 데이터")
    void processStreamData_FirstData_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        String message = "안녕하세요! AI 봇입니다.";
        
        given(streamIdGenerator.generate()).willReturn(streamId);
        streamingSessionManager.startStreaming(userId);
        
        AiStreamData streamData = ChatFixture.createStreamData(streamId, message);
        
        // when
        streamingSessionManager.processStreamData(streamData);
        
        // then
        verify(chatWebSocketService).sendStreamDataToUser(userId, message);
        verify(eventPublisher).publishEvent(streamStartEventCaptor.capture());
        
        StreamStartEvent event = streamStartEventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getStreamId()).isEqualTo(streamId);
        assertThat(event.getFirstStreamData()).isEqualTo(message);
    }
    
    @Test
    @DisplayName("스트림 데이터 처리 성공 - 후속 데이터")
    void processStreamData_SubsequentData_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        String firstMessage = "안녕하세요!";
        String secondMessage = " 반갑습니다.";
        
        given(streamIdGenerator.generate()).willReturn(streamId);
        streamingSessionManager.startStreaming(userId);
        
        // 첫 번째 데이터 처리
        AiStreamData firstData = ChatFixture.createStreamData(streamId, firstMessage);
        streamingSessionManager.processStreamData(firstData);
        
        // when - 두 번째 데이터 처리
        AiStreamData secondData = ChatFixture.createStreamData(streamId, secondMessage);
        streamingSessionManager.processStreamData(secondData);
        
        // then
        verify(chatWebSocketService).sendStreamDataToUser(userId, firstMessage);
        verify(chatWebSocketService).sendStreamDataToUser(userId, secondMessage);
        verify(eventPublisher, times(1)).publishEvent(any(StreamStartEvent.class)); // 첫 번째만 이벤트 발행
    }
    
    @Test
    @DisplayName("스트림 데이터 처리 실패 - 세션 없음")
    void processStreamData_SessionNotFound_ThrowException() {
        // given
        String nonExistentStreamId = "non-existent-stream-id";
        AiStreamData streamData = ChatFixture.createStreamData(nonExistentStreamId, "테스트 메시지");
        
        // when & then
        assertThatThrownBy(() -> streamingSessionManager.processStreamData(streamData))
            .isInstanceOf(StreamException.class)
            .hasFieldOrPropertyWithValue("errorCode", StreamErrorCode.STREAM_SESSION_NOT_FOUND);
    }
    
    @Test
    @DisplayName("스트림 데이터 처리 - null 메시지")
    void processStreamData_NullMessage_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        
        given(streamIdGenerator.generate()).willReturn(streamId);
        streamingSessionManager.startStreaming(userId);
        
        AiStreamData streamData = ChatFixture.createStreamData(streamId, null);
        
        // when
        streamingSessionManager.processStreamData(streamData);
        
        // then
        verify(chatWebSocketService).sendStreamDataToUser(userId, null);
        verify(eventPublisher, never()).publishEvent(any(StreamStartEvent.class)); // null 메시지는 이벤트 발행 안함
    }
    
    // === 스트림 완료 처리 테스트 ===
    
    @Test
    @DisplayName("스트림 완료 처리 성공")
    void processStreamComplete_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        String message = "안녕하세요! 반갑습니다.";
        
        given(streamIdGenerator.generate()).willReturn(streamId);
        streamingSessionManager.startStreaming(userId);
        
        // 스트림 데이터 먼저 추가
        AiStreamData streamData = ChatFixture.createStreamData(streamId, message);
        streamingSessionManager.processStreamData(streamData);
        
        AiStreamData completeData = ChatFixture.createStreamCompleteData(streamId);
        
        // when
        streamingSessionManager.processStreamComplete(completeData);
        
        // then
        verify(chatCommandService).saveBotMessage(userId, message);
        verify(chatWebSocketService).sendStreamDataToUser(
            eq(userId), 
            eq(ChatEventType.CHAT_STREAM_END.getEvent()), 
            eq(completeData)
        );
    }
    
    @Test
    @DisplayName("스트림 완료 처리 - 빈 응답")
    void processStreamComplete_EmptyResponse_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        
        given(streamIdGenerator.generate()).willReturn(streamId);
        streamingSessionManager.startStreaming(userId);
        
        AiStreamData completeData = ChatFixture.createStreamCompleteData(streamId);
        
        // when
        streamingSessionManager.processStreamComplete(completeData);
        
        // then
        verify(chatCommandService, never()).saveBotMessage(any(), any()); // 빈 응답은 저장하지 않음
        verify(chatWebSocketService).sendStreamDataToUser(
            eq(userId), 
            eq(ChatEventType.CHAT_STREAM_END.getEvent()), 
            eq(completeData)
        );
    }
    
    @Test
    @DisplayName("스트림 완료 처리 실패 - 메시지 저장 실패")
    void processStreamComplete_SaveMessageFail_ThrowException() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        String message = "테스트 메시지";
        
        given(streamIdGenerator.generate()).willReturn(streamId);
        streamingSessionManager.startStreaming(userId);
        
        // 스트림 데이터 추가
        AiStreamData streamData = ChatFixture.createStreamData(streamId, message);
        streamingSessionManager.processStreamData(streamData);
        
        // 메시지 저장 시 예외 발생하도록 설정
        doThrow(new RuntimeException("DB 저장 실패"))
            .when(chatCommandService).saveBotMessage(userId, message);
        
        AiStreamData completeData = ChatFixture.createStreamCompleteData(streamId);
        
        // when & then
        assertThatThrownBy(() -> streamingSessionManager.processStreamComplete(completeData))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.MESSAGE_SAVE_FAIL);
    }
    
    @Test
    @DisplayName("스트림 완료 처리 - 세션 없음")
    void processStreamComplete_SessionNotFound_HandleGracefully() {
        // given
        String nonExistentStreamId = "non-existent-stream-id";
        AiStreamData completeData = ChatFixture.createStreamCompleteData(nonExistentStreamId);
        
        // when & then - 예외 발생하지 않고 정상 완료되어야 함
        assertThatCode(() -> streamingSessionManager.processStreamComplete(completeData))
            .doesNotThrowAnyException();
        
        verify(chatCommandService, never()).saveBotMessage(any(), any());
        verify(chatWebSocketService, never()).sendStreamDataToUser(any(), any(), any());
    }
    
    // === 스트림 에러 처리 테스트 ===
    
    @Test
    @DisplayName("스트림 에러 처리 성공")
    void processStreamError_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        String errorMessage = "AI 서버 에러가 발생했습니다.";
        
        given(streamIdGenerator.generate()).willReturn(streamId);
        streamingSessionManager.startStreaming(userId);
        
        AiStreamData errorData = ChatFixture.createStreamErrorData(streamId, errorMessage);
        
        // when
        streamingSessionManager.processStreamError(errorData);
        
        // then
        verify(chatWebSocketService).sendStreamErrorToUser(userId, StreamErrorCode.AI_SERVER_ERROR);
    }
    
    @Test
    @DisplayName("스트림 에러 처리 - null 에러 메시지")
    void processStreamError_NullErrorMessage_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        
        given(streamIdGenerator.generate()).willReturn(streamId);
        streamingSessionManager.startStreaming(userId);
        
        AiStreamData errorData = ChatFixture.createStreamErrorData(streamId, null);
        
        // when
        streamingSessionManager.processStreamError(errorData);
        
        // then
        verify(chatWebSocketService).sendStreamErrorToUser(userId, StreamErrorCode.AI_SERVER_ERROR);
    }
    
    @Test
    @DisplayName("스트림 에러 처리 - 세션 없음")
    void processStreamError_SessionNotFound_HandleGracefully() {
        // given
        String nonExistentStreamId = "non-existent-stream-id";
        AiStreamData errorData = ChatFixture.createStreamErrorData(nonExistentStreamId, "에러 메시지");
        
        // when & then - 예외 발생하지 않고 정상 완료되어야 함
        assertThatCode(() -> streamingSessionManager.processStreamError(errorData))
            .doesNotThrowAnyException();
        
        verify(chatWebSocketService, never()).sendStreamErrorToUser(any(), any());
    }
    
    // === 복합 시나리오 테스트 ===
    
    @Test
    @DisplayName("전체 스트리밍 플로우 시뮬레이션")
    void completeStreamingFlow_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        ChatFixture.StreamingScenario scenario = ChatFixture.createDefaultStreamingScenario(userId);
        
        given(streamIdGenerator.generate()).willReturn(streamId);
        
        // when - 1. 스트리밍 시작
        String actualStreamId = streamingSessionManager.startStreaming(userId);
        assertThat(actualStreamId).isEqualTo(streamId);
        
        // when - 2. 스트림 데이터 처리
        for (AiStreamData chunk : scenario.getChunks()) {
            AiStreamData data = ChatFixture.createStreamData(streamId, chunk.message());
            streamingSessionManager.processStreamData(data);
        }
        
        // when - 3. 스트림 완료
        AiStreamData completeData = ChatFixture.createStreamCompleteData(streamId);
        streamingSessionManager.processStreamComplete(completeData);
        
        // then
        verify(eventPublisher, times(1)).publishEvent(any(StreamStartEvent.class)); // 첫 번째 데이터만 이벤트 발행
        verify(chatWebSocketService, times(scenario.getChunkCount()))
            .sendStreamDataToUser(eq(userId), anyString()); // 모든 청크 전송
        verify(chatCommandService).saveBotMessage(eq(userId), anyString()); // 최종 메시지 저장
        verify(chatWebSocketService).sendStreamDataToUser(
            eq(userId), 
            eq(ChatEventType.CHAT_STREAM_END.getEvent()), 
            eq(completeData)
        ); // 완료 알림
    }
    
    @Test
    @DisplayName("스트리밍 에러 플로우 시뮬레이션")
    void errorStreamingFlow_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        
        given(streamIdGenerator.generate()).willReturn(streamId);
        
        // when - 1. 스트리밍 시작
        streamingSessionManager.startStreaming(userId);
        
        // when - 2. 일부 데이터 처리
        AiStreamData firstData = ChatFixture.createStreamData(streamId, "안녕하세요");
        streamingSessionManager.processStreamData(firstData);
        
        // when - 3. 에러 발생
        AiStreamData errorData = ChatFixture.createStreamErrorData(streamId, "AI 서버 에러");
        streamingSessionManager.processStreamError(errorData);
        
        // then
        verify(chatWebSocketService).sendStreamDataToUser(userId, "안녕하세요");
        verify(chatWebSocketService).sendStreamErrorToUser(userId, StreamErrorCode.AI_SERVER_ERROR);
        verify(chatCommandService, never()).saveBotMessage(any(), any()); // 에러로 인해 저장되지 않음
    }
    
    @Test
    @DisplayName("동시 다중 세션 관리 테스트")
    void concurrentMultipleSessionManagement_Success() {
        // given
        Long[] userIds = {1L, 2L, 3L, 4L, 5L};
        String[] streamIds = new String[userIds.length];
        
        for (int i = 0; i < userIds.length; i++) {
            streamIds[i] = ChatFixture.generateTestStreamId() + "-" + i;
            given(streamIdGenerator.generate()).willReturn(streamIds[i]);
        }
        
        // when - 모든 사용자 세션 시작
        for (int i = 0; i < userIds.length; i++) {
            String actualStreamId = streamingSessionManager.startStreaming(userIds[i]);
            assertThat(actualStreamId).isEqualTo(streamIds[i]);
        }
        
        // when - 일부 세션에 데이터 처리
        for (int i = 0; i < 3; i++) {
            AiStreamData data = ChatFixture.createStreamData(streamIds[i], "메시지 " + i);
            streamingSessionManager.processStreamData(data);
        }
        
        // when - 일부 세션 완료, 일부 세션 취소
        streamingSessionManager.processStreamComplete(ChatFixture.createStreamCompleteData(streamIds[0]));
        streamingSessionManager.processStreamComplete(ChatFixture.createStreamCompleteData(streamIds[1]));
        streamingSessionManager.cancelStreaming(streamIds[2]);
        
        // then
        verify(streamIdGenerator, times(userIds.length)).generate();
        verify(chatCommandService, times(2)).saveBotMessage(any(), any()); // 완료된 세션만 저장
    }
}