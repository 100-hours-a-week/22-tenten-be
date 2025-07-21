package com.kakaobase.snsapp.domain.chat.util;

import com.kakaobase.snsapp.annotation.ServiceTest;
import com.kakaobase.snsapp.config.ChatTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

/**
 * StreamIdGenerator 단위 테스트
 * 
 * 테스트 대상:
 * - StreamId 생성 로직
 * - 유니크함 보장
 * - 동시성 처리
 * - 유효성 검사 로직
 * - 형식 검증
 * - 성능 특성
 */
@ServiceTest
@Import(ChatTestConfig.class)
@DisplayName("StreamIdGenerator 단위 테스트")
class StreamIdGeneratorTest {
    
    private final StreamIdGenerator streamIdGenerator = new StreamIdGenerator();
    
    // StreamId 형식: "숫자-16진수" (예: "12345678901234-a1b2c3d4")
    private static final Pattern STREAM_ID_PATTERN = Pattern.compile("^\\d+-[0-9a-f]+$");
    
    // === StreamId 생성 테스트 ===
    
    @Test
    @DisplayName("StreamId 생성 성공")
    void generate_Success() {
        // when
        String streamId = streamIdGenerator.generate();
        
        // then
        assertThat(streamId).isNotNull();
        assertThat(streamId).isNotEmpty();
        assertThat(streamId).contains("-");
        assertThat(streamId).matches(STREAM_ID_PATTERN);
    }
    
    @Test
    @DisplayName("StreamId 형식 검증")
    void generate_FormatValidation() {
        // when
        String streamId = streamIdGenerator.generate();
        
        // then
        String[] parts = streamId.split("-");
        assertThat(parts).hasSize(2);
        
        // nanoTime 부분 검증 (숫자)
        assertThatCode(() -> Long.parseLong(parts[0]))
            .doesNotThrowAnyException();
        
        // random 부분 검증 (16진수)
        assertThatCode(() -> Long.parseLong(parts[1], 16))
            .doesNotThrowAnyException();
    }
    
    @RepeatedTest(10)
    @DisplayName("StreamId 생성 반복 테스트 - 일관된 형식")
    void generate_RepeatedTest_ConsistentFormat() {
        // when
        String streamId = streamIdGenerator.generate();
        
        // then
        assertThat(streamId).matches(STREAM_ID_PATTERN);
        assertThat(streamIdGenerator.isValid(streamId)).isTrue();
    }
    
    @Test
    @DisplayName("StreamId 유니크함 보장 - 순차 생성")
    void generate_Uniqueness_Sequential() {
        // given
        int generateCount = 1000;
        Set<String> generatedIds = new HashSet<>();
        
        // when
        for (int i = 0; i < generateCount; i++) {
            String streamId = streamIdGenerator.generate();
            generatedIds.add(streamId);
        }
        
        // then - 모든 ID가 유니크해야 함
        assertThat(generatedIds).hasSize(generateCount);
    }
    
