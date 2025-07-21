package com.kakaobase.snsapp.domain.chat.util;

import com.kakaobase.snsapp.annotation.ServiceTest;
import com.kakaobase.snsapp.config.ChatTestConfig;
import com.kakaobase.snsapp.domain.chat.exception.ChatException;
import com.kakaobase.snsapp.domain.chat.exception.errorcode.ChatErrorCode;
import com.kakaobase.snsapp.fixture.chat.ChatFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * ChatBufferCacheUtil 단위 테스트
 * 
 * 테스트 대상:
 * - Redis 캐시 기본 연산 (저장, 조회, 삭제)
 * - 메시지 누적 로직
 * - TTL 관리
 * - 예외 상황 처리
 * - 버퍼 존재 여부 확인
 */
@ServiceTest
@DisplayName("ChatBufferCacheUtil 단위 테스트")
class ChatBufferCacheUtilTest {
    
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @InjectMocks
    private ChatBufferCacheUtil chatBufferCacheUtil;
    
    private static final String CACHE_PREFIX = "chatbuffer:";
    private static final long TTL_SECONDS = 60L;
    
    @BeforeEach
    void setUp() {
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
    }
    
    // === 키 생성 테스트 ===
    
    @Test
    @DisplayName("캐시 키 생성 확인")
    void generateKey_Verification() {
        // given
        Long userId = 1L;
        String expectedKey = CACHE_PREFIX + userId;
        
        // when - appendMessage 호출을 통해 키 생성 확인
        given(valueOperations.get(expectedKey)).willReturn(null);
        
        chatBufferCacheUtil.appendMessage(userId, "테스트 메시지");
        
        // then - 올바른 키로 Redis 연산 수행 확인
        verify(valueOperations).get(expectedKey);
        verify(valueOperations).set(eq(expectedKey), anyString(), eq(TTL_SECONDS), eq(TimeUnit.SECONDS));
    }
    
    // === 메시지 추가 테스트 ===
    
    @Test
    @DisplayName("메시지 추가 성공 - 새로운 버퍼")
    void appendMessage_NewBuffer_Success() {
        // given
        Long userId = 1L;
        String message = "안녕하세요!";
        String key = CACHE_PREFIX + userId;
        
        given(valueOperations.get(key)).willReturn(null);
        
        // when
        chatBufferCacheUtil.appendMessage(userId, message);
        
        // then
        verify(valueOperations).get(key);
        verify(valueOperations).set(key, message.trim(), TTL_SECONDS, TimeUnit.SECONDS);
    }
    
    @Test
    @DisplayName("메시지 추가 성공 - 기존 버퍼에 누적")
    void appendMessage_ExistingBuffer_Success() {
        // given
        Long userId = 1L;
        String existingMessage = "안녕하세요";
        String newMessage = "반갑습니다";
        String expectedMessage = existingMessage + " " + newMessage;
        String key = CACHE_PREFIX + userId;
        
        given(valueOperations.get(key)).willReturn(existingMessage);
        
        // when
        chatBufferCacheUtil.appendMessage(userId, newMessage);
        
        // then
        verify(valueOperations).get(key);
        verify(valueOperations).set(key, expectedMessage, TTL_SECONDS, TimeUnit.SECONDS);
    }
    
    @Test
    @DisplayName("메시지 추가 성공 - 공백 문자 정리")
    void appendMessage_TrimWhitespace_Success() {
        // given
        Long userId = 1L;
        String messageWithSpaces = "  테스트 메시지  ";
        String trimmedMessage = "테스트 메시지";
        String key = CACHE_PREFIX + userId;
        
        given(valueOperations.get(key)).willReturn(null);
        
        // when
        chatBufferCacheUtil.appendMessage(userId, messageWithSpaces);
        
        // then
        verify(valueOperations).set(key, trimmedMessage, TTL_SECONDS, TimeUnit.SECONDS);
    }
    
