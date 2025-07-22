package com.kakaobase.snsapp.global.common.metrics;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QueryCountInspector implements StatementInspector {

    private final ThreadLocal<Counter> queryCount = new ThreadLocal<>();

    public void startCounter() {
        long startTime = System.currentTimeMillis();
        queryCount.set(new Counter(0L, startTime));
        log.debug("QueryCountInspector: Counter started at {}", startTime);
    }

    public Counter getQueryCount() {
        Counter counter = queryCount.get();
        log.debug("QueryCountInspector: Getting counter - {}", 
            counter != null ? "count=" + counter.getCount() : "null");
        return counter;
    }

    public void clearCounter() {
        Counter counter = queryCount.get();
        if (counter != null) {
            log.debug("QueryCountInspector: Clearing counter with final count: {}", counter.getCount());
        }
        queryCount.remove();
    }

    @Override
    public String inspect(String sql) {
        Counter counter = queryCount.get();
        if (counter != null) {
            counter.increaseCount();
            log.debug("QueryCountInspector: SQL executed (count: {}): {}", counter.getCount(), sql);
        } else {
            log.warn("QueryCountInspector: Counter is null when executing SQL: {}", sql);
        }
        return sql;
    }

    @Getter
    public class Counter {
        private Long count;
        private Long time;

        public Counter(Long count, Long time) {
            this.count = count;
            this.time = time;
        }

        public void increaseCount() {
            count++;
        }

        public Long getElapsedTime() {
            return System.currentTimeMillis() - time;
        }
    }
}
