package japlearn.demo.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import japlearn.demo.Entity.User;
import japlearn.demo.Repository.UserRepository;

@Service
public class StudentAuthorizationService {
    private final UserRepository users;

    public StudentAuthorizationService(UserRepository users) { this.users = users; }

    public String requireStudent(String email, String token) {
        if (email == null || email.isBlank() || token == null || token.isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Student sign-in is required");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(normalized);
        if (user == null || !"student".equalsIgnoreCase(user.getRole())
                || user.getPortalSessionToken() == null || user.getPortalSessionExpiresAt() == null
                || !MessageDigest.isEqual(user.getPortalSessionToken().getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8))
                || LocalDateTime.now(ZoneId.of("Asia/Manila")).isAfter(user.getPortalSessionExpiresAt()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid student session");
        return normalized;
    }
}
