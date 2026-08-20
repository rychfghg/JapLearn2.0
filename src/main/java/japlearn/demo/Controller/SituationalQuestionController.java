package japlearn.demo.Controller;

import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.SituationalAttempt;
import japlearn.demo.Entity.SituationalQuestion;
import japlearn.demo.Repository.SituationalAttemptRepository;
import japlearn.demo.Repository.SituationalQuestionRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/situational")
public class SituationalQuestionController {
    private final SituationalQuestionRepository questions;
    private final SituationalAttemptRepository attempts;

    public SituationalQuestionController(SituationalQuestionRepository questions,
            SituationalAttemptRepository attempts) {
        this.questions = questions;
        this.attempts = attempts;
    }

    @GetMapping("/questions")
    public List<SituationalQuestion> getQuestions(
            @RequestParam(defaultValue = "RECOGNITION") String gameType,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return activeOnly
                ? questions.findByGameTypeIgnoreCaseAndActiveTrueOrderByOrderAsc(gameType)
                : questions.findByGameTypeIgnoreCaseOrderByOrderAsc(gameType);
    }

    @PostMapping("/questions")
    public SituationalQuestion create(@RequestBody SituationalQuestion question) {
        question.setId(null);
        return questions.save(question);
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<SituationalQuestion> update(@PathVariable String id,
            @RequestBody SituationalQuestion question) {
        if (!questions.existsById(id)) return ResponseEntity.notFound().build();
        question.setId(id);
        return ResponseEntity.ok(questions.save(question));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!questions.existsById(id)) return ResponseEntity.notFound().build();
        questions.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/attempts")
    public ResponseEntity<SituationalAttempt> saveAttempt(@RequestBody SituationalAttempt attempt) {
        if (attempt.getEmail() == null || attempt.getEmail().isBlank()
                || attempt.getGameType() == null || attempt.getGameType().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        attempt.setId(null);
        attempt.setCompletedAt(Instant.now());
        attempt.setWrongAnswers(Math.max(0, attempt.getTotalQuestions() - attempt.getCorrectAnswers()));
        attempt.setAccuracy(attempt.getTotalQuestions() == 0 ? 0
                : Math.round((attempt.getCorrectAnswers() * 10000.0) / attempt.getTotalQuestions()) / 100.0);
        return ResponseEntity.ok(attempts.save(attempt));
    }

    @GetMapping("/attempts")
    public List<SituationalAttempt> getAttempts(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String gameType) {
        List<SituationalAttempt> result = email == null || email.isBlank()
                ? attempts.findAll()
                : attempts.findByEmailIgnoreCaseOrderByCompletedAtDesc(email);
        return gameType == null || gameType.isBlank() ? result : result.stream()
                .filter(item -> gameType.equalsIgnoreCase(item.getGameType())).toList();
    }

    @GetMapping("/best")
    public ResponseEntity<SituationalAttempt> getBest(@RequestParam String email,
            @RequestParam(defaultValue = "RECOGNITION") String gameType) {
        return attempts.findTopByEmailIgnoreCaseAndGameTypeIgnoreCaseOrderByScoreDesc(email, gameType)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/expression-match/progress")
    public Map<String, Object> expressionMatchProgress(@RequestParam String email) {
        List<SituationalAttempt> records = attempts.findByEmailIgnoreCaseOrderByCompletedAtDesc(email).stream()
                .filter(item -> "EXPRESSION_MATCH".equalsIgnoreCase(item.getGameType())).toList();
        List<String> completedSets = records.stream().filter(SituationalAttempt::isCompleted)
                .map(item -> item.getLevel() + "-" + item.getSetNumber()).distinct().toList();
        int highestCompletedLevel = 0;
        int[] requiredSets = {0, 3, 3, 3, 5, 10};
        for (int level = 1; level <= 5; level++) {
            final int currentLevel = level;
            long cleared = completedSets.stream().filter(key -> key.startsWith(currentLevel + "-")).count();
            if (cleared < requiredSets[level]) break;
            highestCompletedLevel = level;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unlockedLevel", Math.min(5, Math.max(1, highestCompletedLevel + 1)));
        result.put("highestCompletedLevel", highestCompletedLevel);
        result.put("completedSets", completedSets);
        result.put("bestScore", records.stream().mapToInt(SituationalAttempt::getScore).max().orElse(0));
        result.put("attempts", records.size());
        return result;
    }
}
