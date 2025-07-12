package com.kakaobase.snsapp.domain.chat.service.communication;

import com.kakaobase.snsapp.annotation.ServiceTest;
import com.kakaobase.snsapp.config.ChatTestConfig;
import com.kakaobase.snsapp.domain.chat.converter.ChatConverter;
import com.kakaobase.snsapp.domain.chat.entity.ChatMessage;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoom;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoomMember;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.chat.repository.ChatMessageRepository;
import com.kakaobase.snsapp.domain.chat.repository.ChatRoomMemberRepository;
import com.kakaobase.snsapp.domain.chat.repository.ChatRoomRepository;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.fixture.chat.ChatFixture;
import com.kakaobase.snsapp.fixture.members.MemberFixture;
import com.kakaobase.snsapp.global.common.constant.BotConstants;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * ChatCommandService 단위 테스트
 * 
 * 테스트 대상:
 * - 봇 채팅방 생성 로직
 * - 사용자 메시지 저장 로직
 * - 봇 메시지 저장 로직
 * - 예외 상황 처리
 */
@ServiceTest
@Import(ChatTestConfig.class)
@DisplayName("ChatCommandService 단위 테스트")
class ChatCommandServiceTest {
    
    @Mock
    private EntityManager entityManager;
    
    @Mock
    private ChatMessageRepository chatMessageRepository;
    
    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    
    @Mock
    private ChatConverter chatConverter;
    
    @InjectMocks
    private ChatCommandService chatCommandService;
    
    // === 봇 채팅방 생성 테스트 ===
    
    @Test
    @DisplayName("봇 채팅방 생성 성공")
    void createBotChatRoom_Success() {
        // given
        Member testUser = MemberFixture.createMemberWithNickname("testuser");
        ChatFixture.setId(testUser, 1L);
        
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(testUser.getId());
        Member botMember = ChatFixture.createBotMember();
        
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(chatRoom);
        given(entityManager.getReference(Member.class, testUser.getId())).willReturn(testUser);
        given(entityManager.getReference(Member.class, BotConstants.BOT_MEMBER_ID)).willReturn(botMember);
        
        // when
        Long result = chatCommandService.createBotChatRoom(testUser.getId());
        
        // then
        assertThat(result).isEqualTo(testUser.getId());
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
    }
    
    @Test
    @DisplayName("봇 채팅방 생성 실패 - 채팅방 저장 실패")
    void createBotChatRoom_Failure_ChatRoomSaveFail() {
        // given
        Long userId = 1L;
        
        given(chatRoomRepository.save(any(ChatRoom.class)))
            .willThrow(new RuntimeException("DB 저장 실패"));
        
        // when & then
        assertThatThrownBy(() -> chatCommandService.createBotChatRoom(userId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("DB 저장 실패");
    }
    
    // === 사용자 메시지 저장 테스트 ===
    
    @Test
    @DisplayName("사용자 메시지 저장 성공")
    void saveChatMessage_Success() {
        // given
        Long userId = 1L;
        String content = "안녕하세요! 테스트 메시지입니다.";
        
        Member testUser = MemberFixture.createMemberWithNickname("testuser");
        ChatFixture.setId(testUser, userId);
        
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        ChatMessage message = ChatFixture.createUserMessage(testUser, chatRoom, content);
        
        given(chatConverter.toChatMessage(userId, content)).willReturn(message);
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(message);
        
        // when & then
        assertThatCode(() -> chatCommandService.saveChatMessage(userId, content))
            .doesNotThrowAnyException();
        
        verify(chatConverter).toChatMessage(userId, content);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }
    
    @Test
    @DisplayName("사용자 메시지 저장 실패 - 컨버터 예외")
    void saveChatMessage_Failure_ConverterException() {
        // given
        Long userId = 1L;
        String content = "테스트 메시지";
        
        // ChatConverter에서 예외가 발생하는 상황을 시뮬레이션
        given(chatConverter.toChatMessage(userId, content))
            .willThrow(new ChatException(ChatErrorCode.CHAT_INVALID, userId));
        
        // when & then - ChatCommandService가 예외를 MESSAGE_SAVE_FAIL로 변환
        assertThatThrownBy(() -> chatCommandService.saveChatMessage(userId, content))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.MESSAGE_SAVE_FAIL);
    }
    
    @Test
    @DisplayName("사용자 메시지 저장 실패 - 저장소 오류")
    void saveChatMessage_Failure_RepositoryError() {
        // given
        Long userId = 1L;
        String content = "테스트 메시지";
        
        Member testUser = MemberFixture.createMemberWithNickname("testuser");
        ChatFixture.setId(testUser, userId);
        
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        ChatMessage message = ChatFixture.createUserMessage(testUser, chatRoom, content);
        
        given(chatConverter.toChatMessage(userId, content)).willReturn(message);
        given(chatMessageRepository.save(any(ChatMessage.class)))
            .willThrow(new RuntimeException("DB 저장 실패"));
        
        // when & then
        assertThatThrownBy(() -> chatCommandService.saveChatMessage(userId, content))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.MESSAGE_SAVE_FAIL);
    }
    
    // === 사용자 메시지 동기 저장 테스트 ===
    
    @Test
    @DisplayName("사용자 메시지 동기 저장 성공")
    void saveChatMessageSync_Success() {
        // given
        Long userId = 1L;
        String content = "동기 저장 테스트 메시지";
        
        Member testUser = MemberFixture.createMemberWithNickname("testuser");
        ChatFixture.setId(testUser, userId);
        
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        ChatMessage message = ChatFixture.createUserMessage(testUser, chatRoom, content);
        ChatFixture.setId(message, 100L);
        
        given(chatConverter.toChatMessage(userId, content)).willReturn(message);
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(message);
        
        // when
        Long result = chatCommandService.saveChatMessageSync(userId, content);
        
        // then
        assertThat(result).isEqualTo(100L);
        verify(chatConverter).toChatMessage(userId, content);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }
    
