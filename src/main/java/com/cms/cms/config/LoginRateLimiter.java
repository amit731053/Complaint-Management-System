package com.cms.cms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory brute-force protection for /api/auth/login.
 * Counts failed logins per email and per IP inside a fixed time window; once the limit is
 * reached, further attempts are refused until the window ends (even with the right password).
 * In-memory = resets on restart and is per-instance, which is fine for a single server.
 */
@Component
public class LoginRateLimiter {

    private record Entry(int count, long windowStart) { }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final int maxPerEmail;
    private final int maxPerIp;
    private final long windowMillis;

    public LoginRateLimiter(@Value("${app.security.login-max-attempts:5}") int maxPerEmail,
                            @Value("${app.security.login-max-attempts-per-ip:300}") int maxPerIp,
                            @Value("${app.security.login-window-minutes:15}") int windowMinutes) {
        this.maxPerEmail = maxPerEmail;
        this.maxPerIp = maxPerIp;
        this.windowMillis = windowMinutes * 60_000L;
    }

    /** @return 0 if the attempt may proceed, otherwise seconds until the block ends. */
    public long retryAfterSeconds(String email, String ip) {
        long now = System.currentTimeMillis();
        return Math.max(remaining("e:" + email, maxPerEmail, now), remaining("i:" + ip, maxPerIp, now));
    }

    public void recordFailure(String email, String ip) {
        long now = System.currentTimeMillis();
        bump("e:" + email, now);
        bump("i:" + ip, now);
        if (entries.size() > 10_000) {
            entries.entrySet().removeIf(e -> now - e.getValue().windowStart() > windowMillis);
        }
    }

    public void recordSuccess(String email) {
        entries.remove("e:" + email);
    }

    private long remaining(String key, int max, long now) {
        Entry e = entries.get(key);
        if (e == null || now - e.windowStart() >= windowMillis || e.count() < max) {
            return 0;
        }
        return Math.max(1, (e.windowStart() + windowMillis - now + 999) / 1000);
    }

    private void bump(String key, long now) {
        entries.compute(key, (k, e) ->
                (e == null || now - e.windowStart() >= windowMillis)
                        ? new Entry(1, now)
                        : new Entry(e.count() + 1, e.windowStart()));
    }
}
