package japlearn.demo.Controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.bson.types.ObjectId;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import japlearn.demo.Entity.QuackTalkSession;
import japlearn.demo.Entity.ReplyCoachAttempt;
import japlearn.demo.Entity.Score;
import japlearn.demo.Entity.SituationalAttempt;
import japlearn.demo.Repository.QuackTalkSessionRepository;
import japlearn.demo.Repository.ReplyCoachAttemptRepository;
import japlearn.demo.Repository.ScoreRepository;
import japlearn.demo.Repository.SituationalAttemptRepository;
import japlearn.demo.Service.StudentService;
import japlearn.demo.Service.TeacherAuthorizationService;

/**
 * A single, teacher-scoped view of every persisted game attempt. Personal-best
 * records are not substituted for history: latest, average and highest are
 * calculated from all scored attempts for the selected student.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/teacher/game-performance")
public class TeacherGamePerformanceController {
    private final TeacherAuthorizationService authorization;
    private final StudentService students;
    private final SituationalAttemptRepository situational;
    private final ReplyCoachAttemptRepository replyCoach;
    private final ScoreRepository scores;
    private final QuackTalkSessionRepository talk;

    public TeacherGamePerformanceController(TeacherAuthorizationService authorization,
            StudentService students, SituationalAttemptRepository situational,
            ReplyCoachAttemptRepository replyCoach, ScoreRepository scores,
            QuackTalkSessionRepository talk) {
        this.authorization = authorization;
        this.students = students;
        this.situational = situational;
        this.replyCoach = replyCoach;
        this.scores = scores;
        this.talk = talk;
    }

    public record AttemptView(String id, String game, String activity, Integer score,
            Integer maxScore, Integer percentage, String playedAt, String status,
            String mode, String feedbackSummary, Integer pronunciationScore,
            Integer accuracyScore, Integer fluencyScore, Integer completenessScore,
            Integer contextualAccuracy, List<String> areasForImprovement,
            List<String> expressionsPracticed, Integer conversationTurns) {}

    public record SummaryView(String label, int attempts, int scoredAttempts,
            Integer latest, Integer average, Integer highest, String latestAt) {}

    public record GameView(String name, SummaryView summary, List<SummaryView> activities) {}

    public record PerformanceView(String studentEmail, int totalAttempts,
            List<GameView> games, List<AttemptView> attempts) {}

    private record TimedAttempt(Instant time, AttemptView attempt) {}

    @GetMapping
    public PerformanceView get(@RequestParam String teacherEmail,
            @RequestParam String studentEmail,
            @RequestHeader(value = "X-Teacher-Token", required = false) String token) {
        String teacher = authorization.requireTeacher(teacherEmail, token);
        if (studentEmail == null || studentEmail.isBlank()
                || students.getStudentsForTeacher(teacher).stream()
                        .noneMatch(student -> student.getEmail() != null
                                && student.getEmail().equalsIgnoreCase(studentEmail.trim()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This student is not in one of your classes");
        }

        String email = studentEmail.trim().toLowerCase(Locale.ROOT);
        List<TimedAttempt> rows = new ArrayList<>();
        for (SituationalAttempt item : situational.findByEmailIgnoreCaseOrderByCompletedAtDesc(email)) {
            String activity = switch (safeUpper(item.getGameType())) {
                case "RECOGNITION" -> "Recognition";
                case "EXPRESSION_MATCH" -> "Expression Match";
                case "POLITENESS" -> "Politeness";
                default -> safeLabel(item.getGameType(), "Other situation");
            };
            String game = "RESPONSE".equals(safeUpper(item.getGameType()))
                    ? "QuackResponse" : "QuackSituate";
            int max = item.getMaxScore() > 0 ? item.getMaxScore()
                    : Math.max(0, item.getTotalQuestions() * 10);
            Integer percent = max > 0 ? percentage(item.getScore(), max)
                    : clamp((int) Math.round(item.getAccuracy()));
            Instant time = orEpoch(item.getCompletedAt());
            rows.add(new TimedAttempt(time, new AttemptView(item.getId(), game, activity,
                    item.getScore(), max > 0 ? max : null, percent, time.toString(),
                    item.isCompleted() ? "COMPLETED" : "IN_PROGRESS",
                    item.getDifficulty(), null, null, null, null, null, null,
                    List.of(), List.of(), null)));
        }
        for (ReplyCoachAttempt item : replyCoach.findByEmailIgnoreCaseOrderByUpdatedAtDesc(email)) {
            boolean completed = "COMPLETED".equalsIgnoreCase(item.getStatus());
            Instant time = orEpoch(completed && item.getCompletedAt() != null
                    ? item.getCompletedAt() : item.getUpdatedAt());
            int max = item.getMaximumScore();
            Integer percent = completed ? clamp(item.getFinalPercentage())
                    : max > 0 ? percentage(item.getScore(), max) : null;
            rows.add(new TimedAttempt(time, new AttemptView(item.getId(), "QuackResponse",
                    "Reply Coach · " + safeLabel(item.getChapterTitle(), "Chapter"),
                    item.getScore(), max > 0 ? max : null, percent, time.toString(),
                    completed ? "COMPLETED" : "IN_PROGRESS", null, null, null, null,
                    null, null, null, List.of(), List.of(), null)));
        }
        for (Score item : scores.findByEmailIgnoreCaseOrderByDateDesc(email)) {
            String gameCode = safeUpper(item.getGame());
            String game = switch (gameCode) {
                case "QUACKRESPONSE_RUSH", "QUACKRESPONSE_RELAY" -> "QuackResponse";
                case "QUACKAMOLE" -> "Quack-a-Mole";
                case "QUACKMAN" -> "Quackman";
                case "QUACKSLATE" -> "QuackSlate";
                default -> safeLabel(item.getGame(), "Other game");
            };
            String activity = switch (gameCode) {
                case "QUACKRESPONSE_RUSH" -> "Response Rush";
                case "QUACKRESPONSE_RELAY" -> "Dialogue Relay";
                case "QUACKAMOLE" -> "Quack-a-Mole";
                case "QUACKMAN" -> "Quackman";
                case "QUACKSLATE" -> "QuackSlate";
                default -> game;
            };
            Instant time = scoreTime(item);
            Integer percent = item.getMaxScore() > 0
                    ? percentage(item.getScore(), item.getMaxScore())
                    : item.getTotalQuestions() > 0
                            ? percentage(item.getCorrectAnswers(), item.getTotalQuestions())
                            : clamp(item.getScore());
            rows.add(new TimedAttempt(time, new AttemptView(item.getId(), game, activity,
                    item.getScore(), item.getMaxScore() > 0 ? item.getMaxScore() : null,
                    percent, time.equals(Instant.EPOCH) ? item.getDate() : time.toString(),
                    item.isCompleted() ? "COMPLETED" : "IN_PROGRESS", item.getMode(),
                    null, null, null, null, null, null, List.of(), List.of(), null)));
        }
        for (QuackTalkSession item : talk.findByEmailIgnoreCaseOrderByPracticedAtDesc(email)) {
            Instant time = orEpoch(item.getPracticedAt());
            boolean scored = item.isEvaluated() && item.getScore() != null;
            rows.add(new TimedAttempt(time, new AttemptView(item.getId(), "QuackTalk",
                    "GUIDED_PHRASE".equalsIgnoreCase(item.getRoomType())
                            ? "Guided Phrase" : "Talk with Sumi",
                    scored ? item.getScore() : null, scored ? 100 : null,
                    scored ? clamp(item.getScore()) : null, time.toString(),
                    item.isCompleted() ? "COMPLETED" : "IN_PROGRESS",
                    item.getScenarioTitle(), item.getFeedbackSummary(),
                    item.getPronunciationScore(), item.getAccuracyScore(),
                    item.getFluencyScore(), item.getCompletenessScore(),
                    item.getContextualAccuracy(),
                    item.getAreasForImprovement() == null ? List.of() : item.getAreasForImprovement(),
                    item.getExpressionsPracticed() == null ? List.of() : item.getExpressionsPracticed(),
                    item.getConversationTurns())));
        }

        rows.sort(Comparator.comparing(TimedAttempt::time).reversed());
        List<AttemptView> history = rows.stream().map(TimedAttempt::attempt).toList();
        List<GameView> games = List.of(
                game(history, "QuackTalk", List.of("Guided Phrase", "Talk with Sumi")),
                game(history, "QuackSituate", List.of("Recognition", "Expression Match", "Politeness")),
                game(history, "QuackResponse", List.of("Reply Coach", "Response Rush", "Dialogue Relay")),
                game(history, "Quack-a-Mole", List.of()),
                game(history, "Quackman", List.of()),
                game(history, "QuackSlate", List.of()));
        return new PerformanceView(email, history.size(), games, history);
    }

    private GameView game(List<AttemptView> history, String name, List<String> activities) {
        List<AttemptView> gameRows = history.stream().filter(row -> name.equals(row.game())).toList();
        List<SummaryView> children = activities.stream().map(activity -> summary(
                activity, gameRows.stream().filter(row -> row.activity().equals(activity)
                        || ("Reply Coach".equals(activity) && row.activity().startsWith("Reply Coach · ")))
                        .toList())).toList();
        return new GameView(name, summary(name, gameRows), children);
    }

    private SummaryView summary(String label, List<AttemptView> rows) {
        List<AttemptView> scored = rows.stream().filter(row -> row.percentage() != null
                && "COMPLETED".equals(row.status())).toList();
        return new SummaryView(label, rows.size(), scored.size(),
                scored.isEmpty() ? null : scored.get(0).percentage(),
                scored.isEmpty() ? null : (int) Math.round(scored.stream()
                        .mapToInt(AttemptView::percentage).average().orElse(0)),
                scored.stream().map(AttemptView::percentage).filter(Objects::nonNull)
                        .max(Integer::compareTo).orElse(null),
                scored.isEmpty() ? null : scored.get(0).playedAt());
    }

    private int percentage(int value, int maximum) {
        return maximum <= 0 ? 0 : clamp((int) Math.round(value * 100.0 / maximum));
    }
    private int clamp(int value) { return Math.max(0, Math.min(100, value)); }
    private String safeUpper(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private String safeLabel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.replace('_', ' ');
    }
    private Instant orEpoch(Instant value) { return value == null ? Instant.EPOCH : value; }
    private Instant scoreTime(Score score) {
        String date = score.getDate();
        if (date != null && date.contains("T")) return parseDate(date);
        // Older arcade records store only a day. Their Mongo id supplies the
        // missing time, so multiple plays on that day have a real latest run.
        if (score.getId() != null && ObjectId.isValid(score.getId()))
            return new ObjectId(score.getId()).getDate().toInstant();
        return parseDate(date);
    }
    private Instant parseDate(String value) {
        if (value == null || value.isBlank()) return Instant.EPOCH;
        try { return Instant.parse(value); } catch (Exception ignored) {}
        try { return OffsetDateTime.parse(value).toInstant(); } catch (Exception ignored) {}
        try { return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC); }
        catch (Exception ignored) { return Instant.EPOCH; }
    }
}
