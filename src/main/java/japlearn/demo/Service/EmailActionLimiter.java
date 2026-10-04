package japlearn.demo.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Stops anyone from flooding a person's inbox with password-reset or
 * account-deletion emails: at most one email per address every 10 minutes
 * and three per day, per action. Callers still answer with the same generic
 * message, so this never reveals whether an address is registered.
 */
@Service
public class EmailActionLimiter {
    private static final long COOLDOWN_SECONDS = 600;
    private static final int PER_DAY = 3;
    private static final int MAX_TRACKED = 50_000;

    private final Map<String, Deque<Long>> sent = new ConcurrentHashMap<>();

    /** Returns true (and records it) if an email for this action may be sent to this address now. */
    public boolean allow(String action, String email) {
        if (email == null || email.isBlank()) return false;
        String key = action + "|" + email.trim().toLowerCase(Locale.ROOT);
        long now = Instant.now().getEpochSecond();
        if (sent.size() > MAX_TRACKED) sent.entrySet().removeIf((e) -> e.getValue().isEmpty() || now - e.getValue().peekLast() > 86_400);
        Deque<Long> times = sent.computeIfAbsent(key, (k) -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > 86_400) times.pollFirst();
            if (!times.isEmpty() && now - times.peekLast() < COOLDOWN_SECONDS) return false;
            if (times.size() >= PER_DAY) return false;
            times.addLast(now);
            return true;
        }
    }
}
