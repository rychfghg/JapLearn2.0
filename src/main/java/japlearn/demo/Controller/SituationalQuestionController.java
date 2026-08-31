package japlearn.demo.Controller;

import java.time.Instant;
import java.io.IOException;
import java.util.List;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.client.gridfs.model.GridFSFile;

import japlearn.demo.Entity.SituationalAttempt;
import japlearn.demo.Entity.SituationalQuestion;
import japlearn.demo.Entity.SituationalRun;
import japlearn.demo.Repository.SituationalAttemptRepository;
import japlearn.demo.Repository.SituationalQuestionRepository;
import japlearn.demo.Repository.SituationalRunRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/situational")
public class SituationalQuestionController {
    private final SituationalQuestionRepository questions;
    private final SituationalAttemptRepository attempts;
    private final SituationalRunRepository runs;
    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations gridFsOperations;

    public SituationalQuestionController(SituationalQuestionRepository questions,
            SituationalAttemptRepository attempts,
            SituationalRunRepository runs,
            GridFsTemplate gridFsTemplate,
            GridFsOperations gridFsOperations) {
        this.questions = questions;
        this.attempts = attempts;
        this.runs = runs;
        this.gridFsTemplate = gridFsTemplate;
        this.gridFsOperations = gridFsOperations;
    }

    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadMedia(@RequestPart("file") MultipartFile file)
            throws IOException {
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        if (!contentType.startsWith("image/") && !contentType.startsWith("audio/")) {
            return ResponseEntity.badRequest().build();
        }
        Object id = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), contentType);
        return ResponseEntity.ok(Map.of(
                "url", "/api/situational/media/" + id,
                "contentType", contentType));
    }

    @GetMapping("/media/{id}")
    public ResponseEntity<byte[]> getMedia(@PathVariable String id) throws IOException {
        GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(id)));
        if (file == null) return ResponseEntity.notFound().build();
        var resource = gridFsOperations.getResource(file);
        String contentType = resource.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : resource.getContentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.noCache())
                .body(resource.getInputStream().readAllBytes());
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
        normalizeRecognitionScore(attempt);
        return ResponseEntity.ok(attempts.save(attempt));
    }

    @GetMapping("/attempts")
    public List<SituationalAttempt> getAttempts(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String gameType) {
        List<SituationalAttempt> result = email == null || email.isBlank()
                ? attempts.findAll()
                : attempts.findByEmailIgnoreCaseOrderByCompletedAtDesc(email);
        return (gameType == null || gameType.isBlank() ? result : result.stream()
                .filter(item -> gameType.equalsIgnoreCase(item.getGameType())).toList()).stream()
                .map(this::normalizeRecognitionScore)
                .toList();
    }

    @GetMapping("/best")
    public ResponseEntity<SituationalAttempt> getBest(@RequestParam String email,
            @RequestParam(defaultValue = "RECOGNITION") String gameType) {
        return attempts.findByEmailIgnoreCaseOrderByCompletedAtDesc(email).stream()
                .filter(item -> gameType.equalsIgnoreCase(item.getGameType()))
                .map(this::normalizeRecognitionScore)
                .max(Comparator.comparingInt(SituationalAttempt::getScore))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private SituationalAttempt normalizeRecognitionScore(SituationalAttempt attempt) {
        if ("RECOGNITION".equalsIgnoreCase(attempt.getGameType())) {
            attempt.setScore(attempt.getCorrectAnswers() * 10);
            attempt.setMaxScore(attempt.getTotalQuestions() * 10);
        } else if (attempt.getMaxScore() <= 0 && attempt.getTotalQuestions() > 0) {
            attempt.setMaxScore(attempt.getTotalQuestions() * 10);
        }
        return attempt;
    }

    @GetMapping("/runs/current")
    public ResponseEntity<SituationalRun> getCurrentRun(
            @RequestParam String email,
            @RequestParam(defaultValue = "RECOGNITION") String gameType) {
        return runs.findByEmailIgnoreCaseAndGameTypeIgnoreCase(email, gameType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/runs/current")
    public ResponseEntity<SituationalRun> saveCurrentRun(@RequestBody SituationalRun run) {
        if (run.getEmail() == null || run.getEmail().isBlank()
                || run.getGameType() == null || run.getGameType().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        runs.findByEmailIgnoreCaseAndGameTypeIgnoreCase(run.getEmail(), run.getGameType())
                .ifPresent(existing -> run.setId(existing.getId()));
        run.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(runs.save(run));
    }

    @DeleteMapping("/runs/current")
    public ResponseEntity<Void> clearCurrentRun(
            @RequestParam String email,
            @RequestParam(defaultValue = "RECOGNITION") String gameType) {
        runs.deleteByEmailIgnoreCaseAndGameTypeIgnoreCase(email, gameType);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expression-match/progress")
    public Map<String, Object> expressionMatchProgress(@RequestParam String email) {
        List<SituationalAttempt> records = attempts.findByEmailIgnoreCaseOrderByCompletedAtDesc(email).stream()
                .filter(item -> "EXPRESSION_MATCH".equalsIgnoreCase(item.getGameType())).toList();
        List<String> completedSets = records.stream().filter(SituationalAttempt::isCompleted)
                .map(item -> item.getLevel() + "-" + item.getSetNumber()).distinct().toList();
        int highestCompletedLevel = 0;
        int[] requiredSets = {0, 1, 1, 1};
        for (int level = 1; level <= 3; level++) {
            final int currentLevel = level;
            long cleared = completedSets.stream().filter(key -> key.startsWith(currentLevel + "-")).count();
            if (cleared < requiredSets[level]) break;
            highestCompletedLevel = level;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unlockedLevel", Math.min(3, Math.max(1, highestCompletedLevel + 1)));
        result.put("highestCompletedLevel", highestCompletedLevel);
        result.put("completedSets", completedSets);
        result.put("bestScore", records.stream().mapToInt(SituationalAttempt::getScore).max().orElse(0));
        result.put("bestAccuracy", round(records.stream().mapToDouble(SituationalAttempt::getAccuracy).max().orElse(0)));
        result.put("averageAccuracy", round(records.stream().mapToDouble(SituationalAttempt::getAccuracy).average().orElse(0)));
        result.put("attempts", records.size());
        return result;
    }

    private int round(double value) {
        return (int) Math.round(value);
    }
}
