package com.example.englishlearningplatform.ratelimit;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.englishlearningplatform.config.RateLimitProperties;
import com.example.englishlearningplatform.exception.RateLimitExceededException;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private final RateLimiter rateLimiter;
    private final RateLimitProperties props;

    public RateLimitService(RateLimiter rateLimiter, RateLimitProperties props) {
        this.rateLimiter = rateLimiter;
        this.props = props;
    }

    public void checkLogin(String clientIp) {
        check("rl:login:" + clientIp, props.loginPerMinute(), ONE_MINUTE);
    }

    public void checkRegister(String clientIp) {
        check("rl:register:" + clientIp, props.registerPerHour(), ONE_HOUR);
    }

    public void checkChat(String username) {
        check("rl:chat:" + username, props.chatPerMinute(), ONE_MINUTE);
    }

    private void check(String key, int limit, Duration window) {
        try {
            RateLimitDecision d = rateLimiter.tryAcquire(key, limit, window);
            if (!d.allowed()) {
                throw new RateLimitExceededException(d.retryAfterSeconds());
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Rate limiter unavailable, failing open: {}", e.getClass().getSimpleName());
        }
    }
}