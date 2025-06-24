package com.kakaobase.snsapp.domain.posts.event;

import com.kakaobase.snsapp.domain.posts.util.BoardType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 게시글 카운터 관리
 *
 * <p>게시판별로 게시글 수를 카운트하여 봇 게시글 생성 조건을 관리합니다.</p>
 */
@Slf4j
@Component
public class PostCounter {

    /**
     * 게시판별 카운터 맵
     *
     * <p>스레드 안전성을 위해 ConcurrentHashMap과 AtomicInteger를 사용합니다.</p>
     */
    private final Map<BoardType, AtomicInteger> counters = new ConcurrentHashMap<>();

    /**
     * 게시글 카운터 증가
     *
     * <p>지정된 게시판의 카운터를 1 증가시키고 현재 값을 반환합니다.</p>
     *
     * @param boardType 게시판 타입
     * @return 증가 후 카운터 값
     */
    public int increment(BoardType boardType) {
        AtomicInteger counter = counters.computeIfAbsent(boardType, k -> new AtomicInteger(0));
        int newValue = counter.incrementAndGet();
        log.debug("카운터 증가 - boardType: {}, count: {}", boardType, newValue);
        return newValue;
    }

    /**
     * 게시글 카운터 리셋
     *
     * <p>지정된 게시판의 카운터를 0으로 초기화합니다.</p>
     *
     * @param boardType 게시판 타입
     */
    public void reset(BoardType boardType) {
        counters.computeIfAbsent(boardType, k -> new AtomicInteger(0)).set(0);
        log.debug("카운터 리셋 - boardType: {}", boardType);
    }

    /**
     * 현재 카운터 값 조회
     *
     * <p>지정된 게시판의 현재 카운터 값을 반환합니다.</p>
     *
     * @param boardType 게시판 타입
     * @return 현재 카운터 값
     */
    public int getCount(BoardType boardType) {
        AtomicInteger counter = counters.get(boardType);
        int count = (counter != null) ? counter.get() : 0;
        log.debug("카운터 조회 - boardType: {}, count: {}", boardType, count);
        return count;
    }

    /**
     * 모든 카운터 리셋
     *
     * <p>모든 게시판의 카운터를 0으로 초기화합니다.
     * 주로 테스트나 초기화 작업에 사용됩니다.</p>
     */
    public void resetAll() {
        counters.forEach((boardType, counter) -> counter.set(0));
        log.info("모든 카운터 리셋 완료");
    }

    /**
     * 카운터 상태 정보 반환
     *
     * <p>디버깅용으로 모든 게시판의 카운터 상태를 문자열로 반환합니다.</p>
     *
     * @return 카운터 상태 문자열
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder("PostCounter Status: ");
        counters.forEach((boardType, counter) ->
                status.append(String.format("[%s: %d] ", boardType, counter.get()))
        );
        return status.toString();
    }
}