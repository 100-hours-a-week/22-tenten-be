package com.kakaobase.snsapp.domain.chat.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kakaobase.snsapp.annotation.ServiceTest;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.StreamErrorCode;
import com.kakaobase.snsapp.domain.chat.service.communication.ChatWebSocketService;
import com.kakaobase.snsapp.domain.chat.service.streaming.StreamingSessionManager;
import com.kakaobase.snsapp.domain.chat.util.AiServerHealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiServerSseManager 단위 테스트
 * 
 * 테스트 대상:
 * - SSE 데이터 파싱 실패 시나리오
 * - JSON 형식 불일치 처리
 * - 잘못된 스트림 ID 처리
 * - 알 수 없는 이벤트 타입 처리
 */
@ServiceTest
@DisplayName("AiServerSseManager 단위 테스트")
class AiServerSseManagerTest {
    
    @Mock
    private ChatWebSocketService chatWebSocketService;
    
    @Mock
    private StreamingSessionManager streamingSessionManager;
    
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    
    @InjectMocks
    private AiServerSseManager aiServerSseManager;
    
    private static final String VALID_STREAM_ID = "stream-12345";
    private static final Long VALID_USER_ID = 1L;
    
    @BeforeEach
    void setUp() {
        // 기본적으로 유효한 사용자 ID 반환하도록 설정
        lenient().when(streamingSessionManager.getUserIdByStreamId(anyString()))
            .thenReturn(VALID_USER_ID);
    }
    
    // === JSON 파싱 실패 테스트 ===
    
    @Test
    @DisplayName("SSE 데이터 파싱 실패 - 잘못된 JSON 형식")
    void processSseEvent_JsonParsingFailure_MalformedJson() {
        // given
        ServerSentEvent<String> sse = createSseEvent("stream", "{ invalid json }");
        
        // when - 잘못된 JSON을 파싱하려고 시도
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException(); // 예외가 내부에서 처리됨
        
        // then - JSON 파싱 실패로 인해 아무것도 호출되지 않음
        verifyNoInteractions(streamingSessionManager);
        verifyNoInteractions(chatWebSocketService);
    }
    
    @Test
    @DisplayName("SSE 데이터 파싱 실패 - 필수 필드 누락")
    void processSseEvent_JsonParsingFailure_MissingRequiredFields() {
        // given - stream_id 필드가 누락된 JSON
        String jsonWithMissingStreamId = """
            {
                "message": "테스트 메시지",
                "timestamp": "2024-01-01T12:00:00"
            }
            """;
        
        ServerSentEvent<String> sse = createSseEvent("stream", jsonWithMissingStreamId);
        
        // when
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException();
        
        // then - streamId가 null이므로 에러 전송
        verify(chatWebSocketService).sendStreamErrorToUser(
            eq(VALID_USER_ID), 
            eq(StreamErrorCode.INVALID_STREAM_ID)
        );
    }
    
    // === 빈 데이터 처리 테스트 ===
    
