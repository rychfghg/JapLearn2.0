package japlearn.demo.Controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.QuackslateQuestion;
import japlearn.demo.Service.QuackslateSessionService;
import japlearn.demo.Service.TeacherAuthorizationService;

@RestController
@RequestMapping("/api/teacher/quackslate")
public class TeacherQuackslateSessionController {
    private final TeacherAuthorizationService authorization;
    private final QuackslateSessionService sessions;

    public TeacherQuackslateSessionController(TeacherAuthorizationService authorization,
            QuackslateSessionService sessions) {
        this.authorization = authorization;
        this.sessions = sessions;
    }

    public record ScheduleRequest(Instant startsAt, Instant endsAt) {}

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> validationError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",
                error.getReason() == null ? "The request could not be completed." : error.getReason()));
    }

    @DeleteMapping("/sessions/{code}")
    public ResponseEntity<Void> delete(@RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String token, @PathVariable String code) {
        sessions.deleteSession(authorization.requireTeacher(teacherEmail, token), code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/questions")
    public List<QuackslateQuestion> questions(@RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String token) {
        return sessions.availableQuestions(authorization.requireTeacher(teacherEmail, token));
    }

    @PostMapping("/questions")
    public QuackslateQuestion addQuestion(@RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String token,
            @RequestBody QuackslateQuestion question) {
        return sessions.addQuestion(authorization.requireTeacher(teacherEmail, token), question);
    }

    @GetMapping("/sessions")
    public List<QuackslateSessionService.SessionView> list(@RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String token) {
        return sessions.list(authorization.requireTeacher(teacherEmail, token));
    }

    @PostMapping("/sessions")
    public QuackslateSessionService.SessionView create(@RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String token) {
        return sessions.view(sessions.create(authorization.requireTeacher(teacherEmail, token)));
    }

    @GetMapping("/sessions/{code}")
    public Map<String, Object> details(@RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String token, @PathVariable String code) {
        var game = sessions.owned(authorization.requireTeacher(teacherEmail, token), code);
        return Map.of("session", sessions.view(game), "questionIds", game.getQuestionIds());
    }

    @PutMapping("/sessions/{code}/questions")
    public QuackslateSessionService.SessionView selectQuestions(@RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String token,
            @PathVariable String code, @RequestBody List<String> questionIds) {
        return sessions.setQuestions(authorization.requireTeacher(teacherEmail, token), code, questionIds);
    }

    @PostMapping("/sessions/{code}/schedule")
    public QuackslateSessionService.SessionView schedule(@RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String token,
            @PathVariable String code, @RequestBody ScheduleRequest request) {
        return sessions.publish(authorization.requireTeacher(teacherEmail, token), code,
                request.startsAt(), request.endsAt());
    }

    @GetMapping("/sessions/{code}/sheet")
    public QuackslateSessionService.ScoreSheet scoreSheet(@RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String token, @PathVariable String code) {
        return sessions.sheet(authorization.requireTeacher(teacherEmail, token), code);
    }
}
