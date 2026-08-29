package japlearn.demo.Controller;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import japlearn.demo.Entity.ReplyCoachAttempt;
import japlearn.demo.Entity.ReplyCoachChapter;
import japlearn.demo.Repository.ReplyCoachAttemptRepository;
import japlearn.demo.Repository.ReplyCoachChapterRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/reply-coach")
public class ReplyCoachController {
    private final ReplyCoachChapterRepository chapters;
    private final ReplyCoachAttemptRepository attempts;

    public ReplyCoachController(
            ReplyCoachChapterRepository chapters,
            ReplyCoachAttemptRepository attempts) {
        this.chapters = chapters;
        this.attempts = attempts;
    }

    @GetMapping("/chapters")
    public List<ReplyCoachChapter> chapters(
            @RequestParam(defaultValue = "true") boolean publishedOnly) {
        return publishedOnly
                ? chapters.findByStatusIgnoreCaseOrderByOrderAsc("PUBLISHED")
                : chapters.findAll();
    }

    @GetMapping("/chapters/{id}")
    public ResponseEntity<ReplyCoachChapter> chapter(@PathVariable String id) {
        return chapters.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/chapters")
    public ResponseEntity<?> create(@RequestBody ReplyCoachChapter chapter) {
        chapter.setId(null);
        chapter.setCreatedAt(Instant.now());
        chapter.setUpdatedAt(Instant.now());
        Map<String, Object> validation = validate(chapter);
        if ("PUBLISHED".equalsIgnoreCase(chapter.getStatus())
                && Boolean.FALSE.equals(validation.get("valid"))) {
            return ResponseEntity.badRequest().body(validation);
        }
        return ResponseEntity.ok(chapters.save(chapter));
    }

    @PutMapping("/chapters/{id}")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody ReplyCoachChapter chapter) {
        if (!chapters.existsById(id)) return ResponseEntity.notFound().build();
        chapter.setId(id);
        chapter.setUpdatedAt(Instant.now());
        Map<String, Object> validation = validate(chapter);
        if ("PUBLISHED".equalsIgnoreCase(chapter.getStatus())
                && Boolean.FALSE.equals(validation.get("valid"))) {
            return ResponseEntity.badRequest().body(validation);
        }
        return ResponseEntity.ok(chapters.save(chapter));
    }

