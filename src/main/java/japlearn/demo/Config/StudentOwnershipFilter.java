package japlearn.demo.Config;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
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
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.UserRepository;
import japlearn.demo.Service.StudentAuthorizationService;
import japlearn.demo.Service.StudentService;

/**
 * One place that protects every student's learning data, for reads AND writes.
 *
 * For each route below it finds which student the request is about (query, path or JSON body)
 * and allows it only for:
 *   - that same student (their own X-Student-Token), or
 *   - the teacher of that student's class (portal / teacher token), or
 *   - an admin.
 * An email address alone is never enough to READ a student's data.
 * Writes from the student app follow the STUDENT_TOKEN_REQUIRED rollout switch.
 */
@Component
@Order(20)
public class StudentOwnershipFilter extends OncePerRequestFilter {

    private enum Source { QUERY, BODY, PATH }
    private record Rule(String method, Pattern path, Source source, boolean read) {}

    private static final List<Rule> RULES = List.of(
        // ---- writes (saving a student's own data)
        new Rule("POST", Pattern.compile("^/api/scores/(save|high-score)$"), Source.BODY, false),
        new Rule("PUT", Pattern.compile("^/api/response-rush/progress$"), Source.QUERY, false),
        new Rule("PUT", Pattern.compile("^/api/dialogue-relay/progress$"), Source.QUERY, false),
        new Rule("DELETE", Pattern.compile("^/api/dialogue-relay/progress$"), Source.QUERY, false),
        new Rule("POST", Pattern.compile("^/api/situational/attempts$"), Source.BODY, false),
        new Rule("PUT", Pattern.compile("^/api/situational/runs/current$"), Source.BODY, false),
        new Rule("POST", Pattern.compile("^/api/reply-coach/attempts/start$"), Source.BODY, false),
        new Rule("POST", Pattern.compile("^/api/quackTalkSessions/record$"), Source.BODY, false),
        new Rule("POST", Pattern.compile("^/api/progress/([^/]+)$"), Source.PATH, false),
        new Rule("POST", Pattern.compile("^/api/talk-with-sumi/(live-token|complete|reserve-response|release-response)$"), Source.BODY, false),
        new Rule("POST", Pattern.compile("^/api/talk-with-sumi/assess$"), Source.QUERY, false),
        new Rule("POST", Pattern.compile("^/api/guided-phrase/(live-token|complete)$"), Source.BODY, false),
        new Rule("POST", Pattern.compile("^/api/guided-phrase/assess$"), Source.QUERY, false),
        new Rule("POST", Pattern.compile("^/api/dialogue-relay/bonus/assess$"), Source.QUERY, false),
        new Rule("POST", Pattern.compile("^/api/students/joinClass$"), Source.QUERY, false),
        // ---- reads (a student's scores, progress, history, analytics)
        new Rule("GET", Pattern.compile("^/api/progress/(?!teacher$)([^/]+)$"), Source.PATH, true),
        new Rule("GET", Pattern.compile("^/api/scores/(student|high-score)$"), Source.QUERY, true),
        new Rule("GET", Pattern.compile("^/api/quackTalkSessions$"), Source.QUERY, true),
        new Rule("GET", Pattern.compile("^/api/communicationAnalytics/getStudentAnalytics$"), Source.QUERY, true),
        new Rule("GET", Pattern.compile("^/api/quackProgress/(analytics|progression)$"), Source.QUERY, true),
        new Rule("GET", Pattern.compile("^/api/guided-phrase/(history|status)$"), Source.QUERY, true),
        new Rule("GET", Pattern.compile("^/api/reply-coach/(attempts|progress)$"), Source.QUERY, true),
        new Rule("GET", Pattern.compile("^/api/situational/(attempts|best|runs/current|expression-match/progress)$"), Source.QUERY, true),
        new Rule("GET", Pattern.compile("^/api/response-rush/progress$"), Source.QUERY, true),
        new Rule("GET", Pattern.compile("^/api/students/getStudentByEmail$"), Source.QUERY, true)
    );

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ZoneId ZONE = ZoneId.of("Asia/Manila");

    private final StudentAuthorizationService students;
    private final UserRepository users;
    private final StudentService studentService;

    public StudentOwnershipFilter(StudentAuthorizationService students, UserRepository users, StudentService studentService) {
        this.students = students;
        this.users = users;
        this.studentService = studentService;
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

        // Reads with no student named (e.g. an empty lookup) return nothing anyway.
        if (rule.read() && (email == null || email.isBlank())) { chain.doFilter(forward, response); return; }

        // Staff path: admin sees any student; a teacher only students in their own classes.
        String staffToken = firstNonBlank(request.getHeader("X-Portal-Token"), request.getHeader("X-Teacher-Token"));
        if (staffToken != null) {
            if (staffMayAccess(staffToken, email)) { chain.doFilter(forward, response); return; }
            deny(response, 403);
            return;
        }

        // Student path: their own token. Reads always need it; writes follow the rollout switch.
        try {
            students.requireOwnStudent(email, request.getHeader("X-Student-Token"), rule.read());
        } catch (ResponseStatusException e) {
            deny(response, e.getStatusCode().value());
            return;
        }
        chain.doFilter(forward, response);
    }

    private boolean staffMayAccess(String token, String studentEmail) {
        User staff = users.findByPortalSessionToken(token);
        boolean valid = staff != null && staff.getPortalSessionToken() != null && staff.getPortalSessionExpiresAt() != null
                && MessageDigest.isEqual(staff.getPortalSessionToken().getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))
                && LocalDateTime.now(ZONE).isBefore(staff.getPortalSessionExpiresAt());
        if (!valid) return false;
        String role = String.valueOf(staff.getRole()).toLowerCase(Locale.ROOT);
        if (role.equals("admin")) return true;
        if (!role.equals("teacher") || !staff.isApproved()) return false;
        String target = studentEmail == null ? "" : studentEmail.trim().toLowerCase(Locale.ROOT);
        return studentService.getStudentsForTeacher(staff.getEmail()).stream()
                .anyMatch((s) -> s.getEmail() != null && s.getEmail().trim().equalsIgnoreCase(target));
    }

    private static void deny(HttpServletResponse response, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Please sign in again to continue.\"}");
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        return b == null || b.isBlank() ? null : b.trim();
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
