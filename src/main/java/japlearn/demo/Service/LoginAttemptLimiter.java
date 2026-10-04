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
        adminFailures.remove(normalize(email));
    }

    // ---- Admin accounts: much stricter. 5 wrong passwords within 15 minutes locks the
    // account for 30 minutes (even the right password is refused while locked).
    private static final int ADMIN_MAX_FAILURES = 5;
    private static final long ADMIN_WINDOW_SECONDS = 15 * 60;
    private static final long ADMIN_LOCK_SECONDS = 30 * 60;
    private record AdminState(int failures, long windowStart, long lockedUntil) {}
    private final ConcurrentHashMap<String, AdminState> adminFailures = new ConcurrentHashMap<>();

    public boolean isAdminLocked(String email) {
        AdminState s = adminFailures.get(normalize(email));
        return s != null && Instant.now().getEpochSecond() < s.lockedUntil();
    }

    public void recordAdminFailure(String email) {
        String key = normalize(email);
        if (key.isEmpty()) return;
        long now = Instant.now().getEpochSecond();
        adminFailures.compute(key, (k, prev) -> {
            if (prev == null || (now - prev.windowStart() >= ADMIN_WINDOW_SECONDS && now >= prev.lockedUntil())) {
                return new AdminState(1, now, 0);
            }
            int failures = prev.failures() + 1;
            long lockedUntil = failures >= ADMIN_MAX_FAILURES ? Math.max(prev.lockedUntil(), now + ADMIN_LOCK_SECONDS) : prev.lockedUntil();
            return new AdminState(failures, prev.windowStart(), lockedUntil);
        });
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