    @DeleteMapping("/chapters/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!chapters.existsById(id)) return ResponseEntity.notFound().build();
        chapters.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/chapters/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateChapter(@PathVariable String id) {
        return chapters.findById(id)
                .map(chapter -> ResponseEntity.ok(validate(chapter)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/attempts/start")
    public ResponseEntity<?> start(@RequestBody Map<String, String> request) {
        String email = request.getOrDefault("email", "").trim();
        String chapterId = request.getOrDefault("chapterId", "").trim();
        if (email.isBlank() || chapterId.isBlank()) return ResponseEntity.badRequest().build();

        ReplyCoachChapter chapter = chapters.findById(chapterId).orElse(null);
        if (chapter == null || !"PUBLISHED".equalsIgnoreCase(chapter.getStatus())) {
            return ResponseEntity.notFound().build();
        }

        var resumable = attempts
                .findTopByEmailIgnoreCaseAndChapterIdAndStatusIgnoreCaseOrderByUpdatedAtDesc(
                        email,
                        chapterId,
                        "IN_PROGRESS");
        if (resumable.isPresent()) return ResponseEntity.ok(resumable.get());

        ReplyCoachAttempt attempt = new ReplyCoachAttempt();
        attempt.setEmail(email);
        attempt.setName(request.getOrDefault("name", "Student"));
        attempt.setChapterId(chapterId);
        attempt.setChapterTitle(chapter.getTitle());
        attempt.setAttemptNumber((int) attempts.countByEmailIgnoreCaseAndChapterId(email, chapterId) + 1);
        attempt.setCurrentNodeId(chapter.getStartNodeId());
        attempt.setMaximumScore(maximumScore(chapter));
        return ResponseEntity.ok(attempts.save(attempt));
    }

    @PutMapping("/attempts/{id}/progress")
    public ResponseEntity<ReplyCoachAttempt> progress(
            @PathVariable String id,
            @RequestBody Map<String, Object> request) {
        ReplyCoachAttempt attempt = attempts.findById(id).orElse(null);
        if (attempt == null || !"IN_PROGRESS".equalsIgnoreCase(attempt.getStatus())) {
            return ResponseEntity.notFound().build();
        }
        Object currentNodeId = request.get("currentNodeId");
        if (currentNodeId != null) attempt.setCurrentNodeId(String.valueOf(currentNodeId));
        attempt.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(attempts.save(attempt));
    }

    @PostMapping("/attempts/{id}/answer")
    public ResponseEntity<?> answer(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        ReplyCoachAttempt attempt = attempts.findById(id).orElse(null);
        if (attempt == null || !"IN_PROGRESS".equalsIgnoreCase(attempt.getStatus())) {
            return ResponseEntity.notFound().build();
        }
        ReplyCoachChapter chapter = chapters.findById(attempt.getChapterId()).orElse(null);
        if (chapter == null) return ResponseEntity.notFound().build();

        String nodeId = request.getOrDefault("nodeId", "");
        String choiceId = request.getOrDefault("choiceId", "");
        ReplyCoachChapter.StoryNode node = chapter.getNodes().stream()
                .filter(item -> nodeId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        if (node == null || !"CHOICE".equalsIgnoreCase(node.getType())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid decision node."));
        }
        ReplyCoachChapter.ChoiceOption choice = node.getChoices().stream()
                .filter(item -> choiceId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        if (choice == null) return ResponseEntity.badRequest().body(Map.of("message", "Invalid choice."));
        boolean alreadyAnswered = attempt.getAnswers().stream()
                .anyMatch(item -> nodeId.equals(item.getNodeId()));
        if (alreadyAnswered) return ResponseEntity.badRequest().body(Map.of("message", "Decision already recorded."));

        ReplyCoachAttempt.AnswerRecord record = new ReplyCoachAttempt.AnswerRecord();
        record.setNodeId(nodeId);
        record.setPrompt(node.getText());
        record.setChoiceId(choiceId);
        record.setSelectedText(choice.getText());
        record.setSelectedJapanese(choice.getJapanese());
        record.setEvaluation(choice.getEvaluation());
        record.setPoints(choice.getPoints());
        record.setExplanation(choice.getExplanation());
        record.setCulturalNote(choice.getCulturalNote());
        record.setBestResponse(node.getChoices().stream()
                .filter(item -> "BEST".equalsIgnoreCase(item.getEvaluation()))
                .map(item -> item.getJapanese() + " · " + item.getRomaji())
                .findFirst()
                .orElse(""));
        attempt.getAnswers().add(record);
        attempt.setScore(attempt.getScore() + choice.getPoints());
        increment(attempt, choice.getEvaluation());
        attempt.setCurrentNodeId(choice.getNextNodeId());
        attempt.setUpdatedAt(Instant.now());
        attempts.save(attempt);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attempt", attempt);
        response.put("choice", choice);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/attempts/{id}/complete")
    public ResponseEntity<ReplyCoachAttempt> complete(@PathVariable String id) {
        ReplyCoachAttempt attempt = attempts.findById(id).orElse(null);
        if (attempt == null) return ResponseEntity.notFound().build();
        attempt.setStatus("COMPLETED");
        attempt.setCompletedAt(Instant.now());
        attempt.setUpdatedAt(Instant.now());
        attempt.setFinalPercentage(attempt.getMaximumScore() == 0
                ? 0
                : (int) Math.round(attempt.getScore() * 100.0 / attempt.getMaximumScore()));
        return ResponseEntity.ok(attempts.save(attempt));
    }

    @GetMapping("/attempts")
    public List<ReplyCoachAttempt> attemptHistory(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status) {
        List<ReplyCoachAttempt> result = email == null || email.isBlank()
                ? attempts.findAll()
                : attempts.findByEmailIgnoreCaseOrderByUpdatedAtDesc(email);
        if (status == null || status.isBlank()) return result;
        return result.stream()
                .filter(item -> status.equalsIgnoreCase(item.getStatus()))
                .toList();
    }

    @GetMapping("/progress")
    public Map<String, Object> progressSummary(@RequestParam String email) {
        List<ReplyCoachAttempt> completed = attempts
                .findByEmailIgnoreCaseAndStatusIgnoreCaseOrderByUpdatedAtDesc(email, "COMPLETED");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("completedChapters", completed.stream().map(ReplyCoachAttempt::getChapterId).distinct().count());
        result.put("attempts", completed.size());
        result.put("bestScore", completed.stream().mapToInt(ReplyCoachAttempt::getFinalPercentage).max().orElse(0));
        result.put("averageScore", (int) Math.round(completed.stream().mapToInt(ReplyCoachAttempt::getFinalPercentage).average().orElse(0)));
        result.put("bestResponses", completed.stream().mapToInt(ReplyCoachAttempt::getBestCount).sum());
        result.put("acceptableResponses", completed.stream().mapToInt(ReplyCoachAttempt::getAcceptableCount).sum());
        result.put("awkwardResponses", completed.stream().mapToInt(ReplyCoachAttempt::getAwkwardCount).sum());
        result.put("impoliteOrRudeResponses", completed.stream()
                .mapToInt(item -> item.getImpoliteCount() + item.getRudeCount()).sum());
        result.put("recentAttempts", completed.stream().limit(5).toList());
        return result;
    }

    private int maximumScore(ReplyCoachChapter chapter) {
        return chapter.getNodes().stream()
                .filter(node -> "CHOICE".equalsIgnoreCase(node.getType()))
                .mapToInt(node -> node.getChoices().stream()
                        .mapToInt(ReplyCoachChapter.ChoiceOption::getPoints)
                        .max()
                        .orElse(0))
                .sum();
    }

    private void increment(ReplyCoachAttempt attempt, String evaluation) {
        switch (evaluation == null ? "" : evaluation.toUpperCase()) {
            case "BEST" -> attempt.setBestCount(attempt.getBestCount() + 1);
            case "ACCEPTABLE" -> attempt.setAcceptableCount(attempt.getAcceptableCount() + 1);
            case "AWKWARD" -> attempt.setAwkwardCount(attempt.getAwkwardCount() + 1);
            case "IMPOLITE" -> attempt.setImpoliteCount(attempt.getImpoliteCount() + 1);
            default -> attempt.setRudeCount(attempt.getRudeCount() + 1);
        }
    }

    private Map<String, Object> validate(ReplyCoachChapter chapter) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, ReplyCoachChapter.StoryNode> byId = new HashMap<>();
        for (ReplyCoachChapter.StoryNode node : chapter.getNodes()) {
            if (node.getId() == null || node.getId().isBlank()) {
                errors.add("Every node needs an ID.");
                continue;
            }
            if (byId.put(node.getId(), node) != null) errors.add("Duplicate node ID: " + node.getId());
        }
        if (chapter.getStartNodeId() == null || !byId.containsKey(chapter.getStartNodeId())) {
            errors.add("The start node is missing or does not exist.");
        }
        int endings = 0;
        for (ReplyCoachChapter.StoryNode node : chapter.getNodes()) {
            if ("ENDING".equalsIgnoreCase(node.getType())) endings++;
            if ("CHOICE".equalsIgnoreCase(node.getType())) {
                if (node.getChoices() == null || node.getChoices().size() < 2) {
                    errors.add("Choice node " + node.getId() + " needs at least two options.");
                } else {
                    for (ReplyCoachChapter.ChoiceOption option : node.getChoices()) {
                        if (option.getNextNodeId() == null || !byId.containsKey(option.getNextNodeId())) {
                            errors.add("Choice " + option.getId() + " in " + node.getId() + " has no valid destination.");
                        }
                    }
                }
            } else if (!"ENDING".equalsIgnoreCase(node.getType())
                    && (node.getNextNodeId() == null || !byId.containsKey(node.getNextNodeId()))) {
                errors.add("Node " + node.getId() + " has no valid continuation.");
            }
        }
        if (endings == 0) errors.add("The chapter needs at least one ending.");

        if (byId.containsKey(chapter.getStartNodeId())) {
            Set<String> reachable = new HashSet<>();
            ArrayDeque<String> queue = new ArrayDeque<>();
            queue.add(chapter.getStartNodeId());
            while (!queue.isEmpty()) {
                String id = queue.removeFirst();
                if (!reachable.add(id)) continue;
                ReplyCoachChapter.StoryNode node = byId.get(id);
                if (node == null) continue;
                if (node.getNextNodeId() != null) queue.add(node.getNextNodeId());
                if (node.getChoices() != null) {
                    node.getChoices().stream()
                            .map(ReplyCoachChapter.ChoiceOption::getNextNodeId)
                            .filter(next -> next != null && !next.isBlank())
                            .forEach(queue::add);
                }
            }
            byId.keySet().stream()
                    .filter(id -> !reachable.contains(id))
                    .forEach(id -> warnings.add("Unreachable node: " + id));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("nodeCount", chapter.getNodes().size());
        result.put("decisionCount", chapter.getNodes().stream()
                .filter(node -> "CHOICE".equalsIgnoreCase(node.getType())).count());
        return result;
    }
}
