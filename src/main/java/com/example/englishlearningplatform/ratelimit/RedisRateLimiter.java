package com.example.englishlearningplatform.ratelimit;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimiter implements RateLimiter {

    // Kết quả: 0 = cho qua, > 0 = số giây phải chờ. Chạy nguyên tử trong Redis.
    private static final String LUA = """
            local c = redis.call('INCR', KEYS[1])
            if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end
            if c > tonumber(ARGV[1]) then
                local ttl = redis.call('TTL', KEYS[1])
                if ttl < 0 then
                    redis.call('EXPIRE', KEYS[1], ARGV[2])
                    ttl = tonumber(ARGV[2])
                end
                if ttl < 1 then ttl = 1 end
                return ttl
            end
            return 0
            """;

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(LUA, Long.class);

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public RateLimitDecision tryAcquire(String key, int limit, Duration window) {
        if (limit < 1 || window.toSeconds() < 1) {
            throw new IllegalArgumentException("limit must be >= 1 and window >= 1 second");
        }

        Long result = redis.execute(SCRIPT, List.of(key),
                String.valueOf(limit), String.valueOf(window.toSeconds()));
        if (result == null) {
            throw new IllegalStateException("Rate limit script returned no result");
        }
        return result == 0 ? RateLimitDecision.allow() : RateLimitDecision.deny(result);
    }
}