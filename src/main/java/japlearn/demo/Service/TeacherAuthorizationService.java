package japlearn.demo.Service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
                || !user.getPortalSessionToken().equals(sessionToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid teacher session");
        }
        return normalizedEmail;
    }
}