    @Test
    @DisplayName("StreamId 유니크함 보장 - 동시 생성")
    void generate_Uniqueness_Concurrent() throws InterruptedException {
        // given
        int threadCount = 10;
        int idsPerThread = 100;
        Set<String> generatedIds = new HashSet<>();
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        String streamId = streamIdGenerator.generate();
                        synchronized (generatedIds) {
                            generatedIds.add(streamId);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // then
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(generatedIds).hasSize(threadCount * idsPerThread);
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("StreamId 생성 시간 차이 반영")
    void generate_TimeDifference() throws InterruptedException {
        // when
        String streamId1 = streamIdGenerator.generate();
        Thread.sleep(1); // 최소한의 시간 차이
        String streamId2 = streamIdGenerator.generate();
        
        // then
        assertThat(streamId1).isNotEqualTo(streamId2);
        
        // 시간 부분 비교 (첫 번째 ID가 더 작아야 함)
        long time1 = Long.parseLong(streamId1.split("-")[0]);
        long time2 = Long.parseLong(streamId2.split("-")[0]);
        assertThat(time2).isGreaterThanOrEqualTo(time1);
    }
    
    // === StreamId 유효성 검사 테스트 ===
    
    @Test
    @DisplayName("유효한 StreamId 검증 성공")
    void isValid_ValidStreamId_ReturnTrue() {
        // given
        String validStreamId = streamIdGenerator.generate();
        
        // when
        boolean result = streamIdGenerator.isValid(validStreamId);
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("유효한 StreamId 검증 - 수동 생성")
    void isValid_ManualValidStreamId_ReturnTrue() {
        // given
        String validStreamId = "123456789012345-abc123def456";
        
        // when
        boolean result = streamIdGenerator.isValid(validStreamId);
        
        // then
        assertThat(result).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "", // 빈 문자열
        "   ", // 공백만
        "invalid", // 하이픈 없음
        "123-", // 랜덤 부분 없음
        "-abc", // 시간 부분 없음
        "abc-def", // 시간 부분이 숫자가 아님
        "123-xyz", // 랜덤 부분이 16진수가 아님
        "123-456-789", // 하이픈이 2개 이상
        "123--456" // 연속 하이픈
    })
    @DisplayName("무효한 StreamId 검증 실패")
    void isValid_InvalidStreamId_ReturnFalse(String invalidStreamId) {
        // when
        boolean result = streamIdGenerator.isValid(invalidStreamId);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("null StreamId 검증 실패")
    void isValid_NullStreamId_ReturnFalse() {
        // when
        boolean result = streamIdGenerator.isValid(null);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("유효성 검사 - 경계 값 테스트")
    void isValid_BoundaryValues() {
        // given
        String minValidTime = "0-0";
        String maxValidTime = String.valueOf(Long.MAX_VALUE) + "-" + Long.toHexString(Long.MAX_VALUE);
        String negativeTime = "-123-abc";
        
        // when & then
        assertThat(streamIdGenerator.isValid(minValidTime)).isTrue();
        assertThat(streamIdGenerator.isValid(maxValidTime)).isTrue();
        assertThat(streamIdGenerator.isValid(negativeTime)).isFalse();
    }
    
    // === 성능 테스트 ===
    
    @Test
    @DisplayName("StreamId 생성 성능 테스트")
    void generate_PerformanceTest() {
        // given
        int iterations = 10000;
        
        // when
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            streamIdGenerator.generate();
        }
        long endTime = System.nanoTime();
        
        // then - More realistic expectations for CI environment
        long durationMs = (endTime - startTime) / 1_000_000;
        assertThat(durationMs).isLessThan(5000); // 5초 이내 (CI 환경 고려)
        
        // 평균 생성 시간 계산 - 더 현실적인 기대값
        double avgTimePerGeneration = (double) durationMs / iterations;
        assertThat(avgTimePerGeneration).isLessThan(1.0); // 1ms 이내
    }
    
    @Test
    @DisplayName("StreamId 유효성 검사 성능 테스트")
    void isValid_PerformanceTest() {
        // given
        int iterations = 10000;
        String validStreamId = streamIdGenerator.generate();
        String invalidStreamId = "invalid-format";
        
        // when - 유효한 ID 검증
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            streamIdGenerator.isValid(validStreamId);
        }
        long validationTime = System.nanoTime() - startTime;
        
        // when - 무효한 ID 검증
        startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            streamIdGenerator.isValid(invalidStreamId);
        }
        long invalidValidationTime = System.nanoTime() - startTime;
        
        // then
        long validDurationMs = validationTime / 1_000_000;
        long invalidDurationMs = invalidValidationTime / 1_000_000;
        
        assertThat(validDurationMs).isLessThan(1000); // 1초 이내 (CI 환경 고려)
        assertThat(invalidDurationMs).isLessThan(1000); // 1초 이내 (CI 환경 고려)
    }
    
    // === 실제 사용 시나리오 테스트 ===
    
    @Test
    @DisplayName("스트리밍 세션 시나리오 - 다중 스트림")
    void streamingScenario_MultipleStreams() {
        // given
        int streamCount = 5;
        Set<String> activeStreams = new HashSet<>();
        
        // when - 여러 스트림 동시 시작
        for (int i = 0; i < streamCount; i++) {
            String streamId = streamIdGenerator.generate();
            activeStreams.add(streamId);
            
            // 각 스트림 ID 유효성 확인
            assertThat(streamIdGenerator.isValid(streamId)).isTrue();
        }
        
        // then
        assertThat(activeStreams).hasSize(streamCount);
        
        // 모든 스트림이 유효한 형식인지 확인
        for (String streamId : activeStreams) {
            assertThat(streamId).matches(STREAM_ID_PATTERN);
        }
    }
    
    @Test
    @DisplayName("부하 테스트 - 대량 스트림 생성")
    void loadTest_MassiveStreamGeneration() {
        // given
        int massiveCount = 100000;
        Set<String> uniqueIds = new HashSet<>();
        
        // when
        for (int i = 0; i < massiveCount; i++) {
            String streamId = streamIdGenerator.generate();
            uniqueIds.add(streamId);
            
            // 주기적으로 유효성 검사
            if (i % 10000 == 0) {
                assertThat(streamIdGenerator.isValid(streamId)).isTrue();
            }
        }
        
        // then
        assertThat(uniqueIds).hasSize(massiveCount); // 모든 ID가 유니크
    }
    
    @Test
    @DisplayName("실시간 스트리밍 시뮬레이션")
    void realTimeStreamingSimulation() throws InterruptedException {
        // given
        int simulationDurationMs = 100;
        Set<String> generatedIds = new HashSet<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        // when - 실시간으로 스트림 생성 시뮬레이션
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                long endTime = System.currentTimeMillis() + simulationDurationMs;
                while (System.currentTimeMillis() < endTime) {
                    String streamId = streamIdGenerator.generate();
                    synchronized (generatedIds) {
                        generatedIds.add(streamId);
                    }
                    Thread.sleep(1); // 1ms 간격
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        // then
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(generatedIds).isNotEmpty();
        
        // 생성된 모든 ID가 유효한지 확인
        for (String streamId : generatedIds) {
            assertThat(streamIdGenerator.isValid(streamId)).isTrue();
        }
        
        executor.shutdown();
    }
    
    // === 에러 케이스 테스트 ===
    
    @Test
    @DisplayName("특수 문자가 포함된 StreamId 검증")
    void isValid_SpecialCharacters_ReturnFalse() {
        // given
        String[] invalidIds = {
            "123-abc@def",
            "123-abc def",
            "123-abc\ndef",
            "123-abc\tdef",
            "123-abc!@#",
            "한글-abc123"
        };
        
        // when & then
        for (String invalidId : invalidIds) {
            assertThat(streamIdGenerator.isValid(invalidId))
                .as("ID '%s' should be invalid", invalidId)
                .isFalse();
        }
    }
    
    @Test
    @DisplayName("매우 긴 StreamId 검증")
    void isValid_VeryLongStreamId() {
        // given - Long.MAX_VALUE 범위 내의 유효한 값
        String validTimepart = String.valueOf(Long.MAX_VALUE);
        String longRandomPart = "a".repeat(15); // 긴 랜덤 부분 (16진수 범위 내)
        String longStreamId = validTimepart + "-" + longRandomPart;
        
        // when
        boolean result = streamIdGenerator.isValid(longStreamId);
        
        // then - 유효한 형식이면 true
        assertThat(result).isTrue();
    }
}