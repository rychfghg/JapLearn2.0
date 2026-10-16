package japlearn.demo.Service;

import java.util.*;
import japlearn.demo.Entity.ExpressionQuestionResult;
import japlearn.demo.Entity.SituationalAttempt;

public final class ExpressionMatchEvidence {
    private ExpressionMatchEvidence() {}
    public static void normalize(SituationalAttempt attempt) {
        var rows = attempt.getQuestionResults();
        if (rows == null) return; // Legacy clients have no per-question history.
        if (rows.size() > 1000) throw new IllegalArgumentException("Too many question records");
        Set<String> answered = new HashSet<>();
        int correct = 0, wrong = 0, hints = 0;
        for (var row : rows) {
            if (row == null || row.questionId() == null || row.expression() == null
                    || !Set.of("PICTURE", "CAPTION_ONLY").contains(row.mode() == null ? "" : row.mode())
                    || !Set.of("CORRECT", "INCORRECT", "TECHNICAL_INTERRUPTION").contains(row.outcome() == null ? "" : row.outcome()))
                throw new IllegalArgumentException("Invalid question evidence");
            if ("TECHNICAL_INTERRUPTION".equals(row.outcome())) continue;
            if (!answered.add(row.questionId())) throw new IllegalArgumentException("Duplicate scored question");
            if (!Set.of("top", "bottom").contains(row.selectedPosition() == null ? "" : row.selectedPosition())
                    || !Set.of("top", "bottom").contains(row.correctPosition() == null ? "" : row.correctPosition())
                    || (row.selectedPosition().equals(row.correctPosition()) != "CORRECT".equals(row.outcome())))
                throw new IllegalArgumentException("Inconsistent question result");
            if ("CORRECT".equals(row.outcome())) correct++; else wrong++;
            if (row.hintUsed()) hints++;
        }
        if (attempt.isCompleted() && correct + wrong != attempt.getTotalQuestions())
            throw new IllegalArgumentException("Completed attempt needs every scored question");
        attempt.setCorrectAnswers(correct);
        attempt.setWrongAnswers(wrong);
        attempt.setAnsweredCount(correct + wrong);
        attempt.setHintsUsed(hints);
        attempt.setScore(correct * 10);
        attempt.setMaxScore(attempt.getTotalQuestions() * 10);
        attempt.setAccuracy(correct + wrong == 0 ? 0 : Math.round(correct * 10000.0 / (correct + wrong)) / 100.0);
    }

    public static List<Map<String, Object>> breakdown(List<SituationalAttempt> attempts) {
        Map<String, Map<String, Object>> groups = new LinkedHashMap<>();
        for (var attempt : attempts) {
            if (attempt.getQuestionResults() == null) continue;
            for (var row : attempt.getQuestionResults()) {
                if (row == null) continue;
                String key = row.expression() + "|" + row.mode();
                var group = groups.computeIfAbsent(key, ignored -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("expression", row.expression()); item.put("mode", row.mode());
                    item.put("correct", 0); item.put("incorrect", 0); item.put("technicalInterruptions", 0);
                    item.put("confusedContexts", new ArrayList<String>()); return item;
                });
                String count = "CORRECT".equals(row.outcome()) ? "correct"
                    : "INCORRECT".equals(row.outcome()) ? "incorrect" : "technicalInterruptions";
                group.put(count, (Integer) group.get(count) + 1);
                if ("INCORRECT".equals(row.outcome())) {
                    @SuppressWarnings("unchecked") var contexts = (List<String>) group.get("confusedContexts");
                    if (!contexts.contains(row.selectedScenario())) contexts.add(row.selectedScenario());
                }
            }
        }
        return new ArrayList<>(groups.values());
    }
}
