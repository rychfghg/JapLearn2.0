package japlearn.demo.Service;

import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import japlearn.demo.Entity.ReplyCoachAttempt;
import japlearn.demo.Entity.ReplyCoachAttempt.ReadinessRun;
import japlearn.demo.Entity.ReplyCoachAttempt.ReadinessAnswer;

@Service
public class ReplyCoachReadiness {
    public static final String CHAPTER = "A Day of First Impressions";
    private final ReplyCoachQuestionBank bank;
    private final int threshold;
    public ReplyCoachReadiness(ReplyCoachQuestionBank bank,
            @Value("${reply-coach.readiness.threshold-percent:80}") int threshold) {
        if (threshold < 1 || threshold > 100) throw new IllegalArgumentException("Readiness threshold must be 1 to 100.");
        this.bank = bank; this.threshold = threshold;
    }
    public boolean passed(ReadinessRun run) {
        return run.bankVersion == 1 && run.completedAt != null && run.questionIds.size() == 10
            && run.answers.size() == 10
            // A supported answer (meaning or romaji shown) is saved as practice and counts as not correct.
            && run.answers.stream().filter(a -> a.correct && a.helpTypes.isEmpty()).count() * 100 >= threshold * 10;
    }
    public Map<String, Object> policy() {
        return Map.of("thresholdPercent", threshold, "questionCount", 10, "provisional", true,
            "label", "Pilot policy · instructor approval pending");
    }
    public ReadinessRun latest(ReplyCoachAttempt attempt) {
        var runs = attempt.getReadinessRuns();
        return runs.isEmpty() ? null : runs.get(runs.size() - 1);
    }
    public ReadinessRun start(ReplyCoachAttempt attempt) {
        if (!CHAPTER.equals(attempt.getChapterTitle()) || !"COMPLETED".equals(attempt.getStatus()))
            throw new IllegalArgumentException("Finish the original Reply Coach chapter before taking this check.");
        ReadinessRun previous = latest(attempt);
        if (previous != null && previous.completedAt == null) return previous;
        if (previous != null && !passed(previous)) {
            for (var answer : previous.answers) {
                if (answer.correct && answer.helpTypes.isEmpty()) continue;
                String key = previous.id + ":practice-" + bank.get(answer.questionId).practiceNumber();
                if (attempt.getPracticeResults().stream().noneMatch(p -> key.equals(p.key)))
                    throw new IllegalArgumentException("Complete the recommended practice before a fresh readiness check.");
            }
        }
        ReadinessRun run = new ReadinessRun();
        run.id = UUID.randomUUID().toString();
        run.questionIds = bank.select(previous == null ? List.of() : previous.questionIds);
        for (String id : run.questionIds) {
            var order = new ArrayList<>(bank.get(id).choices().stream().map(ReplyCoachQuestionBank.Option::id).toList());
            Collections.shuffle(order); run.optionOrder.put(id, order);
        }
        attempt.getReadinessRuns().add(run);
        return run;
    }
    private String current(ReadinessRun run) {
        if (run == null || run.completedAt != null || run.answers.size() >= run.questionIds.size())
            throw new IllegalArgumentException("No active readiness question.");
        return run.questionIds.get(run.answers.size());
    }
    public void support(ReadinessRun run, String questionId, String type) {
        if (!current(run).equals(questionId) || !Set.of("romaji", "meaning").contains(type))
            throw new IllegalArgumentException("Invalid readiness support request.");
        var help = run.support.computeIfAbsent(questionId, key -> new ArrayList<>());
        if (!help.contains(type)) help.add(type);
    }
    public void answer(ReadinessRun run, String questionId, String choiceId) {
        if (!current(run).equals(questionId)) throw new IllegalArgumentException("This question is not the current question.");
        var question = bank.get(questionId);
        if (question.choices().stream().noneMatch(c -> c.id().equals(choiceId))) throw new IllegalArgumentException("Invalid answer choice.");
        ReadinessAnswer answer = new ReadinessAnswer();
        answer.questionId = questionId; answer.choiceId = choiceId; answer.skill = question.skill();
        answer.correct = question.correctId().equals(choiceId);
        answer.helpTypes = new ArrayList<>(run.support.getOrDefault(questionId, List.of()));
        run.answers.add(answer);
        if (run.answers.size() == run.questionIds.size()) run.completedAt = Instant.now();
    }
    public Map<String, Object> view(ReplyCoachAttempt attempt) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("policy", policy());
        result.put("ready", attempt.getReadinessRuns().stream().anyMatch(this::passed));
        ReadinessRun run = latest(attempt);
        if (run == null) { result.put("status", "NOT_STARTED"); return result; }
        result.put("runId", run.id); result.put("answered", run.answers.size());
        result.put("practicedQuestions", attempt.getPracticeResults().stream().filter(p -> p.key.startsWith(run.id + ":"))
            .map(p -> p.questionId).toList());
        result.put("status", run.completedAt == null ? "IN_PROGRESS" : "COMPLETED");
        if (run.completedAt == null) {
            var q = bank.get(current(run));
            var help = run.support.getOrDefault(q.id(), List.of());
            result.put("question", Map.of("id", q.id(), "skill", q.skill(), "prompt", q.prompt(), "helpTypes", help,
                "choices", run.optionOrder.get(q.id()).stream().map(id -> {
                    var c = q.choices().stream().filter(o -> o.id().equals(id)).findFirst().orElseThrow();
                    return Map.of("id", c.id(), "japanese", c.japanese(), "romaji", c.romaji(),
                        "meaning", help.contains("meaning") ? c.meaning() : "");
                }).toList()));
        } else {
            result.put("passed", passed(run));
            result.put("correct", run.answers.stream().filter(a -> a.correct).count());
            result.put("independentCorrect", run.answers.stream().filter(a -> a.correct && a.helpTypes.isEmpty()).count());
            result.put("supported", run.answers.stream().filter(a -> !a.helpTypes.isEmpty()).count());
            result.put("review", run.answers.stream().map(a -> {
                var q = bank.get(a.questionId);
                return Map.of("questionId", q.id(), "skill", q.skill(), "prompt", q.prompt(), "correct", a.correct,
                    "supported", !a.helpTypes.isEmpty(), "explanation", q.explanation(),
                    "bestReply", q.choices().stream().filter(c -> c.id().equals(q.correctId())).findFirst().orElseThrow().japanese(),
                    "bestRomaji", q.choices().stream().filter(c -> c.id().equals(q.correctId())).findFirst().orElseThrow().romaji(),
                    "practiceNumber", q.practiceNumber());
            }).toList());
        }
        return result;
    }
}
