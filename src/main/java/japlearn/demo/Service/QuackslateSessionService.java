package japlearn.demo.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.security.SecureRandom;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import japlearn.demo.Entity.QuackslateContent;
import japlearn.demo.Entity.QuackslateGameCode;
import japlearn.demo.Entity.QuackslateQuestion;
import japlearn.demo.Entity.Score;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.QuackslateContentRepository;
import japlearn.demo.Repository.QuackslateGameCodeRepository;
import japlearn.demo.Repository.QuackslateQuestionRepository;
import japlearn.demo.Repository.ScoreRepository;
import japlearn.demo.Repository.UserRepository;

/** Scheduled teacher-code sessions use persisted UTC instants, not an in-memory timer. */
@Service
public class QuackslateSessionService {
    private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Manila");
    private final QuackslateGameCodeService codeService;
    private final QuackslateGameCodeRepository games;
    private final QuackslateQuestionRepository questions;
    private final QuackslateContentRepository content;
    private final ScoreRepository scores;
    private final UserRepository users;
    private final MongoTemplate mongo;
    private final SecureRandom random = new SecureRandom();

    public QuackslateSessionService(QuackslateGameCodeService codeService,
            QuackslateGameCodeRepository games, QuackslateQuestionRepository questions,
            QuackslateContentRepository content, ScoreRepository scores,
            UserRepository users, MongoTemplate mongo) {
        this.codeService = codeService;
        this.games = games;
        this.questions = questions;
        this.content = content;
        this.scores = scores;
        this.users = users;
        this.mongo = mongo;
    }

    public record SessionView(String gameCode, String status, Instant serverNow,
            Instant startsAt, Instant endsAt, long remainingSeconds, int questionCount,
            int joinedCount) {}
    public record ScoreAttempt(int score, int maxScore, double percentage,
            Instant playedAt, boolean completed) {}
    public record ScoreRow(String email, String name, int attempts,
            Double latest, Double average, Double highest, Instant latestAt,
            List<ScoreAttempt> history) {}
    public record ScoreSheet(String gameCode, int joinedCount, int submittedCount,
            List<ScoreRow> rows) {}

    public static String status(QuackslateGameCode game, Instant now) {
        if (!game.isPublished() || game.getStartsAt() == null || game.getEndsAt() == null)
            return "DRAFT";
        if (now.isBefore(game.getStartsAt())) return "UPCOMING";
        return now.isBefore(game.getEndsAt()) ? "LIVE" : "ENDED";
    }

    public SessionView view(QuackslateGameCode game) {
        Instant now = Instant.now();
        String state = status(game, now);
        long remaining = "LIVE".equals(state)
                ? Math.max(0, Duration.between(now, game.getEndsAt()).getSeconds()) : 0;
        return new SessionView(game.getGameCode(), state, now, game.getStartsAt(),
                game.getEndsAt(), remaining, game.getQuestionIds().size(),
                game.getJoinedStudentEmails().size());
    }

    public QuackslateGameCode owned(String teacherEmail, String code) {
        QuackslateGameCode game = require(code);
        if (game.getOwnerTeacherEmail() == null
                || !game.getOwnerTeacherEmail().equalsIgnoreCase(teacherEmail))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This session belongs to another teacher");
        return game;
    }

