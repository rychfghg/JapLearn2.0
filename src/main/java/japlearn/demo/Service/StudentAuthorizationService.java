package japlearn.demo.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import japlearn.demo.Entity.User;
import japlearn.demo.Repository.UserRepository;

@Service
public class StudentAuthorizationService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Manila");
    /** Students stay signed in on the app for days, so their session is long and renewed on use. */
    public static final int STUDENT_SESSION_DAYS = 30;

    private final UserRepository users;

    /**
     * false (transition): requests without a token are still accepted so older app builds keep working;
     * a token that does not belong to the account is always rejected.
     * true (strict): every request must carry the student's own valid token.
     */
    @Value("${app.student-token.required:false}")
    private boolean tokenRequired;

    public StudentAuthorizationService(UserRepository users) { this.users = users; }

    public String requireStudent(String email, String token) {
        if (email == null || email.isBlank() || token == null || token.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Student sign-in is required");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(normalized);
        if (user == null || !"student".equalsIgnoreCase(user.getRole()) || !tokenMatches(user, token)
                || LocalDateTime.now(ZONE).isAfter(user.getPortalSessionExpiresAt()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid student session");
        renew(user);
        return normalized;
    }

    /**
     * Guards a student's own progress routes: the caller must be signed in as that same student.
     * While the transition flag is off, a request with no token is let through (older app builds),
     * and a matching-but-expired token is renewed instead of rejected.
     */
    public void requireOwnStudent(String email, String token) {
        boolean hasToken = token != null && !token.isBlank();
        if (!hasToken) {
            if (tokenRequired) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Student sign-in is required");
            return;
        }
        if (email == null || email.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        User user = users.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (user == null || !"student".equalsIgnoreCase(user.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid student session");
        if (tokenMatches(user, token)) {
            if (tokenRequired && LocalDateTime.now(ZONE).isAfter(user.getPortalSessionExpiresAt()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid student session");
            renew(user);
            return;
        }
        // Token does not match. Strict: reject. Transition: reject only when the account has a
        // different, still-valid session (someone else's token); a session cleared by a password
        // change or an expired one still works until the student signs in again.
        boolean accountHasLiveSession = user.getPortalSessionToken() != null && user.getPortalSessionExpiresAt() != null
                && LocalDateTime.now(ZONE).isBefore(user.getPortalSessionExpiresAt());
        if (tokenRequired || accountHasLiveSession)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid student session");
    }

    private boolean tokenMatches(User user, String token) {
        return user.getPortalSessionToken() != null && user.getPortalSessionExpiresAt() != null
                && MessageDigest.isEqual(user.getPortalSessionToken().getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8));
    }

    // Sliding expiry, written at most about once a day per student.
    private void renew(User user) {
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (user.getPortalSessionExpiresAt().isBefore(now.plusDays(STUDENT_SESSION_DAYS - 1))) {
            user.setPortalSessionExpiresAt(now.plusDays(STUDENT_SESSION_DAYS));
            users.save(user);
        }
    }
}
