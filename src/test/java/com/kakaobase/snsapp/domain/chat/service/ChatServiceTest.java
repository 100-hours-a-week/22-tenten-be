package com.kakaobase.snsapp.domain.chat.service;

import com.kakaobase.snsapp.annotation.ServiceTest;
import com.kakaobase.snsapp.config.ChatTestConfig;
import com.kakaobase.snsapp.domain.auth.principal.CustomUserDetails;
import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.dto.SimpTimeData;
import com.kakaobase.snsapp.domain.chat.dto.request.ChatData;
import com.kakaobase.snsapp.domain.chat.dto.request.StreamStopData;
import com.kakaobase.snsapp.domain.chat.dto.response.ChatList;
import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.repository.ChatMessageRepository;
import com.kakaobase.snsapp.domain.chat.repository.ChatRoomMemberRepository;
import com.kakaobase.snsapp.domain.chat.service.ai.AiServerHttpClient;
import com.kakaobase.snsapp.domain.chat.service.ai.AiServerSseManager;
import com.kakaobase.snsapp.domain.chat.service.communication.ChatCommandService;
import com.kakaobase.snsapp.domain.chat.service.streaming.ChatBufferManager;
import com.kakaobase.snsapp.domain.chat.service.streaming.StreamingSessionManager;
import com.kakaobase.snsapp.domain.chat.util.ChatBufferCacheUtil;
import com.kakaobase.snsapp.domain.chat.util.AiServerHealthStatus;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.fixture.chat.ChatFixture;
import com.kakaobase.snsapp.fixture.members.MemberFixture;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * ChatService 단위 테스트
 * 
 * 테스트 대상:
 * - 채팅 메시지 조회 로직
 * - 타이핑 이벤트 처리
 * - 메시지 전송 이벤트 처리
 * - 스트리밍 중단 처리
 * - 스트림 종료 처리
 * - 예외 상황 처리
 */
@ServiceTest
@Import(ChatTestConfig.class)
@DisplayName("ChatService 단위 테스트")
class ChatServiceTest {
    
    @Mock
    private ChatMessageRepository chatMessageRepository;
    
    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    
    @Mock
    private ChatConverter chatConverter;
    
    @Mock
    private ChatCommandService chatCommandService;
    
    @Mock
    private ChatBufferCacheUtil cacheUtil;
    
    @Mock
    private StreamingSessionManager streamingSessionManager;
    
    @Mock
    private ChatBufferManager chatBufferManager;
    
    @Mock
    private AiServerSseManager aiServerSseManager;
    
    @Mock
    private AiServerHttpClient aiServerHttpClient;
    
    @Mock
    private EntityManager entityManager;
    
    @InjectMocks
    private ChatService chatService;
    
    // === 채팅 메시지 조회 테스트 ===
    
    @Test
    @DisplayName("채팅 메시지 조회 성공 - 기존 채팅방이 있는 경우")
    void getChatMessages_ExistingChatRoom_Success() {
        // given
        CustomUserDetails userDetails = createUserDetails("testuser");
        Long userId = 1L;
        Integer limit = 40;
        Long cursor = null;
        
        ChatFixture.ChatScenario scenario = ChatFixture.createCompleteChatScenario("testuser");
        List<ChatMessage> messages = scenario.getMessages();
        
        given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(userId, BotConstants.BOT_MEMBER_ID))
            .willReturn(true);
        given(chatMessageRepository.findMessagesByChatRoomId(userId, limit, cursor))
            .willReturn(ChatFixture.createChatList(messages, false));
        
        // when
        ChatList result = chatService.getChatMessages(userDetails, limit, cursor);
        
