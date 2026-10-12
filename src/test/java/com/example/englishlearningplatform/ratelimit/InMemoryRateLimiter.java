package com.example.englishlearningplatform.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimiter implements RateLimiter {

    private static final class Window {
        final Instant expiresAt;
        int count;

        Window(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public RateLimitDecision tryAcquire(String key, int limit, Duration window) {
        if (limit < 1 || window.toSeconds() < 1) {
            throw new IllegalArgumentException("limit must be >= 1 and window >= 1 second");
        }

        Instant now = clock.instant();
        RateLimitDecision[] result = new RateLimitDecision[1];

        windows.compute(key, (k, w) -> {
            Window current = (w == null || !now.isBefore(w.expiresAt)) ? new Window(now.plus(window)) : w;
            current.count++;
            if (current.count > limit) {
                long millis = Duration.between(now, current.expiresAt).toMillis();
                result[0] = RateLimitDecision.deny(Math.max(1, (millis + 999) / 1000));
            } else {
                result[0] = RateLimitDecision.allow();
            }
            return current;
        });
        return result[0];
    }
}