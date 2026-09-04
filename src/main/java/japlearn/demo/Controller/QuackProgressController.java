package japlearn.demo.Controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.QuackTalkSession;
import japlearn.demo.Entity.ReplyCoachAttempt;
import japlearn.demo.Entity.Score;
import japlearn.demo.Entity.SituationalAttempt;
import japlearn.demo.Repository.QuackTalkSessionRepository;
import japlearn.demo.Repository.ReplyCoachAttemptRepository;
import japlearn.demo.Repository.ScoreRepository;
import japlearn.demo.Repository.SituationalAttemptRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/quackProgress")
public class QuackProgressController {
    private final SituationalAttemptRepository situationalAttempts;
    private final ReplyCoachAttemptRepository replyAttempts;
    private final QuackTalkSessionRepository talkSessions;
    private final ScoreRepository scores;

    public QuackProgressController(SituationalAttemptRepository situationalAttempts,
            ReplyCoachAttemptRepository replyAttempts, QuackTalkSessionRepository talkSessions,
            ScoreRepository scores) {
        this.situationalAttempts = situationalAttempts;
        this.replyAttempts = replyAttempts;
        this.talkSessions = talkSessions;
        this.scores = scores;
    }

    @GetMapping("/analytics")
    public Map<String, Object> analytics(@RequestParam String email) {
        List<SituationalAttempt> situational = situationalAttempts.findByEmailIgnoreCaseOrderByCompletedAtDesc(email);
        List<ReplyCoachAttempt> replies = replyAttempts.findByEmailIgnoreCaseOrderByUpdatedAtDesc(email);
        List<QuackTalkSession> talk = talkSessions.findByEmailIgnoreCaseOrderByPracticedAtDesc(email);
        List<Score> arcade = scores.findByEmailIgnoreCaseOrderByDateDesc(email);

        List<Map<String, Object>> modules = new ArrayList<>();
        addModule(modules, "Recognition", averageSituational(situational, "RECOGNITION"));
        addModule(modules, "Expression Match", averageSituational(situational, "EXPRESSION_MATCH"));
        addModule(modules, "Politeness", averageSituational(situational, "POLITENESS"));
        addModule(modules, "Reply Coach", averageReplies(replies));
        addArcadeModule(modules, arcade, "QUACKRESPONSE_RUSH", "Response Rush");
        addModule(modules, "QuackTalk", averageTalk(talk));
        addArcadeModule(modules, arcade, "QUACKAMOLE", "Quack-a-Mole");
        addArcadeModule(modules, arcade, "QUACKMAN", "Quackman");
        addArcadeModule(modules, arcade, "QUACKSLATE", "QuackSlate");

        int overall = (int) Math.round(modules.stream().mapToInt(item -> (int) item.get("value")).average().orElse(0));
        int situationalAccuracy = averageAvailable(
                averageSituational(situational, "RECOGNITION"),
                averageSituational(situational, "EXPRESSION_MATCH"),
                averageSituational(situational, "POLITENESS"));
        int interactionAccuracy = averageAvailable(averageReplies(replies), averageTalk(talk));

        List<String> weakAreas = modules.stream()
                .filter(item -> (int) item.get("value") < 70)
                .map(item -> item.get("label") + " needs reinforcement")
                .toList();
        List<String> mistakes = situational.stream().filter(item -> item.getWrongAnswers() > 0)
                .map(item -> item.getGameType().replace('_', ' ') + ": " + item.getWrongAnswers() + " responses to review")
                .distinct().limit(6).toList();

        List<Map<String, Object>> history = new ArrayList<>();
        situational.stream().filter(SituationalAttempt::isCompleted).limit(12)
                .forEach(item -> history.add(history(item.getGameType().replace('_', ' '), (int) Math.round(item.getAccuracy()))));
        replies.stream().filter(item -> "COMPLETED".equalsIgnoreCase(item.getStatus())).limit(8)
                .forEach(item -> history.add(history("Reply Coach · " + item.getChapterTitle(), item.getFinalPercentage())));
        arcade.stream().limit(12).forEach(item -> history.add(history(displayGame(item.getGame()), scorePercent(item))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overallMastery", overall);
        result.put("situationalAccuracy", situationalAccuracy);
        result.put("interactionAccuracy", interactionAccuracy);
        result.put("progressSummary", overall == 0 ? "Complete a communication game to begin your report." : "Your overall communication mastery is " + overall + "% across " + modules.size() + " practiced areas.");
        result.put("recommendation", weakAreas.isEmpty() ? "Keep replaying completed activities to maintain mastery." : "Focus next on " + weakAreas.get(0).replace(" needs reinforcement", "") + ".");
        result.put("weakAreas", weakAreas);
        result.put("repeatedMistakes", mistakes);
        result.put("history", history);
        result.put("moduleAccuracy", modules);
        return result;
    }

    @GetMapping("/progression")
    public Map<String, Object> progression(@RequestParam String email) {
        Map<String, Object> report = analytics(email);
        int mastery = (int) report.get("overallMastery");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) report.get("moduleAccuracy");
        List<Map<String, Object>> stages = new ArrayList<>();
        for (int index = 0; index < modules.size(); index++) {
            Map<String, Object> module = modules.get(index);
            int value = (int) module.get("value");
            stages.add(Map.of("id", index + 1, "name", module.get("label"), "progress", value,
                    "status", value >= 80 ? "MASTERED" : value > 0 ? "IN PROGRESS" : "READY",
                    "unlocked", index == 0 || mastery >= Math.min(80, 40 + index * 5)));
        }
        @SuppressWarnings("unchecked")
        List<String> weakAreas = (List<String>) report.get("weakAreas");
        List<Map<String, Object>> reinforcement = weakAreas.stream().limit(5)
                .map(area -> Map.<String, Object>of("title", area.replace(" needs reinforcement", ""),
                        "mistake", area, "retry", "Replay this activity and review every explanation.",
                        "targetRoute", routeFor(area))).toList();
        return Map.of("currentMastery", mastery, "unlockRequirement", 80,
                "masteryHint", mastery >= 80 ? "Advanced communication practice is unlocked." : (80 - mastery) + "% more mastery is needed for the next milestone.",
                "coachMessage", weakAreas.isEmpty() ? "Your practiced skills are balanced." : "A focused replay will strengthen your lowest area.",
                "stages", stages, "reinforcement", reinforcement);
    }

    private void addModule(List<Map<String, Object>> modules, String label, int value) { if (value > 0) modules.add(Map.of("label", label, "value", value)); }
    private void addArcadeModule(List<Map<String, Object>> modules, List<Score> records, String game, String label) {
        records.stream().filter(item -> game.equalsIgnoreCase(item.getGame())).mapToInt(this::scorePercent).max().ifPresent(value -> addModule(modules, label, value));
    }
    private int averageSituational(List<SituationalAttempt> records, String type) { return (int) Math.round(records.stream().filter(SituationalAttempt::isCompleted).filter(item -> type.equalsIgnoreCase(item.getGameType())).mapToDouble(SituationalAttempt::getAccuracy).average().orElse(0)); }
    private int averageReplies(List<ReplyCoachAttempt> records) { return (int) Math.round(records.stream().filter(item -> "COMPLETED".equalsIgnoreCase(item.getStatus())).mapToInt(ReplyCoachAttempt::getFinalPercentage).average().orElse(0)); }
    private int averageTalk(List<QuackTalkSession> records) { return (int) Math.round(records.stream().filter(QuackTalkSession::isEvaluated).filter(item -> item.getScore() != null).mapToInt(QuackTalkSession::getScore).average().orElse(0)); }
    private int averageAvailable(int... values) { return (int) Math.round(java.util.Arrays.stream(values).filter(value -> value > 0).average().orElse(0)); }
    private int scorePercent(Score score) { if (score.getMaxScore() > 0) return Math.min(100, (int) Math.round(score.getScore() * 100.0 / score.getMaxScore())); if (score.getTotalQuestions() > 0) return Math.min(100, (int) Math.round(score.getCorrectAnswers() * 100.0 / score.getTotalQuestions())); return Math.min(100, Math.max(0, score.getScore())); }
    private Map<String, Object> history(String title, int score) { return Map.of("title", title, "score", score); }
    private String displayGame(String game) { if (game == null) return "Arcade"; return switch (game.toUpperCase()) { case "QUACKRESPONSE_RUSH" -> "Response Rush"; case "QUACKAMOLE" -> "Quack-a-Mole"; case "QUACKMAN" -> "Quackman"; case "QUACKSLATE" -> "QuackSlate"; default -> game; }; }
    private String routeFor(String area) { if (area.contains("Reply") || area.contains("Response Rush")) return "/QuackResponse"; if (area.contains("Talk")) return "/QuackTalk"; if (area.contains("Mole")) return "/Quackamole"; if (area.contains("Quackman")) return "/QuackmanLevels"; if (area.contains("Slate")) return "/QuackslateMenu"; return "/QuackSituate"; }
}
