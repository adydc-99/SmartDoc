package com.smartdoc.ai;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DailyAiQuotaTest {
    @Test
    void concurrentConsumptionCannotExceedLimitAndDateChangeResetsUsage() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-12T01:00:00Z"), ZoneId.of("Asia/Shanghai"));
        DailyAiQuota quota = new DailyAiQuota(clock);
        ExecutorService pool = Executors.newFixedThreadPool(12);
        AtomicInteger accepted = new AtomicInteger();
        List<Future<?>> jobs = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            jobs.add(pool.submit(() -> {
                try { quota.consume(7); accepted.incrementAndGet(); }
                catch (AiQuotaExceededException ignored) { }
            }));
        }
        for (Future<?> job : jobs) job.get(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertEquals(7, accepted.get());
        assertEquals(7, quota.used());

        clock.set(Instant.parse("2026-08-13T01:00:00Z"));
        assertEquals(0, quota.used());
        quota.consume(7);
        assertEquals(1, quota.used());
    }

    private static final class MutableClock extends Clock {
        private volatile Instant instant;
        private final ZoneId zone;
        private MutableClock(Instant instant, ZoneId zone) { this.instant = instant; this.zone = zone; }
        void set(Instant instant) { this.instant = instant; }
        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return new MutableClock(instant, zone); }
        @Override public Instant instant() { return instant; }
    }
}
