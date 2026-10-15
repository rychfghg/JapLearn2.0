package japlearn.demo.Controller;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import japlearn.demo.Entity.ReplyCoachAttempt;
import japlearn.demo.Repository.ReplyCoachAttemptRepository;
import japlearn.demo.Repository.ReplyCoachChapterRepository;
import japlearn.demo.Service.*;

@RestController
@RequestMapping("/api/reply-coach/readiness")
public class ReplyCoachReadinessController {
    private final ReplyCoachAttemptRepository attempts;
    private final ReplyCoachChapterRepository chapters;
    private final StudentAuthorizationService authorization;
    private final ReplyCoachReadiness readiness;
    private final ReplyCoachQuestionBank bank;
    public ReplyCoachReadinessController(ReplyCoachAttemptRepository attempts, ReplyCoachChapterRepository chapters,
            StudentAuthorizationService authorization, ReplyCoachReadiness readiness, ReplyCoachQuestionBank bank) {
        this.attempts = attempts; this.chapters = chapters; this.authorization = authorization; this.readiness = readiness; this.bank = bank;
    }
    private ReplyCoachAttempt own(String id, String email, String token) {
        authorization.requireOwnStudent(email, token, true);
        var attempt = attempts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (email == null || !email.trim().equalsIgnoreCase(attempt.getEmail())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return attempt;
    }
    @GetMapping
    public Map<String, Object> get(@RequestParam String attemptId, @RequestParam String email,
            @RequestHeader(value = "X-Student-Token", required = false) String token) {
        return readiness.view(own(attemptId, email, token));
    }
    @PostMapping
    public synchronized Map<String, Object> update(@RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Student-Token", required = false) String token) {
        var attempt = own(request.get("attemptId"), request.get("email"), token);
        try {
            String action = request.getOrDefault("action", "");
            if ("practice".equals(action)) return practice(attempt, request);
            if ("start".equals(action)) {
                var chapter = chapters.findById(attempt.getChapterId()).orElseThrow();
                var required = chapter.getNodes().stream().filter(n -> "CHOICE".equals(n.getType())).map(n -> n.getId()).toList();
                var answered = attempt.getAnswers().stream().map(a -> a.getNodeId()).toList();
                if (required.isEmpty() || !answered.containsAll(required)) throw new IllegalArgumentException("Finish the chapter decisions first.");
                readiness.start(attempt);
            } else {
                var run = readiness.latest(attempt);
                if (run == null || !run.id.equals(request.get("runId"))) throw new IllegalArgumentException("Reopen your current readiness check.");
                if ("support".equals(action)) readiness.support(run, request.get("questionId"), request.get("helpType"));
                else if ("answer".equals(action)) readiness.answer(run, request.get("questionId"), request.get("choiceId"));
                else throw new IllegalArgumentException("Unknown readiness action.");
            }
            attempt.setUpdatedAt(Instant.now()); attempts.save(attempt);
            return readiness.view(attempt);
        } catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); }
    }
    private Map<String, Object> practice(ReplyCoachAttempt attempt, Map<String, String> request) {
        if (!ReplyCoachReadiness.CHAPTER.equals(attempt.getChapterTitle())) throw new IllegalArgumentException("No practice bank is assigned to this chapter.");
        var q = bank.get(request.getOrDefault("questionId", ""));
        if (!q.id().startsWith("practice-")) throw new IllegalArgumentException("Not a practice example.");
        var run = readiness.latest(attempt);
        boolean targeted = "targeted".equals(request.get("mode"));
        if (targeted && (run == null || run.completedAt == null)) throw new IllegalArgumentException("Finish the readiness check first.");
        if (!targeted && attempt.getAnswers().stream().noneMatch(a -> ("decision-" + q.practiceNumber()).equals(a.getNodeId())))
            throw new IllegalArgumentException("Complete the related chapter decision first.");
        String key = (targeted ? run.id : "chapter") + ":" + q.id();
        var existing = attempt.getPracticeResults().stream().filter(p -> key.equals(p.key)).findFirst();
        if (existing.isPresent()) return Map.of("saved", true, "correct", existing.get().correct);
        var selected = q.choices().stream().filter(c -> c.japanese().equals(request.get("selectedJapanese"))).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid practice choice."));
        var result = new ReplyCoachAttempt.PracticeResult();
        result.key = key; result.questionId = q.id(); result.correct = q.correctId().equals(selected.id());
        for (String help : request.getOrDefault("helpTypes", "").split(","))
            if (Set.of("romaji", "meaning").contains(help) && !result.helpTypes.contains(help)) result.helpTypes.add(help);
        attempt.getPracticeResults().add(result); attempt.setUpdatedAt(Instant.now()); attempts.save(attempt);
        return Map.of("saved", true, "correct", result.correct);
    }
}