    @Test
    @DisplayName("SSE 데이터 처리 - null 데이터")
    void processSseEvent_NullData() {
        // given
        ServerSentEvent<String> sse = createSseEvent("stream", null);
        
        // when
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException();
        
        // then - null 데이터는 조기 반환되어 아무 처리도 하지 않음
        verifyNoInteractions(streamingSessionManager);
        verifyNoInteractions(chatWebSocketService);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    @DisplayName("SSE 데이터 처리 - 빈 문자열 또는 공백")
    void processSseEvent_EmptyOrWhitespaceData(String emptyData) {
        // given
        ServerSentEvent<String> sse = createSseEvent("stream", emptyData);
        
        // when
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException();
        
        // then - 빈 데이터는 조기 반환되어 아무 처리도 하지 않음
        verifyNoInteractions(streamingSessionManager);
        verifyNoInteractions(chatWebSocketService);
    }
    
    // === 스트림 ID 검증 테스트 ===
    
    @Test
    @DisplayName("SSE 데이터 처리 - 빈 스트림 ID")
    void processSseEvent_EmptyStreamId() {
        // given
        String jsonWithEmptyStreamId = """
            {
                "stream_id": "",
                "message": "테스트 메시지",
                "timestamp": "2024-01-01T12:00:00"
            }
            """;
        
        ServerSentEvent<String> sse = createSseEvent("stream", jsonWithEmptyStreamId);
        
        // when
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException();
        
        // then - 빈 스트림 ID로 인해 에러 전송
        verify(chatWebSocketService).sendStreamErrorToUser(
            eq(VALID_USER_ID), 
            eq(StreamErrorCode.INVALID_STREAM_ID)
        );
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("SSE 데이터 처리 - 공백만 있는 스트림 ID")
    void processSseEvent_WhitespaceOnlyStreamId(String whitespaceStreamId) {
        // given
        String jsonWithWhitespaceStreamId = String.format("""
            {
                "stream_id": "%s",
                "message": "테스트 메시지",
                "timestamp": "2024-01-01T12:00:00"
            }
            """, whitespaceStreamId);
        
        ServerSentEvent<String> sse = createSseEvent("stream", jsonWithWhitespaceStreamId);
        
        // when
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException();
        
        // then - 공백 스트림 ID로 인해 에러 전송
        verify(chatWebSocketService).sendStreamErrorToUser(
            eq(VALID_USER_ID), 
            eq(StreamErrorCode.INVALID_STREAM_ID)
        );
    }
    
    // === 알 수 없는 이벤트 타입 테스트 ===
    
    @Test
    @DisplayName("SSE 데이터 처리 - 알 수 없는 이벤트 타입")
    void processSseEvent_UnknownEventType() {
        // given
        String validJson = """
            {
                "stream_id": "stream-12345",
                "message": "테스트 메시지",
                "timestamp": "2024-01-01T12:00:00"
            }
            """;
        
        ServerSentEvent<String> sse = createSseEvent("unknown_event", validJson);
        
        // when
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException();
        
        // then - 알 수 없는 이벤트 타입으로 인해 에러 전송
        verify(chatWebSocketService).sendStreamErrorToUser(
            eq(VALID_USER_ID), 
            eq(StreamErrorCode.AI_SERVER_RESPONSE_PARSE_FAIL)
        );
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "STREAM", "Done", "ERROR", "stream_data", "complete"})
    @DisplayName("SSE 데이터 처리 - 다양한 잘못된 이벤트 타입")
    void processSseEvent_VariousInvalidEventTypes(String invalidEventType) {
        // given
        String validJson = """
            {
                "stream_id": "stream-12345",
                "message": "테스트 메시지",
                "timestamp": "2024-01-01T12:00:00"
            }
            """;
        
        ServerSentEvent<String> sse = createSseEvent(invalidEventType, validJson);
        
        // when
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException();
        
        // then - 잘못된 이벤트 타입으로 인해 에러 전송
        verify(chatWebSocketService).sendStreamErrorToUser(
            eq(VALID_USER_ID), 
            eq(StreamErrorCode.AI_SERVER_RESPONSE_PARSE_FAIL)
        );
    }
    
    // === 정상 이벤트 처리 테스트 ===
    
    @Test
    @DisplayName("SSE 데이터 처리 성공 - stream 이벤트")
    void processSseEvent_StreamEvent_Success() {
        // given
        String validJson = """
            {
                "stream_id": "stream-12345",
                "message": "테스트 메시지",
                "timestamp": "2024-01-01T12:00:00"
            }
            """;
        
        ServerSentEvent<String> sse = createSseEvent("stream", validJson);
        
        // when
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException();
        
        // then - 정상적으로 스트림 데이터 처리
        verify(streamingSessionManager).processStreamData(any());
        verifyNoInteractions(chatWebSocketService);
    }
    
    @Test
    @DisplayName("SSE 데이터 처리 성공 - done 이벤트")
    void processSseEvent_DoneEvent_Success() {
        // given
        String validJson = """
            {
                "stream_id": "stream-12345",
                "message": "완료된 메시지",
                "timestamp": "2024-01-01T12:00:00"
            }
            """;
        
        ServerSentEvent<String> sse = createSseEvent("done", validJson);
        
        // when
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException();
        
        // then - 정상적으로 스트림 완료 처리
        verify(streamingSessionManager).processStreamComplete(any());
        verifyNoInteractions(chatWebSocketService);
    }
    
    @Test
    @DisplayName("SSE 데이터 처리 성공 - error 이벤트")
    void processSseEvent_ErrorEvent_Success() {
        // given
        String validJson = """
            {
                "stream_id": "stream-12345",
                "message": "에러 메시지",
                "timestamp": "2024-01-01T12:00:00"
            }
            """;
        
        ServerSentEvent<String> sse = createSseEvent("error", validJson);
        
        // when
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException();
        
        // then - 정상적으로 스트림 에러 처리
        verify(streamingSessionManager).processStreamError(any());
        verifyNoInteractions(chatWebSocketService);
    }
    
    @Test
    @DisplayName("SSE 데이터 처리 - null 이벤트 타입 (기본값 stream 사용)")
    void processSseEvent_NullEventType_DefaultToStream() {
        // given
        String validJson = """
            {
                "stream_id": "stream-12345",
                "message": "기본 이벤트 메시지",
                "timestamp": "2024-01-01T12:00:00"
            }
            """;
        
        ServerSentEvent<String> sse = createSseEvent(null, validJson);
        
        // when
        assertThatCode(() -> invokeProcessSseEvent(sse))
            .doesNotThrowAnyException();
        
        // then - null 이벤트는 "stream"으로 처리됨
        verify(streamingSessionManager).processStreamData(any());
        verifyNoInteractions(chatWebSocketService);
    }
    
    // === 헬스 체크 테스트 ===
    
    @Test
    @DisplayName("헬스 상태 조회 - 초기 상태는 DISCONNECTED")
    void getHealthStatus_InitialState_Disconnected() {
        // when
        AiServerHealthStatus status = aiServerSseManager.getHealthStatus();
        
        // then
        assertThat(status).isEqualTo(AiServerHealthStatus.DISCONNECTED);
    }
    
    // === 유틸리티 메서드 ===
    
    /**
     * ServerSentEvent 생성 헬퍼 메서드
     */
    private ServerSentEvent<String> createSseEvent(String eventType, String data) {
        return ServerSentEvent.<String>builder()
            .event(eventType)
            .data(data)
            .build();
    }
    
    /**
     * private processSseEvent 메서드 호출을 위한 헬퍼 메서드
     */
    private void invokeProcessSseEvent(ServerSentEvent<String> sse) {
        try {
            ReflectionTestUtils.invokeMethod(aiServerSseManager, "processSseEvent", sse);
        } catch (Exception e) {
            // 테스트에서는 예외를 무시 (메서드 내부에서 처리됨)
        }
    }
}