package com.kakaobase.snsapp.fixture.common;

import java.time.LocalDateTime;

/**
 * 테스트 환경에서 시간 제어를 위한 Time Provider
 *
 * 기능:
 * - 테스트에서 시간을 고정하거나 조작 가능
 * - 시간 의존적인 테스트의 일관성 보장
 * - 시간 흐름 시뮬레이션
 */
public class TestTimeProvider {

    private LocalDateTime fixedTime;
    private boolean useFixedTime = false;

    // 기본 고정 시간 (2024년 1월 1일 12:00:00)
    private static final LocalDateTime DEFAULT_FIXED_TIME = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

    /**
     * 현재 시간 반환
     * - 고정 시간이 설정된 경우: 고정 시간 반환
     * - 그렇지 않은 경우: 실제 현재 시간 반환
     *
     * @return 현재 LocalDateTime
     */
    public LocalDateTime now() {
        if (useFixedTime) {
            return fixedTime != null ? fixedTime : DEFAULT_FIXED_TIME;
        }
        return LocalDateTime.now();
    }

    /**
     * 시간을 고정값으로 설정
     *
     * @param fixedTime 고정할 시간
     */
    public void setFixedTime(LocalDateTime fixedTime) {
        this.fixedTime = fixedTime;
        this.useFixedTime = true;
    }

    /**
     * 기본 고정 시간으로 설정
     */
    public void setDefaultFixedTime() {
        this.fixedTime = DEFAULT_FIXED_TIME;
        this.useFixedTime = true;
    }

    /**
     * 실제 시간 사용으로 복원
     */
    public void useRealTime() {
        this.useFixedTime = false;
        this.fixedTime = null;
    }

    /**
     * 고정된 시간을 지정된 시간만큼 앞으로 이동
     *
     * @param minutes 이동할 분 수
     */
    public void addMinutes(long minutes) {
        if (useFixedTime) {
            LocalDateTime currentFixed = fixedTime != null ? fixedTime : DEFAULT_FIXED_TIME;
            this.fixedTime = currentFixed.plusMinutes(minutes);
        }
    }

    /**
     * 고정된 시간을 지정된 시간만큼 뒤로 이동
     *
     * @param minutes 이동할 분 수
     */
    public void subtractMinutes(long minutes) {
        if (useFixedTime) {
            LocalDateTime currentFixed = fixedTime != null ? fixedTime : DEFAULT_FIXED_TIME;
            this.fixedTime = currentFixed.minusMinutes(minutes);
        }
    }

    /**
     * 고정된 시간을 지정된 일수만큼 앞으로 이동
     *
     * @param days 이동할 일 수
     */
    public void addDays(long days) {
        if (useFixedTime) {
            LocalDateTime currentFixed = fixedTime != null ? fixedTime : DEFAULT_FIXED_TIME;
            this.fixedTime = currentFixed.plusDays(days);
        }
    }

    /**
     * 현재 고정 시간 사용 여부 반환
     *
     * @return 고정 시간 사용 여부
     */
    public boolean isUsingFixedTime() {
        return useFixedTime;
    }

    /**
     * 현재 설정된 고정 시간 반환
     *
     * @return 고정 시간 (설정되지 않은 경우 null)
     */
    public LocalDateTime getFixedTime() {
        return fixedTime;
    }

    /**
     * Time Provider 상태 초기화
     */
    public void reset() {
        this.useFixedTime = false;
        this.fixedTime = null;
    }
}