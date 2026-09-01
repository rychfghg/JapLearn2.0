package japlearn.demo.Controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.SituationalAttempt;
import japlearn.demo.Entity.QuackTalkSession;
import japlearn.demo.Entity.ReplyCoachAttempt;
import japlearn.demo.Entity.Score;
import japlearn.demo.Repository.ReplyCoachAttemptRepository;
import japlearn.demo.Repository.QuackTalkSessionRepository;
import japlearn.demo.Repository.SituationalAttemptRepository;
import japlearn.demo.Repository.ScoreRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/communicationAnalytics")
public class CommunicationAnalyticsController {
    private final SituationalAttemptRepository attempts;
    private final QuackTalkSessionRepository talkSessions;
    private final ReplyCoachAttemptRepository replyCoachAttempts;
    private final ScoreRepository scores;

    public CommunicationAnalyticsController(
            SituationalAttemptRepository attempts,
            QuackTalkSessionRepository talkSessions,
            ReplyCoachAttemptRepository replyCoachAttempts,
            ScoreRepository scores) {
        this.attempts = attempts;
        this.talkSessions = talkSessions;
        this.replyCoachAttempts = replyCoachAttempts;
        this.scores = scores;
    }

    @GetMapping("/getStudentAnalytics")
    public Map<String, Object> getStudentAnalytics(@RequestParam String email) {
        List<SituationalAttempt> records = attempts
                .findByEmailIgnoreCaseAndCompletedTrueOrderByCompletedAtDesc(email);
        List<QuackTalkSession> speakingRecords = talkSessions
                .findByEmailIgnoreCaseOrderByPracticedAtDesc(email);
        List<ReplyCoachAttempt> replyCoachRecords = replyCoachAttempts
                .findByEmailIgnoreCaseAndStatusIgnoreCaseOrderByUpdatedAtDesc(email, "COMPLETED");
        List<Score> arcadeRecords = scores.findByEmailIgnoreCaseOrderByDateDesc(email);

        double recognitionAccuracy = averageAccuracy(records, "RECOGNITION");
        double responseAccuracy = replyCoachRecords.stream()
                .mapToInt(ReplyCoachAttempt::getFinalPercentage)
                .average()
                .orElse(averageAccuracy(records, "RESPONSE"));
        double expressionAccuracy = averageAccuracy(records, "EXPRESSION_MATCH");
        double politenessAccuracy = averageAccuracy(records, "POLITENESS");
        double situationalAccuracy = averageOfAvailable(recognitionAccuracy, expressionAccuracy, politenessAccuracy);

        List<String> strengths = new ArrayList<>();
        List<String> weakAreas = new ArrayList<>();
        if (recognitionAccuracy >= 80) strengths.add("Recognizing natural expressions for everyday situations");
        else if (!records.isEmpty()) weakAreas.add("Choosing expressions that fit the person, place, and time");
        if (records.isEmpty()) weakAreas.add("No completed communication activity has been recorded yet");

        String recommendation;
        if (records.isEmpty()) {
            recommendation = "Complete Recognition to begin building a communication performance record.";
        } else if (recognitionAccuracy < 70) {
            recommendation = "Replay Recognition and review the explanation shown after each answer.";
        } else if (recognitionAccuracy < 90) {
            recommendation = "Continue with the hard Recognition missions to strengthen formal responses.";
        } else {
            recommendation = "Excellent situational awareness. Continue with Expression Match and Politeness.";
        }

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("quackTalkAccuracy", averageEvaluatedSpeakingScore(speakingRecords));
        analytics.put("quackTalkPracticeSessions", speakingRecords.size());
        analytics.put(
                "quackTalkPracticeSeconds",
                speakingRecords.stream().mapToInt(QuackTalkSession::getDurationSeconds).sum());
        analytics.put(
                "quackTalkEvaluationStatus",
                speakingRecords.stream().anyMatch(QuackTalkSession::isEvaluated)
                        ? "EVALUATED"
                        : "PRACTICE_ONLY");
        analytics.put("quackSituateAccuracy", round(situationalAccuracy));
        analytics.put("quackResponseAccuracy", round(responseAccuracy));
        analytics.put("recognitionAccuracy", round(recognitionAccuracy));
        analytics.put("expressionMatchAccuracy", round(expressionAccuracy));
        analytics.put("politenessAccuracy", round(politenessAccuracy));
        analytics.put("completedActivities", records.size() + speakingRecords.size() + replyCoachRecords.size() + arcadeRecords.size());
        analytics.put("quackamoleAccuracy", arcadeAccuracy(arcadeRecords, "QUACKAMOLE"));
        analytics.put("quackmanAccuracy", arcadeAccuracy(arcadeRecords, "QUACKMAN"));
        analytics.put("quackslateAccuracy", arcadeAccuracy(arcadeRecords, "QUACKSLATE"));
        analytics.put("arcadeCompletedActivities", arcadeRecords.stream().filter(Score::isCompleted).count());
        analytics.put("replyCoachCompletedChapters", replyCoachRecords.stream()
                .map(ReplyCoachAttempt::getChapterId).distinct().count());
        analytics.put("replyCoachAttempts", replyCoachRecords.size());
        analytics.put("replyCoachBestScore", replyCoachRecords.stream()
                .mapToInt(ReplyCoachAttempt::getFinalPercentage).max().orElse(0));
        analytics.put("weakAreaCount", weakAreas.size());
        analytics.put("strengths", strengths);
        analytics.put("weakAreas", weakAreas);
        analytics.put("recommendation", recommendation);
        return analytics;
    }

    private double averageOfAvailable(double... values) {
        double sum = 0; int count = 0;
        for (double value : values) if (value > 0) { sum += value; count++; }
        return count == 0 ? 0 : sum / count;
    }

    private double averageAccuracy(List<SituationalAttempt> records, String gameType) {
        return records.stream()
                .filter(record -> gameType.equalsIgnoreCase(record.getGameType()))
                .mapToDouble(SituationalAttempt::getAccuracy)
                .average()
                .orElse(0);
    }

    private int round(double value) {
        return (int) Math.round(value);
    }

    private int averageEvaluatedSpeakingScore(List<QuackTalkSession> records) {
        return (int) Math.round(records.stream()
                .filter(QuackTalkSession::isEvaluated)
                .filter(record -> record.getScore() != null)
                .mapToInt(QuackTalkSession::getScore)
                .average()
                .orElse(0));
    }

    private int arcadeAccuracy(List<Score> records, String game) {
        return records.stream().filter(record -> game.equalsIgnoreCase(record.getGame()))
                .mapToInt(record -> record.getMaxScore() > 0
                        ? (int) Math.round(record.getScore() * 100.0 / record.getMaxScore())
                        : record.getTotalQuestions() > 0
                                ? (int) Math.round(record.getCorrectAnswers() * 100.0 / record.getTotalQuestions())
                                : Math.min(100, Math.max(0, record.getScore())))
                .max().orElse(0);
    }
}
