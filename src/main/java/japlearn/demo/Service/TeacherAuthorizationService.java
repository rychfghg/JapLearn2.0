package japlearn.demo.Service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import japlearn.demo.Entity.User;
import japlearn.demo.Repository.UserRepository;

@Service
public class TeacherAuthorizationService {
    private final UserRepository userRepository;

    public TeacherAuthorizationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String requireTeacher(String teacherEmail, String sessionToken) {
        if (teacherEmail == null || teacherEmail.isBlank() || sessionToken == null || sessionToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Teacher sign-in is required");
        }

        String normalizedEmail = teacherEmail.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail);
        if (user == null || !"teacher".equalsIgnoreCase(user.getRole())
                || user.getPortalSessionToken() == null
                || !java.security.MessageDigest.isEqual(user.getPortalSessionToken().getBytes(java.nio.charset.StandardCharsets.UTF_8), sessionToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                || user.getPortalSessionExpiresAt() == null
                || LocalDateTime.now(ZoneId.of("Asia/Manila")).isAfter(user.getPortalSessionExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid teacher session");
        }
        return normalizedEmail;
    }
}
