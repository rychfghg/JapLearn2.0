package japlearn.demo.Controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.Score;
import japlearn.demo.Service.QuackslateSessionService;
import japlearn.demo.Service.StudentAuthorizationService;

/** App-facing status derives from backend UTC time, even after a Render restart. */
@RestController
@RequestMapping("/api/quackslate/session")
public class QuackslateScheduledSessionController {
    private final QuackslateSessionService sessions;
    private final StudentAuthorizationService students;

    public QuackslateScheduledSessionController(QuackslateSessionService sessions,
            StudentAuthorizationService students) {
        this.sessions = sessions;
        this.students = students;
    }

    @GetMapping("/{code}")
    public QuackslateSessionService.SessionView status(@PathVariable String code) {
        return sessions.publicView(code);
    }

    @PostMapping("/{code}/join")
    public QuackslateSessionService.SessionView join(@PathVariable String code,
            @RequestHeader("X-Student-Token") String token, @RequestBody Map<String, String> body) {
        return sessions.join(code, students.requireStudent(body.get("email"), token));
    }

    @PostMapping("/{code}/score")
    public Score score(@PathVariable String code, @RequestHeader("X-Student-Token") String token,
            @RequestBody Score submitted) {
        submitted.setEmail(students.requireStudent(submitted.getEmail(), token));
        return sessions.saveScore(code, submitted);
    }
}
