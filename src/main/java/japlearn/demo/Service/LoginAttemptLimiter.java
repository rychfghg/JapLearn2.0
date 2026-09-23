package japlearn.demo.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/** Limits repeated invalid passwords without blocking a class behind one IP. */
@Component
public class LoginAttemptLimiter {
    private static final int MAX_FAILURES = 6;
    private static final long WINDOW_SECONDS = 60;
    private static final int MAX_TRACKED_EMAILS = 20_000;

    private record Bucket(AtomicInteger failures, long startedAt) {}
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        String key = normalize(email);
        if (key.isEmpty()) return false;
        Bucket bucket = buckets.get(key);
        return bucket != null && Instant.now().getEpochSecond() - bucket.startedAt() < WINDOW_SECONDS
                && bucket.failures().get() >= MAX_FAILURES;
    }

    public void recordFailure(String email) {
        String key = normalize(email);
        if (key.isEmpty()) return;
        long now = Instant.now().getEpochSecond();
        if (buckets.size() > MAX_TRACKED_EMAILS) {
            buckets.entrySet().removeIf(entry -> now - entry.getValue().startedAt() >= WINDOW_SECONDS);
        }
        buckets.compute(key, (ignored, previous) -> {
            if (previous == null || now - previous.startedAt() >= WINDOW_SECONDS) {
                return new Bucket(new AtomicInteger(1), now);
            }
            previous.failures().incrementAndGet();
            return previous;
        });
    }

    public void clear(String email) {
        buckets.remove(normalize(email));
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
