package japlearn.demo.Config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.UserRepository;

/** Enforces the portal role at the API boundary, independently of the React route guard. */
@Component
@Order(2)
public class PortalAuthorizationFilter extends OncePerRequestFilter {
    private static final ZoneId ZONE = ZoneId.of("Asia/Manila");
    private static final List<String> MANAGED_CONTENT_PREFIXES = List.of(
        "/api/lesson", "/api/lessonPage", "/api/lessonContent",
        "/api/DatabankLesson", "/api/DatabankLessonPage", "/api/DatabankLessonContent",
        "/api/quackamolecontent", "/api/quackmancontent", "/api/quackslateContent",
        "/api/quackslate/question-bank"
    );

    private final UserRepository users;

    public PortalAuthorizationFilter(UserRepository users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        boolean adminOnly = isAdminOnly(request.getMethod(), path);
        boolean portalWrite = isPortalManagedWrite(request.getMethod(), path);
        if (!adminOnly && !portalWrite) { chain.doFilter(request, response); return; }

        String token = firstNonBlank(request.getHeader("X-Portal-Token"), request.getHeader("X-Teacher-Token"));
        User user = token == null ? null : users.findByPortalSessionToken(token);
        boolean valid = user != null && user.getPortalSessionToken() != null
                && MessageDigest.isEqual(user.getPortalSessionToken().getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))
                && user.getPortalSessionExpiresAt() != null
                && LocalDateTime.now(ZONE).isBefore(user.getPortalSessionExpiresAt());
        boolean permittedRole = valid && ("admin".equalsIgnoreCase(user.getRole())
                || (!adminOnly && "teacher".equalsIgnoreCase(user.getRole())));
        if (!permittedRole) {
            response.setStatus(valid ? 403 : 401);
            response.setContentType("application/json");
            response.getWriter().write(valid ? "{\"error\":\"Administrator access is required.\"}"
                    : "{\"error\":\"Your portal session is missing or expired. Please sign in again.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isAdminOnly(String method, String path) {
        if (path == null) return false;
        if (path.equals("/api/users/login") || path.equals("/api/users/register")
                || path.equals("/api/users/register-teacher") || path.equals("/api/users/forgot-password")
                || path.equals("/api/users/reset-password") || path.equals("/api/users/confirm")
                || path.startsWith("/api/users/daily-goal/")) return false;
        if (path.equals("/api/users") && HttpMethod.GET.matches(method)) return true;
        if (path.startsWith("/api/users/pending-approval") || path.startsWith("/api/users/approve/")
                || path.startsWith("/api/users/admin-create")) return true;
        if (path.matches("/api/users/[^/]+(/guided-phrase-access)?")
                && !HttpMethod.GET.matches(method)) return true;
        return path.equals("/api/quackTalkSessions/all")
                || path.equals("/api/dialogue-relay/bonus/assessments");
    }

    private boolean isPortalManagedWrite(String method, String path) {
        if (path == null || HttpMethod.GET.matches(method) || HttpMethod.OPTIONS.matches(method)) return false;
        if (MANAGED_CONTENT_PREFIXES.stream().anyMatch(path::startsWith)) return true;
        if (path.startsWith("/api/situational/media") || path.startsWith("/api/situational/questions")) return true;
        if (path.startsWith("/api/reply-coach/media") || path.startsWith("/api/reply-coach/chapters")) return true;
        return path.startsWith("/api/quackslateLevels/generateGameCode")
                || path.startsWith("/api/quackslateLevels/startQuiz")
                || path.startsWith("/api/quackslateLevels/setCurrentQuestionIndex");
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first.trim();
        return second == null || second.isBlank() ? null : second.trim();
    }
}
