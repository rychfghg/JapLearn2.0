package japlearn.demo.Config;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import japlearn.demo.Service.StudentAuthorizationService;

/**
 * One place that checks a student can only write THEIR OWN learning data.
 * For every route below it finds the student email (query, path or JSON body)
 * and verifies it against the X-Student-Token sent by the app.
 * Uses StudentAuthorizationService.requireOwnStudent, so it follows the
 * STUDENT_TOKEN_REQUIRED switch (transition mode: missing token allowed for old
 * app builds, but a token belonging to someone else is always rejected).
 */
@Component
@Order(20)
public class StudentOwnershipFilter extends OncePerRequestFilter {

    private enum Source { QUERY, BODY, PATH }
    private record Rule(String method, Pattern path, Source source) {}

    private static final List<Rule> RULES = List.of(
        new Rule("POST", Pattern.compile("^/api/scores/(save|high-score)$"), Source.BODY),
        new Rule("POST", Pattern.compile("^/api/users/daily-goal/(minute|complete)$"), Source.BODY),
        new Rule("PUT", Pattern.compile("^/api/response-rush/progress$"), Source.QUERY),
        new Rule("POST", Pattern.compile("^/api/situational/attempts$"), Source.BODY),
        new Rule("PUT", Pattern.compile("^/api/situational/runs/current$"), Source.BODY),
        new Rule("POST", Pattern.compile("^/api/reply-coach/attempts/start$"), Source.BODY),
        new Rule("POST", Pattern.compile("^/api/quackTalkSessions/record$"), Source.BODY),
        new Rule("POST", Pattern.compile("^/api/progress/([^/]+)$"), Source.PATH),
        new Rule("POST", Pattern.compile("^/api/talk-with-sumi/(live-token|complete|reserve-response)$"), Source.BODY),
        new Rule("POST", Pattern.compile("^/api/guided-phrase/(live-token|complete)$"), Source.BODY),
        new Rule("POST", Pattern.compile("^/api/dialogue-relay/bonus/assess$"), Source.QUERY),
        new Rule("POST", Pattern.compile("^/api/students/joinClass$"), Source.QUERY)
    );

    private static final ObjectMapper JSON = new ObjectMapper();
    private final StudentAuthorizationService students;

    public StudentOwnershipFilter(StudentAuthorizationService students) {
        this.students = students;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        Rule rule = null; Matcher match = null;
        for (Rule r : RULES) {
            if (!r.method().equalsIgnoreCase(request.getMethod())) continue;
            Matcher m = r.path().matcher(path == null ? "" : path);
            if (m.matches()) { rule = r; match = m; break; }
        }
        if (rule == null) { chain.doFilter(request, response); return; }
        // A teacher adding a student to their own class is checked by the teacher token instead.
        if (path.equals("/api/students/joinClass") && request.getParameter("teacherEmail") != null) { chain.doFilter(request, response); return; }

        HttpServletRequest forward = request;
        String email;
        if (rule.source() == Source.QUERY) {
            email = request.getParameter("email");
        } else if (rule.source() == Source.PATH) {
            email = java.net.URLDecoder.decode(match.group(1), StandardCharsets.UTF_8);
        } else {
            byte[] body = request.getInputStream().readAllBytes();
            forward = new CachedBody(request, body); // let the controller read the body again
            email = null;
            try {
                JsonNode node = body.length == 0 ? null : JSON.readTree(body);
                if (node != null && node.hasNonNull("email")) email = node.get("email").asText();
            } catch (Exception ignored) {
                // Not JSON: the controller will reject it.
            }
        }

        try {
            students.requireOwnStudent(email, request.getHeader("X-Student-Token"));
        } catch (ResponseStatusException e) {
            response.setStatus(e.getStatusCode().value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Please sign in again to continue.\"}");
            return;
        }
        chain.doFilter(forward, response);
    }

    /** Request wrapper that replays an already-read body. */
    private static final class CachedBody extends HttpServletRequestWrapper {
        private final byte[] body;
        CachedBody(HttpServletRequest request, byte[] body) { super(request); this.body = body; }
        @Override public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return in.read(); }
                @Override public int read(byte[] b, int off, int len) { return in.read(b, off, len); }
                @Override public boolean isFinished() { return in.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { }
            };
        }
        @Override public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
        @Override public int getContentLength() { return body.length; }
        @Override public long getContentLengthLong() { return body.length; }
    }
}