    @Test
    @DisplayName("메시지 추가 성공 - 빈 기존 버퍼")
    void appendMessage_EmptyExistingBuffer_Success() {
        // given
        Long userId = 1L;
        String message = "새 메시지";
        String key = CACHE_PREFIX + userId;
        
        given(valueOperations.get(key)).willReturn("");
        
        // when
        chatBufferCacheUtil.appendMessage(userId, message);
        
        // then
        verify(valueOperations).set(key, message, TTL_SECONDS, TimeUnit.SECONDS);
    }
    
    @Test
    @DisplayName("메시지 추가 실패 - Redis 예외")
    void appendMessage_RedisException_Failure() {
        // given
        Long userId = 1L;
        String message = "테스트 메시지";
        String key = CACHE_PREFIX + userId;
        
        given(valueOperations.get(key)).willThrow(new RuntimeException("Redis 연결 실패"));
        
        // when & then
        assertThatThrownBy(() -> chatBufferCacheUtil.appendMessage(userId, message))
            .isInstanceOf(ChatException.class)
            .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_BUFFER_ADD_FAIL);
    }
    
    // === TTL 연장 테스트 ===
    
    @Test
    @DisplayName("TTL 연장 성공 - 키 존재")
    void extendTTL_KeyExists_Success() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(stringRedisTemplate.hasKey(key)).willReturn(true);
        given(stringRedisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS)).willReturn(true);
        
        // when
        chatBufferCacheUtil.extendTTL(userId);
        
        // then
        verify(stringRedisTemplate).hasKey(key);
        verify(stringRedisTemplate).expire(key, TTL_SECONDS, TimeUnit.SECONDS);
    }
    
    @Test
    @DisplayName("TTL 연장 스킵 - 키 존재하지 않음")
    void extendTTL_KeyNotExists_Skip() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(stringRedisTemplate.hasKey(key)).willReturn(false);
        
        // when
        chatBufferCacheUtil.extendTTL(userId);
        
        // then
        verify(stringRedisTemplate).hasKey(key);
        verify(stringRedisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }
    
    @Test
    @DisplayName("TTL 연장 예외 처리 - Redis 예외")
    void extendTTL_RedisException_HandleGracefully() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(stringRedisTemplate.hasKey(key)).willThrow(new RuntimeException("Redis 연결 실패"));
        
        // when & then - 예외가 발생해도 메서드는 정상 완료되어야 함
        assertThatCode(() -> chatBufferCacheUtil.extendTTL(userId))
            .doesNotThrowAnyException();
    }
    
    // === 버퍼 조회 및 삭제 테스트 ===
    
    @Test
    @DisplayName("버퍼 조회 및 삭제 성공 - 버퍼 존재")
    void getAndDeleteBuffer_BufferExists_Success() {
        // given
        Long userId = 1L;
        String expectedContent = "테스트 버퍼 내용";
        String key = CACHE_PREFIX + userId;
        
        given(valueOperations.get(key)).willReturn(expectedContent);
        given(stringRedisTemplate.delete(key)).willReturn(true);
        
        // when
        String result = chatBufferCacheUtil.getAndDeleteBuffer(userId);
        
        // then
        assertThat(result).isEqualTo(expectedContent.trim());
        verify(valueOperations).get(key);
        verify(stringRedisTemplate).delete(key);
    }
    
    @Test
    @DisplayName("버퍼 조회 및 삭제 - 버퍼 존재하지 않음")
    void getAndDeleteBuffer_BufferNotExists_ReturnEmpty() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(valueOperations.get(key)).willReturn(null);
        
        // when
        String result = chatBufferCacheUtil.getAndDeleteBuffer(userId);
        
        // then
        assertThat(result).isEmpty();
        verify(valueOperations).get(key);
        verify(stringRedisTemplate, never()).delete(anyString());
    }
    
    @Test
    @DisplayName("버퍼 조회 및 삭제 - 공백 문자 정리")
    void getAndDeleteBuffer_TrimWhitespace_Success() {
        // given
        Long userId = 1L;
        String contentWithSpaces = "  테스트 내용  ";
        String expectedContent = "테스트 내용";
        String key = CACHE_PREFIX + userId;
        
        given(valueOperations.get(key)).willReturn(contentWithSpaces);
        given(stringRedisTemplate.delete(key)).willReturn(true);
        
        // when
        String result = chatBufferCacheUtil.getAndDeleteBuffer(userId);
        
        // then
        assertThat(result).isEqualTo(expectedContent);
    }
    
    @Test
    @DisplayName("버퍼 조회 및 삭제 예외 처리 - Redis 예외")
    void getAndDeleteBuffer_RedisException_ReturnEmpty() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(valueOperations.get(key)).willThrow(new RuntimeException("Redis 연결 실패"));
        
        // when
        String result = chatBufferCacheUtil.getAndDeleteBuffer(userId);
        
        // then
        assertThat(result).isEmpty();
    }
    
    // === 버퍼 내용 확인 테스트 ===
    
    @Test
    @DisplayName("버퍼 내용 확인 성공 - 삭제하지 않음")
    void peekBuffer_Success() {
        // given
        Long userId = 1L;
        String expectedContent = "테스트 버퍼 내용";
        String key = CACHE_PREFIX + userId;
        
        given(valueOperations.get(key)).willReturn(expectedContent);
        
        // when
        String result = chatBufferCacheUtil.peekBuffer(userId);
        
        // then
        assertThat(result).isEqualTo(expectedContent.trim());
        verify(valueOperations).get(key);
        verify(stringRedisTemplate, never()).delete(anyString());
    }
    
    @Test
    @DisplayName("버퍼 내용 확인 - 버퍼 존재하지 않음")
    void peekBuffer_BufferNotExists_ReturnEmpty() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(valueOperations.get(key)).willReturn(null);
        
        // when
        String result = chatBufferCacheUtil.peekBuffer(userId);
        
        // then
        assertThat(result).isEmpty();
    }
    
    // === 버퍼 강제 삭제 테스트 ===
    
    @Test
    @DisplayName("버퍼 강제 삭제 성공")
    void deleteBuffer_Success() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(stringRedisTemplate.delete(key)).willReturn(true);
        
        // when
        chatBufferCacheUtil.deleteBuffer(userId);
        
        // then
        verify(stringRedisTemplate).delete(key);
    }
    
    @Test
    @DisplayName("버퍼 강제 삭제 예외 처리 - Redis 예외")
    void deleteBuffer_RedisException_HandleGracefully() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(stringRedisTemplate.delete(key)).willThrow(new RuntimeException("Redis 연결 실패"));
        
        // when & then - 예외가 발생해도 메서드는 정상 완료되어야 함
        assertThatCode(() -> chatBufferCacheUtil.deleteBuffer(userId))
            .doesNotThrowAnyException();
    }
    
    // === 버퍼 존재 여부 확인 테스트 ===
    
    @Test
    @DisplayName("버퍼 존재 여부 확인 - 존재함")
    void exists_BufferExists_ReturnTrue() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(stringRedisTemplate.hasKey(key)).willReturn(true);
        
        // when
        boolean result = chatBufferCacheUtil.exists(userId);
        
        // then
        assertThat(result).isTrue();
        verify(stringRedisTemplate).hasKey(key);
    }
    
    @Test
    @DisplayName("버퍼 존재 여부 확인 - 존재하지 않음")
    void exists_BufferNotExists_ReturnFalse() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(stringRedisTemplate.hasKey(key)).willReturn(false);
        
        // when
        boolean result = chatBufferCacheUtil.exists(userId);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("hasBuffer 메서드 - exists 별칭 확인")
    void hasBuffer_Alias_Success() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(stringRedisTemplate.hasKey(key)).willReturn(true);
        
        // when
        boolean result = chatBufferCacheUtil.hasBuffer(userId);
        
        // then
        assertThat(result).isTrue();
        verify(stringRedisTemplate).hasKey(key);
    }
    
    @Test
    @DisplayName("버퍼 존재 여부 확인 예외 처리 - Redis 예외")
    void exists_RedisException_ReturnFalse() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        
        given(stringRedisTemplate.hasKey(key)).willThrow(new RuntimeException("Redis 연결 실패"));
        
        // when
        boolean result = chatBufferCacheUtil.exists(userId);
        
        // then
        assertThat(result).isFalse();
    }
    
    // === 복합 시나리오 테스트 ===
    
    @Test
    @DisplayName("전체 버퍼 생명주기 테스트")
    void bufferLifecycle_Success() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        String message1 = "첫 번째 메시지";
        String message2 = "두 번째 메시지";
        String expectedFinalMessage = message1 + " " + message2;
        
        // 초기 상태 - 버퍼 없음
        given(stringRedisTemplate.hasKey(key)).willReturn(false);
        
        // 첫 번째 메시지 추가
        given(valueOperations.get(key)).willReturn(null);
        
        // when - 1. 버퍼 존재 확인 (없음)
        boolean existsInitially = chatBufferCacheUtil.exists(userId);
        
        // when - 2. 첫 번째 메시지 추가
        chatBufferCacheUtil.appendMessage(userId, message1);
        
        // when - 3. 버퍼 존재 확인 (있음) 
        given(stringRedisTemplate.hasKey(key)).willReturn(true);
        boolean existsAfterFirst = chatBufferCacheUtil.exists(userId);
        
        // when - 4. 두 번째 메시지 추가
        given(valueOperations.get(key)).willReturn(message1);
        chatBufferCacheUtil.appendMessage(userId, message2);
        
        // when - 5. TTL 연장
        chatBufferCacheUtil.extendTTL(userId);
        
        // when - 6. 버퍼 내용 확인 (삭제하지 않음)
        given(valueOperations.get(key)).willReturn(expectedFinalMessage);
        String peekedContent = chatBufferCacheUtil.peekBuffer(userId);
        
        // when - 7. 버퍼 조회 및 삭제
        String finalContent = chatBufferCacheUtil.getAndDeleteBuffer(userId);
        
        // then
        assertThat(existsInitially).isFalse();
        assertThat(existsAfterFirst).isTrue();
        assertThat(peekedContent).isEqualTo(expectedFinalMessage);
        assertThat(finalContent).isEqualTo(expectedFinalMessage);
        
        verify(stringRedisTemplate).expire(key, TTL_SECONDS, TimeUnit.SECONDS);
        verify(stringRedisTemplate).delete(key);
    }
    
    @ParameterizedTest
    @ValueSource(longs = {1L, 100L, 999L, 12345L})
    @DisplayName("다양한 사용자 ID로 버퍼 연산 테스트")
    void bufferOperations_VariousUserIds(Long userId) {
        // given
        String key = CACHE_PREFIX + userId;
        String message = "사용자 " + userId + "의 메시지";
        
        given(stringRedisTemplate.hasKey(key)).willReturn(false);
        given(valueOperations.get(key)).willReturn(null);
        
        // when
        chatBufferCacheUtil.appendMessage(userId, message);
        boolean exists = chatBufferCacheUtil.exists(userId);
        
        // then
        verify(valueOperations).set(key, message, TTL_SECONDS, TimeUnit.SECONDS);
        // exists는 Mock에서 false로 설정했으므로 확인 생략
    }
    
    @Test
    @DisplayName("스트리밍 시나리오 버퍼 누적 테스트")
    void bufferAccumulation_StreamingScenario() {
        // given
        Long userId = 1L;
        String key = CACHE_PREFIX + userId;
        ChatFixture.StreamingScenario scenario = ChatFixture.createDefaultStreamingScenario(userId);
        
        // 첫 번째 청크
        given(valueOperations.get(key)).willReturn(null);
        
        // when - 첫 번째 청크 추가
        String firstChunk = scenario.getFirstChunk().message();
        chatBufferCacheUtil.appendMessage(userId, firstChunk);
        
        // when - 두 번째 청크 추가 (첫 번째가 이미 있다고 가정)
        given(valueOperations.get(key)).willReturn(firstChunk);
        String secondChunk = scenario.getChunks().get(1).message();
        chatBufferCacheUtil.appendMessage(userId, secondChunk);
        
        // then
        String expectedSecondMessage = firstChunk + " " + secondChunk;
        verify(valueOperations).set(key, firstChunk.trim(), TTL_SECONDS, TimeUnit.SECONDS);
        verify(valueOperations).set(key, expectedSecondMessage, TTL_SECONDS, TimeUnit.SECONDS);
    }
}