    public QuackslateGameCode require(String code) {
        return games.findByGameCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "QuackSlate code not found"));
    }

    public QuackslateGameCode create(String teacherEmail) {
        QuackslateGameCode game = codeService.generateNewGameCode();
        game.setOwnerTeacherEmail(teacherEmail);
        game.setPublished(false);
        return games.save(game);
    }

    public List<SessionView> list(String teacherEmail) {
        return games.findByOwnerTeacherEmailIgnoreCaseOrderByStartsAtDesc(teacherEmail)
                .stream().map(this::view).toList();
    }

    public List<QuackslateQuestion> availableQuestions(String teacherEmail) {
        return questions.findAll().stream()
                .filter(q -> (q.isApproved() && (blank(q.getCreatedBy()) || "SYSTEM".equalsIgnoreCase(q.getCreatedBy())))
                        || teacherEmail.equalsIgnoreCase(q.getCreatedBy()))
                .toList();
    }

    public QuackslateQuestion addQuestion(String teacherEmail, QuackslateQuestion question) {
        if (question == null || blank(question.getPrompt()) || blank(question.getTranslation())
                || blank(question.getCorrectAnswer()) || question.getOptions() == null
                || question.getOptions().length < 2 || question.getOptions().length > 12)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Add the scenario, Japanese sentence, answer and 2–12 word tiles");
        question.setId(null);
        question.setPrompt(question.getPrompt().trim());
        question.setTranslation(question.getTranslation().trim());
        question.setCorrectAnswer(question.getCorrectAnswer().trim());
        question.setOptions(java.util.Arrays.stream(question.getOptions())
                .map(tile -> tile == null ? "" : tile.strip()).toArray(String[]::new));
        List<String> tiles = List.of(question.getOptions());
        List<String> answerTiles = List.of(question.getCorrectAnswer().split("\\s+"));
        if (tiles.stream().anyMatch(String::isBlank) || new LinkedHashSet<>(tiles).size() != tiles.size()
                || answerTiles.stream().anyMatch(tile -> !tiles.contains(tile)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Each answer word must match one distinct word tile");
        question.setCategory(blank(question.getCategory()) ? "Teacher question" : question.getCategory().trim());
        question.setDifficulty(blank(question.getDifficulty()) ? "Easy" : question.getDifficulty().trim());
        question.setCreatedBy(teacherEmail);
        question.setApproved(true);
        question.setSystemAvailable(false); // Teacher-created content never changes solo mode.
        return questions.save(question);
    }

    public void deleteSession(String teacherEmail, String code) {
        QuackslateGameCode game = owned(teacherEmail, code);
        String state = status(game, Instant.now());
        if ("LIVE".equals(state) || "ENDED".equals(state) || !game.getJoinedStudentEmails().isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only unused drafts or upcoming codes can be deleted. Sessions with students keep their records.");
        content.deleteAll(content.findByGameCode(game.getGameCode()));
        games.delete(game);
    }

    public SessionView setQuestions(String teacherEmail, String code, List<String> ids) {
        QuackslateGameCode game = owned(teacherEmail, code);
        if (game.isPublished()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Published sessions cannot change their questions");
        List<String> unique = ids == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(ids));
        if (unique.isEmpty() || unique.size() > 30)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose 1–30 questions");
        Map<String, QuackslateQuestion> available = new LinkedHashMap<>();
        availableQuestions(teacherEmail).forEach(question -> available.put(question.getId(), question));
        if (unique.stream().anyMatch(id -> !available.containsKey(id)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A selected question is unavailable");
        game.setQuestionIds(unique);
        return view(games.save(game));
    }

    public SessionView publish(String teacherEmail, String code, Instant startsAt, Instant endsAt) {
        QuackslateGameCode game = owned(teacherEmail, code);
        Instant now = Instant.now();
        if (game.isPublished()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This session is already published");
        if (game.getQuestionIds().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Add at least one question before scheduling");
        if (startsAt == null || endsAt == null || startsAt.isBefore(now.minusSeconds(30))
                || !endsAt.isAfter(startsAt) || Duration.between(startsAt, endsAt).toHours() > 4)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose a future start and an end within four hours");
        Map<String, QuackslateQuestion> selected = new LinkedHashMap<>();
        questions.findAllById(game.getQuestionIds()).forEach(q -> selected.put(q.getId(), q));
        if (selected.size() != game.getQuestionIds().size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A selected question was removed");
        List<QuackslateContent> rows = new ArrayList<>();
        LinkedHashSet<Integer> allocatedIds = new LinkedHashSet<>();
        for (String id : game.getQuestionIds()) {
            QuackslateQuestion q = selected.get(id);
            int contentId;
            do { contentId = random.nextInt(1, 2_000_000_000); }
            while (allocatedIds.contains(contentId) || content.existsById(contentId));
            allocatedIds.add(contentId);
            QuackslateContent row = new QuackslateContent(contentId, q.getPrompt(), q.getTranslation(),
                    q.getCategory(), game.getGameCode(), q.getOptions(), q.getCorrectAnswer(), null);
            row.setExplanation(q.getExplanation());
            row.setQuestionOrder(rows.size());
            rows.add(row);
        }
        content.deleteAll(content.findByGameCode(game.getGameCode()));
        content.saveAll(rows);
        game.setStartsAt(startsAt);
        game.setEndsAt(endsAt);
        game.setPublished(true);
        return view(games.save(game));
    }

    public SessionView publicView(String code) {
        QuackslateGameCode game = require(code);
        if (!game.isPublished()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "This QuackSlate session is not ready yet");
        return view(game);
    }

    public SessionView join(String code, String email) {
        QuackslateGameCode game = require(code);
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        User learner = users.findByEmail(normalized);
        if (!game.isPublished() || learner == null || !"student".equalsIgnoreCase(learner.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The code is not available to this student");
        if ("ENDED".equals(status(game, Instant.now())))
            throw new ResponseStatusException(HttpStatus.GONE, "This QuackSlate session has ended");
        // addToSet is atomic when many learners join at the same time.
        mongo.updateFirst(Query.query(Criteria.where("gameCode").is(game.getGameCode())),
                new Update().addToSet("joinedStudentEmails", normalized), QuackslateGameCode.class);
        return view(require(code));
    }

    public Score saveScore(String code, Score submitted) {
        QuackslateGameCode game = require(code);
        String email = submitted.getEmail() == null ? "" : submitted.getEmail().trim().toLowerCase(Locale.ROOT);
        Instant now = Instant.now();
        if (!game.isPublished() || !game.getJoinedStudentEmails().contains(email)
                || now.isBefore(game.getStartsAt()) || now.isAfter(game.getEndsAt().plusSeconds(90)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "The student is not enrolled or the score window is closed");
        int max = game.getQuestionIds().size();
        if (submitted.getScore() < 0 || submitted.getScore() > max)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score is outside the question total");
        if (!blank(submitted.getClientAttemptId())) {
            Score prior = scores.findByEmailIgnoreCaseAndClientAttemptId(email,
                    submitted.getClientAttemptId()).orElse(null);
            if (prior != null) return prior;
        }
        User learner = users.findByEmail(email);
        if (learner == null || !"student".equalsIgnoreCase(learner.getRole()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student account not found");
        Score score = new Score();
        score.setEmail(email);
        score.setName((learner.getFname() + " " + learner.getLname()).trim());
        score.setGame("QUACKSLATE");
        score.setMode("TEACHER_CODED");
        score.setGameCode(game.getGameCode());
        score.setScore(submitted.getScore());
        score.setCorrectAnswers(submitted.getScore());
        score.setMaxScore(max);
        score.setTotalQuestions(max);
        score.setCompleted(submitted.isCompleted());
        score.setDate(LocalDate.ofInstant(now, SCHOOL_ZONE).toString());
        score.setPlayedAt(now);
        score.setClientAttemptId(submitted.getClientAttemptId());
        return scores.save(score);
    }

    public ScoreSheet sheet(String teacherEmail, String code) {
        QuackslateGameCode game = owned(teacherEmail, code);
        List<Score> attempts = scores.findByGameCodeIgnoreCaseAndGameIgnoreCaseAndModeIgnoreCase(
                game.getGameCode(), "QUACKSLATE", "TEACHER_CODED");
        List<ScoreRow> rows = game.getJoinedStudentEmails().stream().sorted().map(email -> {
            List<Score> personal = attempts.stream()
                    .filter(s -> email.equalsIgnoreCase(s.getEmail()))
                    .sorted(Comparator.comparing((Score s) -> s.getPlayedAt() == null
                            ? Instant.EPOCH : s.getPlayedAt()).reversed()).toList();
            User learner = users.findByEmail(email);
            String name = learner == null ? email : (learner.getFname() + " " + learner.getLname()).trim();
            List<ScoreAttempt> history = personal.stream().map(s -> new ScoreAttempt(s.getScore(),
                    s.getMaxScore(), percent(s), s.getPlayedAt(), s.isCompleted())).toList();
            Double latest = personal.isEmpty() ? null : percent(personal.get(0));
            Double highest = personal.stream().mapToDouble(this::percent).max().isPresent()
                    ? personal.stream().mapToDouble(this::percent).max().getAsDouble() : null;
            Double average = personal.isEmpty() ? null : Math.round(personal.stream()
                    .mapToDouble(this::percent).average().orElse(0) * 10.0) / 10.0;
            return new ScoreRow(email, name, personal.size(), latest, average, highest,
                    personal.isEmpty() ? null : personal.get(0).getPlayedAt(), history);
        }).toList();
        return new ScoreSheet(game.getGameCode(), rows.size(),
                (int) rows.stream().filter(row -> row.attempts() > 0).count(), rows);
    }

    private double percent(Score score) {
        return score.getMaxScore() <= 0 ? 0 : Math.round(score.getScore() * 1000.0
                / score.getMaxScore()) / 10.0;
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