    // === 봇 메시지 저장 테스트 ===
    
    @Test
    @DisplayName("봇 메시지 저장 성공")
    void saveBotMessage_Success() {
        // given
        Long userId = 1L;
        String botResponse = "안녕하세요! AI 봇 로로입니다.";
        
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        ChatMessage botMessage = ChatFixture.createBotMessage(chatRoom, botResponse);
        
        given(chatConverter.toBotMessage(userId, botResponse)).willReturn(botMessage);
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(botMessage);
        
        // when & then
        assertThatCode(() -> chatCommandService.saveBotMessage(userId, botResponse))
            .doesNotThrowAnyException();
        
        verify(chatConverter).toBotMessage(userId, botResponse);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }
    
    @Test
    @DisplayName("봇 메시지 저장 실패 - 컨버터 예외")
    void saveBotMessage_Failure_ConverterException() {
        // given
        Long userId = 1L;
        String botResponse = "테스트 봇 응답";
        
        // ChatConverter에서 예외가 발생하는 상황을 시뮬레이션
        given(chatConverter.toBotMessage(userId, botResponse))
            .willThrow(new ChatException(ChatErrorCode.CHAT_INVALID, userId));
        
        // when & then - ChatCommandService가 예외를 MESSAGE_SAVE_FAIL로 변환
        assertThatThrownBy(() -> chatCommandService.saveBotMessage(userId, botResponse))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.MESSAGE_SAVE_FAIL);
    }
    
    @Test
    @DisplayName("봇 메시지 저장 실패 - 저장소 오류")
    void saveBotMessage_Failure_RepositoryError() {
        // given
        Long userId = 1L;
        String botResponse = "테스트 봇 응답";
        
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        ChatMessage botMessage = ChatFixture.createBotMessage(chatRoom, botResponse);
        
        given(chatConverter.toBotMessage(userId, botResponse)).willReturn(botMessage);
        given(chatMessageRepository.save(any(ChatMessage.class)))
            .willThrow(new RuntimeException("DB 저장 실패"));
        
        // when & then
        assertThatThrownBy(() -> chatCommandService.saveBotMessage(userId, botResponse))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.MESSAGE_SAVE_FAIL);
    }
    
    // === 봇 메시지 동기 저장 테스트 ===
    
    @Test
    @DisplayName("봇 메시지 동기 저장 성공")
    void saveBotMessageSync_Success() {
        // given
        Long userId = 1L;
        String botResponse = "동기 저장 봇 응답";
        
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        ChatMessage botMessage = ChatFixture.createBotMessage(chatRoom, botResponse);
        ChatFixture.setId(botMessage, 200L);
        
        given(chatConverter.toBotMessage(userId, botResponse)).willReturn(botMessage);
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(botMessage);
        
        // when
        Long result = chatCommandService.saveBotMessageSync(userId, botResponse);
        
        // then
        assertThat(result).isEqualTo(200L);
        verify(chatConverter).toBotMessage(userId, botResponse);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }
    
    // === 메시지 상태 업데이트 테스트 ===
    
    @Test
    @DisplayName("메시지 상태 업데이트 성공")
    void updateMessageStatus_Success() {
        // given
        Long messageId = 1L;
        Long userId = 1L;
        
        // when & then
        assertThatCode(() -> chatCommandService.updateMessageStatus(messageId, userId))
            .doesNotThrowAnyException();
        
        // TODO: 실제 구현 시 추가 검증 필요
        // 현재는 로깅만 수행하므로 예외가 발생하지 않음을 확인
    }
    
    // === 스트리밍 메시지 시나리오 테스트 ===
    
    @Test
    @DisplayName("스트리밍 봇 메시지 저장 성공")
    void saveBotMessage_StreamingScenario_Success() {
        // given
        Long userId = 1L;
        ChatFixture.StreamingScenario scenario = ChatFixture.createDefaultStreamingScenario(userId);
        String finalMessage = scenario.getExpectedFinalMessage();
        
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        ChatMessage botMessage = ChatFixture.createBotMessage(chatRoom, finalMessage);
        
        given(chatConverter.toBotMessage(userId, finalMessage)).willReturn(botMessage);
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(botMessage);
        
        // when & then
        assertThatCode(() -> chatCommandService.saveBotMessage(userId, finalMessage))
            .doesNotThrowAnyException();
        
        verify(chatConverter).toBotMessage(userId, finalMessage);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }
    
    @Test
    @DisplayName("긴 스트리밍 봇 메시지 저장 성공")
    void saveBotMessage_LongStreamingScenario_Success() {
        // given
        Long userId = 1L;
        ChatFixture.StreamingScenario scenario = ChatFixture.createLongStreamingScenario(userId);
        String finalMessage = scenario.getExpectedFinalMessage();
        
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        ChatMessage botMessage = ChatFixture.createBotMessage(chatRoom, finalMessage);
        
        given(chatConverter.toBotMessage(userId, finalMessage)).willReturn(botMessage);
        given(chatMessageRepository.save(any(ChatMessage.class))).willReturn(botMessage);
        
        // when & then
        assertThatCode(() -> chatCommandService.saveBotMessage(userId, finalMessage))
            .doesNotThrowAnyException();
        
        verify(chatConverter).toBotMessage(userId, finalMessage);
        verify(chatMessageRepository).save(any(ChatMessage.class));
        
        // 메시지 길이 확인
        assertThat(finalMessage.length()).isGreaterThan(50);
    }
}