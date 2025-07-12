package com.kakaobase.snsapp.domain.chat.converter;

import com.kakaobase.snsapp.annotation.ServiceTest;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.domain.members.entity.Member;
import com.kakaobase.snsapp.domain.chat.entity.ChatRoom;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

/**
 * ChatConverter 단위 테스트
 * 
 * 테스트 대상:
 * - 메시지 내용 검증 로직
 * - null/empty content 처리
 * - 사용자 메시지 변환
 * - 봇 메시지 변환
 */
@ServiceTest
@DisplayName("ChatConverter 단위 테스트")
class ChatConverterTest {
    
    @Mock
    private EntityManager entityManager;
    
    @InjectMocks
    private ChatConverter chatConverter;
    
    // === 사용자 메시지 변환 테스트 ===
    
    @Test
    @DisplayName("사용자 메시지 변환 성공")
    void toChatMessage_Success() {
        // given
        Long userId = 1L;
        String content = "안녕하세요! 테스트 메시지입니다.";
        
        Member testUser = MemberFixture.createMemberWithNickname("testuser");
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        
        given(entityManager.getReference(ChatRoom.class, userId)).willReturn(chatRoom);
        given(entityManager.getReference(Member.class, userId)).willReturn(testUser);
        
        // when & then
        assertThatCode(() -> chatConverter.toChatMessage(userId, content))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("사용자 메시지 변환 실패 - null 내용")
    void toChatMessage_Failure_NullContent() {
        // given
        Long userId = 1L;
        String content = null;
        
        // when & then
        assertThatThrownBy(() -> chatConverter.toChatMessage(userId, content))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_INVALID)
            .hasFieldOrPropertyWithValue("userId", userId);
    }
    
    @Test
    @DisplayName("사용자 메시지 변환 실패 - 빈 내용")
    void toChatMessage_Failure_EmptyContent() {
        // given
        Long userId = 1L;
        String content = "";
        
        // when & then
        assertThatThrownBy(() -> chatConverter.toChatMessage(userId, content))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_INVALID)
            .hasFieldOrPropertyWithValue("userId", userId);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", "\n", "  \t\n  "})
    @DisplayName("사용자 메시지 변환 실패 - 공백만 있는 내용")
    void toChatMessage_Failure_WhitespaceOnlyContent(String content) {
        // given
        Long userId = 1L;
        
        // when & then
        assertThatThrownBy(() -> chatConverter.toChatMessage(userId, content))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_INVALID)
            .hasFieldOrPropertyWithValue("userId", userId);
    }
    
    // === 봇 메시지 변환 테스트 ===
    
    @Test
    @DisplayName("봇 메시지 변환 성공")
    void toBotMessage_Success() {
        // given
        Long userId = 1L;
        String content = "안녕하세요! AI 봇 로로입니다.";
        
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        Member botMember = ChatFixture.createBotMember();
        
        given(entityManager.getReference(ChatRoom.class, userId)).willReturn(chatRoom);
        given(entityManager.getReference(Member.class, BotConstants.BOT_MEMBER_ID)).willReturn(botMember);
        
        // when & then
        assertThatCode(() -> chatConverter.toBotMessage(userId, content))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("봇 메시지 변환 실패 - null 내용")
    void toBotMessage_Failure_NullContent() {
        // given
        Long userId = 1L;
        String content = null;
        
        // when & then
        assertThatThrownBy(() -> chatConverter.toBotMessage(userId, content))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_INVALID)
            .hasFieldOrPropertyWithValue("userId", userId);
    }
    
    @Test
    @DisplayName("봇 메시지 변환 실패 - 빈 내용")
    void toBotMessage_Failure_EmptyContent() {
        // given
        Long userId = 1L;
        String content = "";
        
        // when & then
        assertThatThrownBy(() -> chatConverter.toBotMessage(userId, content))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_INVALID)
            .hasFieldOrPropertyWithValue("userId", userId);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", "\n", "  \t\n  "})
    @DisplayName("봇 메시지 변환 실패 - 공백만 있는 내용")
    void toBotMessage_Failure_WhitespaceOnlyContent(String content) {
        // given
        Long userId = 1L;
        
        // when & then
        assertThatThrownBy(() -> chatConverter.toBotMessage(userId, content))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_INVALID)
            .hasFieldOrPropertyWithValue("userId", userId);
    }
    
    // === 경계값 테스트 ===
    
    @Test
    @DisplayName("메시지 변환 - 최소 유효 길이")
    void toChatMessage_MinimumValidLength() {
        // given
        Long userId = 1L;
        String content = "a"; // 1글자
        
        Member testUser = MemberFixture.createMemberWithNickname("testuser");
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        
        given(entityManager.getReference(ChatRoom.class, userId)).willReturn(chatRoom);
        given(entityManager.getReference(Member.class, userId)).willReturn(testUser);
        
        // when & then - 1글자도 유효한 메시지로 간주
        assertThatCode(() -> chatConverter.toChatMessage(userId, content))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("메시지 변환 - 긴 메시지")
    void toChatMessage_LongContent() {
        // given
        Long userId = 1L;
        String content = "A".repeat(10000); // 매우 긴 메시지
        
        Member testUser = MemberFixture.createMemberWithNickname("testuser");
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        
        given(entityManager.getReference(ChatRoom.class, userId)).willReturn(chatRoom);
        given(entityManager.getReference(Member.class, userId)).willReturn(testUser);
        
        // when & then - 긴 메시지도 처리 가능
        assertThatCode(() -> chatConverter.toChatMessage(userId, content))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("메시지 변환 - 특수문자 포함")
    void toChatMessage_SpecialCharacters() {
        // given
        Long userId = 1L;
        String content = "안녕하세요! 😊 @user #hashtag $특수문자 테스트입니다.";
        
        Member testUser = MemberFixture.createMemberWithNickname("testuser");
        ChatRoom chatRoom = ChatFixture.createBotChatRoom(userId);
        
        given(entityManager.getReference(ChatRoom.class, userId)).willReturn(chatRoom);
        given(entityManager.getReference(Member.class, userId)).willReturn(testUser);
        
        // when & then - 특수문자도 처리 가능
        assertThatCode(() -> chatConverter.toChatMessage(userId, content))
            .doesNotThrowAnyException();
    }
}