package japlearn.demo.Config;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.annotation.Order;
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
    private static final Map<String, int[]> SENSITIVE_LIMITS = Map.of(
        "/api/users/login", new int[]{10, 60},
        "/api/users/register", new int[]{6, 60},
        "/api/users/register-teacher", new int[]{6, 60},
        "/api/users/forgot-password", new int[]{5, 60},
        "/api/users/reset-password", new int[]{10, 60}
    );

    // Generous fallback so normal gameplay traffic (lesson/score/progress
    // fetches) is never affected — this only catches scripted floods.
    private static final int[] DEFAULT_LIMIT = {200, 60};

    private static final int MAX_TRACKED_KEYS = 20_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/")) {
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
        // Render terminates TLS in front of the app and forwards the real
        // client IP via X-Forwarded-For; fall back to the socket address for
        // local/dev runs where no proxy is present.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
