package japlearn.demo.Controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.QuackTalkSession;
import japlearn.demo.Repository.QuackTalkSessionRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/quackTalkSessions")
public class QuackTalkSessionController {
    private final QuackTalkSessionRepository sessions;

    public QuackTalkSessionController(QuackTalkSessionRepository sessions) {
        this.sessions = sessions;
    }

    @PostMapping("/record")
    public ResponseEntity<?> record(@RequestBody QuackTalkSession session) {
        if (session.getEmail() == null || session.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Student email is required.");
        }

        if (session.getRoomType() == null || session.getRoomType().isBlank()) {
            return ResponseEntity.badRequest().body("Practice room is required.");
        }

        session.setId(null);
        session.setDurationSeconds(Math.max(1, Math.min(session.getDurationSeconds(), 3600)));
        session.setCompleted(true);
        session.setEvaluated(false);
        session.setScore(null);
        session.setPracticedAt(Instant.now());
        return ResponseEntity.ok(sessions.save(session));
    }

    @GetMapping
    public List<QuackTalkSession> getStudentSessions(@RequestParam String email) {
        return sessions.findByEmailIgnoreCaseOrderByPracticedAtDesc(email);
    }

    @GetMapping("/all")
    public List<QuackTalkSession> getAllSessions() {
        return sessions.findAllByOrderByPracticedAtDesc();
    }
}
