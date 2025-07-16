package com.kakaobase.snsapp.domain.chat.service.streaming;

import com.kakaobase.snsapp.annotation.ServiceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

/**
 * ChatTimerManager 단위 테스트 (Mock 기반)
 * 
 * 테스트 대상:
 * - 타이머 생성 및 리셋 로직
 * - 타이머 취소 로직
 * - 스케줄러 호출 검증
 * - 예외 상황 처리
 */
@ServiceTest
@DisplayName("ChatTimerManager 단위 테스트")
class ChatBufferManagerTest {
    
    @Mock
    private ChatBufferManager chatBufferManager;
    
    @Mock
    private ScheduledExecutorService mockScheduler;
    
    @Mock
    private ScheduledFuture<Void> mockFuture;
    
    @InjectMocks
    private ChatBufferManager chatBufferManager;
    
    @BeforeEach
    void setUp() {
        // Mock 스케줄러 주입
        ReflectionTestUtils.setField(chatBufferManager, "scheduler", mockScheduler);
        
        // 기본 Mock 동작 설정 - 명시적 타입 캐스팅
        when(mockScheduler.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class)))
            .thenReturn((ScheduledFuture) mockFuture);
        given(mockFuture.isCancelled()).willReturn(false);
    }
    
    // === 타이머 리셋 테스트 ===
    
    @Test
    @DisplayName("타이머 리셋 성공 - 새로운 사용자")
    void resetTimer_NewUser_Success() {
        // given
        Long userId = 1L;
        
        // when
        chatBufferManager.resetTimer(userId);
        
        // then
        verify(mockScheduler).schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("타이머 리셋 성공 - 기존 타이머 교체")
    void resetTimer_ReplaceExistingTimer_Success() {
        // given
        Long userId = 1L;
        ScheduledFuture<Void> firstMockFuture = mock(ScheduledFuture.class);
        ScheduledFuture<Void> secondMockFuture = mock(ScheduledFuture.class);
        
        when(mockScheduler.schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS)))
            .thenReturn((ScheduledFuture) firstMockFuture)
            .thenReturn((ScheduledFuture) secondMockFuture);
        given(firstMockFuture.isCancelled()).willReturn(false);
        
        // when - 첫 번째 타이머 설정
        chatBufferManager.resetTimer(userId);
        
        // when - 두 번째 타이머로 교체
        chatBufferManager.resetTimer(userId);
        
        // then
        verify(mockScheduler, times(2)).schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS));
        verify(firstMockFuture).cancel(false); // 기존 타이머 취소됨
    }
    
    @ParameterizedTest
    @ValueSource(longs = {1L, 100L, 999L, 12345L})
    @DisplayName("다양한 사용자 ID로 타이머 리셋")
    void resetTimer_VariousUserIds(Long userId) {
        // when
        chatBufferManager.resetTimer(userId);
        
        // then
        verify(mockScheduler).schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS));
    }
    
    // === 타이머 취소 테스트 ===
    
    @Test
    @DisplayName("타이머 취소 성공 - 활성 타이머")
    void cancelTimer_ActiveTimer_Success() {
        // given
        Long userId = 1L;
        ScheduledFuture<Void> activeFuture = mock(ScheduledFuture.class);
        when(mockScheduler.schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS)))
            .thenReturn((ScheduledFuture) activeFuture);
        given(activeFuture.isCancelled()).willReturn(false);
        
        // 타이머 먼저 설정
        chatBufferManager.resetTimer(userId);
        
        // when
        chatBufferManager.cancelTimer(userId);
        
        // then
        verify(activeFuture).cancel(false);
    }
    
    @Test
    @DisplayName("타이머 취소 - 타이머 없는 사용자")
    void cancelTimer_NoTimer_Success() {
        // given
        Long userId = 1L;
        
        // when & then - 예외 발생하지 않아야 함
        chatBufferManager.cancelTimer(userId);
        
        // 스케줄러 취소 호출 없음 확인
        verify(mockFuture, never()).cancel(anyBoolean());
    }
    
    @Test
    @DisplayName("타이머 취소 - 이미 취소된 타이머")
    void cancelTimer_AlreadyCancelledTimer_Success() {
        // given
        Long userId = 1L;
        ScheduledFuture<Void> cancelledFuture = mock(ScheduledFuture.class);
        when(mockScheduler.schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS)))
            .thenReturn((ScheduledFuture) cancelledFuture);
        given(cancelledFuture.isCancelled()).willReturn(true); // 이미 취소됨
        
        // 타이머 설정
        chatBufferManager.resetTimer(userId);
        
        // when & then - 예외 발생하지 않아야 함
        chatBufferManager.cancelTimer(userId);
        
        verify(cancelledFuture).cancel(false);
    }
    
    // === 타이머 트리거 시뮬레이션 테스트 ===
    
    @Test
    @DisplayName("타이머 트리거 시뮬레이션 - 정상 실행")
    void timerTrigger_NormalExecution_Success() {
        // given
        Long userId = 1L;
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        
        // when
        chatBufferManager.resetTimer(userId);
        
        // then - 스케줄러 호출 검증 및 Runnable 캡처
        verify(mockScheduler).schedule(runnableCaptor.capture(), eq(1L), eq(TimeUnit.SECONDS));
        
        // Runnable 실행 시뮬레이션
        Runnable capturedRunnable = runnableCaptor.getValue();
        capturedRunnable.run();
        
        // 버퍼 매니저 호출 검증
        verify(chatBufferManager).sendBufferToAiServerDirectly(userId);
    }
    
    @Test
    @DisplayName("타이머 트리거 예외 처리 - 버퍼 전송 실패")
    void timerTrigger_BufferSendFail_HandleSafely() {
        // given
        Long userId = 1L;
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doThrow(new RuntimeException("버퍼 전송 실패"))
            .when(chatBufferManager).sendBufferToAiServerDirectly(userId);
        
        // when
        chatBufferManager.resetTimer(userId);
        
        // then - 스케줄러 호출 검증 및 Runnable 캡처
        verify(mockScheduler).schedule(runnableCaptor.capture(), eq(1L), eq(TimeUnit.SECONDS));
        
        // Runnable 실행 시뮬레이션 - 예외 발생해도 외부로 전파되지 않아야 함
        Runnable capturedRunnable = runnableCaptor.getValue();
        assertThatCode(() -> capturedRunnable.run()).doesNotThrowAnyException();
        
        // 버퍼 매니저 호출은 여전히 발생해야 함
        verify(chatBufferManager).sendBufferToAiServerDirectly(userId);
    }
    
    // === 복합 시나리오 테스트 ===
    
    @Test
    @DisplayName("다중 사용자 타이머 관리")
    void multiUserTimerManagement_Success() {
        // given
        Long[] userIds = {1L, 2L, 3L, 4L, 5L};
        
        // when - 모든 사용자 타이머 설정
        for (Long userId : userIds) {
            chatBufferManager.resetTimer(userId);
        }
        
        // when - 일부 타이머 취소
        chatBufferManager.cancelTimer(userIds[0]);
        chatBufferManager.cancelTimer(userIds[2]);
        
        // then
        verify(mockScheduler, times(userIds.length))
            .schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("타이머 빠른 연속 리셋")
    void rapidTimerReset_Success() {
        // given
        Long userId = 1L;
        int resetCount = 5;
        
        // when - 빠른 연속 리셋
        for (int i = 0; i < resetCount; i++) {
            chatBufferManager.resetTimer(userId);
        }
        
        // then - 매번 새로운 스케줄 호출
        verify(mockScheduler, times(resetCount))
            .schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("타이머 설정 후 즉시 취소")
    void setTimerThenImmediateCancel_Success() {
        // given
        Long userId = 1L;
        ScheduledFuture<Void> testFuture = mock(ScheduledFuture.class);
        when(mockScheduler.schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS)))
            .thenReturn((ScheduledFuture) testFuture);
        given(testFuture.isCancelled()).willReturn(false);
        
        // when
        chatBufferManager.resetTimer(userId);
        chatBufferManager.cancelTimer(userId);
        
        // then
        verify(mockScheduler).schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS));
        verify(testFuture).cancel(false);
    }
}