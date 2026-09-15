package japlearn.demo.Config;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A small, dependency-free per-IP rate limiter.
 *
 * <p>This is an application-layer speed bump against credential stuffing,
 * brute force, and scripted abuse of the auth endpoints, plus a generous
 * floor under every other /api/** route. It is NOT a substitute for network
 * level DDoS protection — a real volumetric flood has to be stopped by a
 * CDN/WAF sitting in front of Render (e.g. Cloudflare), because it can
 * saturate the network/connection layer before a single byte reaches this
 * filter. This only helps once a request actually lands on the JVM.</p>
 */
@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final class Bucket {
        final AtomicInteger count = new AtomicInteger(0);
        final long windowStartEpochSeconds;

        Bucket(long windowStartEpochSeconds) {
            this.windowStartEpochSeconds = windowStartEpochSeconds;
        }
    }

    // path -> {requests allowed, window length in seconds}
    private static final Map<String, int[]> SENSITIVE_LIMITS = Map.ofEntries(
        Map.entry("/api/users/login", new int[]{10, 60}),
        Map.entry("/api/users/register", new int[]{6, 60}),
        Map.entry("/api/users/register-teacher", new int[]{6, 60}),
        Map.entry("/api/users/forgot-password", new int[]{5, 60}),
        Map.entry("/api/users/reset-password", new int[]{10, 60}),
        Map.entry("/api/dialogue-relay/bonus/assess", new int[]{10, 60}),
        Map.entry("/api/guided-phrase/live-token", new int[]{10, 60}),
        Map.entry("/api/guided-phrase/assess", new int[]{10, 60}),
        Map.entry("/api/talk-with-sumi/live-token", new int[]{10, 60}),
        Map.entry("/api/talk-with-sumi/assess", new int[]{10, 60}),
        Map.entry("/api/talk-with-sumi/reserve-response", new int[]{15, 60})
    );

    // Keep ordinary gameplay and queued offline-score synchronization usable
    // for classrooms whose devices share one public IP. Expensive speech and
    // authentication routes retain the much tighter limits above.
    private static final int[] DEFAULT_LIMIT = {600, 60};

    private static final int MAX_TRACKED_KEYS = 20_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (HttpMethod.OPTIONS.matches(request.getMethod()) || path == null || !path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        int[] limit = SENSITIVE_LIMITS.getOrDefault(path, DEFAULT_LIMIT);
        String bucketKey = clientIp(request) + "|" + (SENSITIVE_LIMITS.containsKey(path) ? path : "*");

        long nowSeconds = Instant.now().getEpochSecond();
        evictStaleBucketsIfNeeded(nowSeconds);

        Bucket bucket = buckets.compute(bucketKey, (key, existing) ->
            (existing == null || nowSeconds - existing.windowStartEpochSeconds >= limit[1])
                ? new Bucket(nowSeconds)
                : existing
        );

        if (bucket.count.incrementAndGet() > limit[0]) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(limit[1]));
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please slow down and try again shortly.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void evictStaleBucketsIfNeeded(long nowSeconds) {
        if (buckets.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        buckets.entrySet().removeIf(entry -> nowSeconds - entry.getValue().windowStartEpochSeconds > 600);
    }

    private String clientIp(HttpServletRequest request) {
        // server.forward-headers-strategy=native delegates trusted-proxy
        // parsing to Tomcat. Do not parse a client-supplied X-Forwarded-For
        // value here because it can be forged when the origin is contacted
        // directly.
        String remote = request.getRemoteAddr();
        return remote == null || remote.isBlank() ? "unknown" : remote;
    }
}