        // then
        assertThat(result.chats()).hasSize(6); // 3 사용자 + 3 봇 메시지
        assertThat(result.hasNext()).isFalse();
        verify(chatCommandService, never()).createBotChatRoom(anyLong());
    }
    
    @Test
    @DisplayName("채팅 메시지 조회 성공 - 새 채팅방 생성")
    void getChatMessages_NewChatRoom_Success() {
        // given
        CustomUserDetails userDetails = createUserDetails("newuser");
        Long userId = 1L;
        Integer limit = 40;
        Long cursor = null;
        
        given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(userId, BotConstants.BOT_MEMBER_ID))
            .willReturn(false);
        given(chatCommandService.createBotChatRoom(userId)).willReturn(userId);
        
        // when
        ChatList result = chatService.getChatMessages(userDetails, limit, cursor);
        
        // then
        assertThat(result.chats()).isNull();
        assertThat(result.hasNext()).isFalse();
        verify(chatCommandService).createBotChatRoom(userId);
    }
    
    @Test
    @DisplayName("채팅 메시지 조회 성공 - 커서 기반 페이징")
    void getChatMessages_WithCursor_Success() {
        // given
        CustomUserDetails userDetails = createUserDetails("testuser");
        Long userId = 1L;
        Integer limit = 20;
        Long cursor = 100L;
        
        ChatFixture.ChatScenario scenario = ChatFixture.createCompleteChatScenario("testuser");
        List<ChatMessage> messages = scenario.getMessages().subList(0, 3); // 일부만 반환
        
        given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(userId, BotConstants.BOT_MEMBER_ID))
            .willReturn(true);
        given(chatMessageRepository.findMessagesByChatRoomId(userId, limit, cursor))
            .willReturn(ChatFixture.createChatList(messages, true));
        
        // when
        ChatList result = chatService.getChatMessages(userDetails, limit, cursor);
        
        // then
        assertThat(result.chats()).hasSize(3);
        assertThat(result.hasNext()).isTrue();
    }
    
    // === 타이핑 이벤트 처리 테스트 ===
    
    @Test
    @DisplayName("타이핑 이벤트 처리 성공")
    void handleTypingEvent_Success() {
        // given
        Long userId = 1L;
        
        doNothing().when(cacheUtil).extendTTL(userId);
        doNothing().when(chatBufferManager).resetTimer(userId);
        
        // when & then
        assertThatCode(() -> chatService.handleTypingEvent(userId))
            .doesNotThrowAnyException();
        
        verify(cacheUtil).extendTTL(userId);
        verify(chatBufferManager).resetTimer(userId);
    }
    
    @Test
    @DisplayName("타이핑 이벤트 처리 실패 - 캐시 TTL 연장 실패")
    void handleTypingEvent_Failure_CacheExtendFail() {
        // given
        Long userId = 1L;
        
        doThrow(new RuntimeException("Redis 연결 실패"))
            .when(cacheUtil).extendTTL(userId);
        
        // when & then
        assertThatThrownBy(() -> chatService.handleTypingEvent(userId))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_BUFFER_EXTEND_FAIL);
    }
    
    @Test
    @DisplayName("타이핑 이벤트 처리 실패 - 타이머 리셋 실패")
    void handleTypingEvent_Failure_TimerResetFail() {
        // given
        Long userId = 1L;
        
        doNothing().when(cacheUtil).extendTTL(userId);
        doThrow(new RuntimeException("타이머 관리 실패"))
            .when(chatBufferManager).resetTimer(userId);
        
        // when & then
        assertThatThrownBy(() -> chatService.handleTypingEvent(userId))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_BUFFER_EXTEND_FAIL);
    }
    
    // === 메시지 전송 이벤트 처리 테스트 ===
    
    @Test
    @DisplayName("메시지 전송 이벤트 처리 성공")
    void handleSendEvent_Success() {
        // given
        Long userId = 1L;
        ChatData chatData = ChatFixture.createChatData("안녕하세요!");
        AiServerHealthStatus healthStatus = AiServerHealthStatus.CONNECTED;
        
        doNothing().when(chatCommandService).saveChatMessage(userId, chatData.content());
        given(aiServerSseManager.getHealthStatus()).willReturn(healthStatus);
        doNothing().when(cacheUtil).appendMessage(userId, chatData.content());
        doNothing().when(chatBufferManager).resetTimer(userId);
        
        // when & then
        assertThatCode(() -> chatService.handleSendEvent(userId, chatData))
            .doesNotThrowAnyException();
        
        verify(chatCommandService).saveChatMessage(userId, chatData.content());
        verify(cacheUtil).appendMessage(userId, chatData.content());
        verify(chatBufferManager).resetTimer(userId);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n\t"})
    @DisplayName("메시지 전송 이벤트 처리 실패 - 빈 메시지")
    void handleSendEvent_Failure_EmptyMessage(String emptyContent) {
        // given
        Long userId = 1L;
        ChatData chatData = ChatFixture.createChatData(emptyContent);
        
        // when & then
        assertThatThrownBy(() -> chatService.handleSendEvent(userId, chatData))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_INVALID);
        
        verify(chatCommandService, never()).saveChatMessage(anyLong(), any());
    }
    
    @Test
    @DisplayName("메시지 전송 이벤트 처리 실패 - null 메시지")
    void handleSendEvent_Failure_NullMessage() {
        // given
        Long userId = 1L;
        ChatData chatData = ChatFixture.createChatData(null);
        
        // when & then
        assertThatThrownBy(() -> chatService.handleSendEvent(userId, chatData))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_INVALID);
        
        verify(chatCommandService, never()).saveChatMessage(anyLong(), any());
    }
    
    @Test
    @DisplayName("메시지 전송 이벤트 처리 실패 - AI 서버 연결 불량")
    void handleSendEvent_Failure_AiServerDisconnected() {
        // given
        Long userId = 1L;
        ChatData chatData = ChatFixture.createChatData("테스트 메시지");
        AiServerHealthStatus disconnectedStatus = AiServerHealthStatus.DISCONNECTED;
        
        doNothing().when(chatCommandService).saveChatMessage(userId, chatData.content());
        given(aiServerSseManager.getHealthStatus()).willReturn(disconnectedStatus);
        
        // when & then
        assertThatThrownBy(() -> chatService.handleSendEvent(userId, chatData))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.AI_SERVER_CONNECTION_FAIL);
        
        verify(chatCommandService).saveChatMessage(userId, chatData.content());
        verify(cacheUtil, never()).appendMessage(anyLong(), any());
    }
    
    @Test
    @DisplayName("메시지 전송 이벤트 처리 실패 - 버퍼 추가 실패")
    void handleSendEvent_Failure_BufferAppendFail() {
        // given
        Long userId = 1L;
        ChatData chatData = ChatFixture.createChatData("테스트 메시지");
        AiServerHealthStatus healthStatus = AiServerHealthStatus.CONNECTED;
        
        doNothing().when(chatCommandService).saveChatMessage(userId, chatData.content());
        given(aiServerSseManager.getHealthStatus()).willReturn(healthStatus);
        doThrow(new RuntimeException("Redis 연결 실패"))
            .when(cacheUtil).appendMessage(userId, chatData.content());
        
        // when & then
        assertThatThrownBy(() -> chatService.handleSendEvent(userId, chatData))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_BUFFER_ADD_FAIL);
        
        verify(chatCommandService).saveChatMessage(userId, chatData.content());
    }
    
    // === 스트리밍 중단 처리 테스트 ===
    
    @Test
    @DisplayName("스트리밍 중단 처리 성공")
    void handleStopEvent_Success() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        StreamStopData data = ChatFixture.createStreamStopData(streamId);
        
        doNothing().when(streamingSessionManager).cancelStreaming(streamId);
        doNothing().when(aiServerHttpClient).stopStream(userId);
        
        // when & then
        assertThatCode(() -> chatService.handleStopEvent(userId, data))
            .doesNotThrowAnyException();
        
        verify(streamingSessionManager).cancelStreaming(streamId);
        verify(aiServerHttpClient).stopStream(userId);
    }
    
    @Test
    @DisplayName("스트리밍 중단 처리 실패 - 세션 취소 실패")
    void handleStopEvent_Failure_SessionCancelFail() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        StreamStopData data = ChatFixture.createStreamStopData(streamId);
        
        doThrow(new RuntimeException("세션 관리 실패"))
            .when(streamingSessionManager).cancelStreaming(streamId);
        
        // when & then
        assertThatThrownBy(() -> chatService.handleStopEvent(userId, data))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.AI_SERVER_CONNECTION_FAIL);
    }
    
    @Test
    @DisplayName("스트리밍 중단 처리 실패 - AI 서버 중지 요청 실패")
    void handleStopEvent_Failure_AiServerStopFail() {
        // given
        Long userId = 1L;
        String streamId = ChatFixture.generateTestStreamId();
        StreamStopData data = ChatFixture.createStreamStopData(streamId);
        
        doNothing().when(streamingSessionManager).cancelStreaming(streamId);
        doThrow(new RuntimeException("AI 서버 통신 실패"))
            .when(aiServerHttpClient).stopStream(userId);
        
        // when & then
        assertThatThrownBy(() -> chatService.handleStopEvent(userId, data))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.AI_SERVER_CONNECTION_FAIL);
        
        verify(streamingSessionManager).cancelStreaming(streamId);
    }
    
    // === 스트림 종료 ACK 처리 테스트 ===
    
    @Test
    @DisplayName("스트림 종료 ACK 처리 성공")
    void handleStreamEndAck_Success() {
        // given
        Long userId = 1L;
        SimpTimeData data = ChatFixture.createSimpTimeData();
        
        // when & then
        assertThatCode(() -> chatService.handleStreamEndAck(userId, data))
            .doesNotThrowAnyException();
        
        // TODO: 실제 구현 시 추가 검증 필요
        // 현재는 로깅만 수행하므로 예외가 발생하지 않음을 확인
    }
    
    // === 스트림 종료 NACK 처리 테스트 ===
    
    @Test
    @DisplayName("스트림 종료 NACK 처리 성공")
    void handleStreamEndNack_Success() {
        // given
        Long userId = 1L;
        SimpTimeData data = ChatFixture.createSimpTimeData();
        
        // when & then
        assertThatCode(() -> chatService.handleStreamEndNack(userId, data))
            .doesNotThrowAnyException();
        
        // TODO: 실제 구현 시 추가 검증 필요
        // 현재는 로깅만 수행하므로 예외가 발생하지 않음을 확인
    }
    
    // === 복합 시나리오 테스트 ===
    
    @Test
    @DisplayName("채팅 전체 플로우 테스트 - 신규 사용자")
    void chatFullFlow_NewUser_Success() {
        // given
        CustomUserDetails userDetails = createUserDetails("newuser");
        Long userId = 1L;
        
        // 1. 채팅방 조회 (신규 생성)
        given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(userId, BotConstants.BOT_MEMBER_ID))
            .willReturn(false);
        given(chatCommandService.createBotChatRoom(userId)).willReturn(userId);
        
        // 2. 메시지 전송 준비
        ChatData chatData = ChatFixture.createChatData("안녕하세요!");
        AiServerHealthStatus healthStatus = AiServerHealthStatus.CONNECTED;
        
        doNothing().when(chatCommandService).saveChatMessage(userId, chatData.content());
        given(aiServerSseManager.getHealthStatus()).willReturn(healthStatus);
        doNothing().when(cacheUtil).appendMessage(userId, chatData.content());
        doNothing().when(chatBufferManager).resetTimer(userId);
        
        // when
        ChatList chatList = chatService.getChatMessages(userDetails, 40, null);
        
        // then
        assertThat(chatList.chats()).isNull();
        verify(chatCommandService).createBotChatRoom(userId);
        
        // when - 메시지 전송
        assertThatCode(() -> chatService.handleSendEvent(userId, chatData))
            .doesNotThrowAnyException();
        
        // then
        verify(chatCommandService).saveChatMessage(userId, chatData.content());
        verify(cacheUtil).appendMessage(userId, chatData.content());
    }
    
    @Test
    @DisplayName("채팅 전체 플로우 테스트 - 기존 사용자")
    void chatFullFlow_ExistingUser_Success() {
        // given
        CustomUserDetails userDetails = createUserDetails("existinguser");
        Long userId = 1L;
        
        ChatFixture.ChatScenario scenario = ChatFixture.createCompleteChatScenario("existinguser");
        
        // 1. 채팅방 조회 (기존 존재)
        given(chatRoomMemberRepository.existsByChatRoomIdAndMemberId(userId, BotConstants.BOT_MEMBER_ID))
            .willReturn(true);
        given(chatMessageRepository.findMessagesByChatRoomId(userId, 40, null))
            .willReturn(ChatFixture.createChatList(scenario.getMessages(), false));
        
        // when
        ChatList chatList = chatService.getChatMessages(userDetails, 40, null);
        
        // then
        assertThat(chatList.chats()).isNotEmpty();
        assertThat(chatList.chats()).hasSize(scenario.getMessageCount());
        verify(chatCommandService, never()).createBotChatRoom(anyLong());
    }
    
    // === 테스트 유틸리티 메서드 ===
    
    /**
     * 테스트용 CustomUserDetails 생성
     */
    private CustomUserDetails createUserDetails(String nickname) {
        Member member = MemberFixture.createMemberWithNickname(nickname);
        ChatFixture.setId(member, 1L);
        return new CustomUserDetails(
            member.getEmail(),
            member.getPassword(),
            member.getId().toString(),
            "USER",
            member.getName(),
            member.getClassName().toString(),
            member.getNickname(),
            member.getProfileImgUrl(),
            true
        );
    }
}