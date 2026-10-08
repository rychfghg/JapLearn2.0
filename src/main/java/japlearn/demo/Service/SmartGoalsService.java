package japlearn.demo.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import japlearn.demo.Entity.Classes;
import japlearn.demo.Entity.QuackTalkSession;
import japlearn.demo.Entity.ReplyCoachAttempt;
import japlearn.demo.Entity.ResponseRushProgress;
import japlearn.demo.Entity.Score;
import japlearn.demo.Entity.SituationalAttempt;
import japlearn.demo.Entity.Student;
import japlearn.demo.Entity.User;
import japlearn.demo.Entity.GuidedPhraseDailyPractice;
import japlearn.demo.Entity.ReplyCoachChapter;
import japlearn.demo.Entity.DialogueRelayProgress;
import japlearn.demo.Entity.SituationalRun;
import japlearn.demo.Repository.ClassesRepository;
import japlearn.demo.Repository.DialogueRelayProgressRepository;
import japlearn.demo.Repository.SituationalQuestionRepository;
import japlearn.demo.Repository.SituationalRunRepository;
import japlearn.demo.Repository.GuidedPhraseDailyPracticeRepository;
import japlearn.demo.Repository.ReplyCoachChapterRepository;
import japlearn.demo.Repository.QuackTalkSessionRepository;
import japlearn.demo.Repository.ReplyCoachAttemptRepository;
import japlearn.demo.Repository.ResponseRushProgressRepository;
import japlearn.demo.Repository.ScoreRepository;
import japlearn.demo.Repository.SituationalAttemptRepository;
import japlearn.demo.Repository.StudentRepository;
import japlearn.demo.Repository.UserRepository;

/**
 * Read-only evaluation of the four JapLearn 2.0 SMART objectives from existing game records.
 * Nothing here writes to student data. The rules are listed in {@link #RULES} and returned with
 * every report so a result can be reproduced from the same records and filters.
 */
@Service
public class SmartGoalsService {

    public static final String METHOD_VERSION = "smart-eval-v2";
    private static final ZoneId ZONE = ZoneId.of("Asia/Manila");
    /** A Talk with Sumi turn counts as a correct response when it is in Japanese and its context score reaches this. */
    public static final int SUMI_CONTEXT_PASS = 70;

    /** Evidence sources and the objective each one measures (RECOGNITION = Ahiru Rescue). */
    public static final Map<String, String> SOURCE_GOAL = Map.of(
            "RECOGNITION", "SMART1",
            "EXPRESSION_MATCH", "SMART1",
            "POLITENESS", "SMART3",
            "REPLY_COACH", "SMART2",
            "RESPONSE_RUSH", "SMART2",
            "DIALOGUE_RELAY", "SMART2",
            "TALK_WITH_SUMI", "SMART2");
    /** The 8 communication activities every eligible student is expected to complete (SMART 4). */
    public static final List<String> COMPLETION_ACTIVITIES = List.of("RECOGNITION", "EXPRESSION_MATCH", "POLITENESS",
            "REPLY_COACH", "RESPONSE_RUSH", "DIALOGUE_RELAY", "TALK_WITH_SUMI", "GUIDED_PHRASE");
    private static final Set<String> ALL_SOURCES = Set.copyOf(COMPLETION_ACTIVITIES);
    private static final Map<String, Set<String>> MODULES = Map.of(
            "QUACKSITUATE", Set.of("RECOGNITION", "EXPRESSION_MATCH", "POLITENESS"),
            "QUACKRESPONSE", Set.of("REPLY_COACH", "RESPONSE_RUSH", "DIALOGUE_RELAY"),
            "QUACKTALK", Set.of("TALK_WITH_SUMI", "GUIDED_PHRASE"));

    public static final List<String> RULES = List.of(
            "Accuracy = correct scored answers / total scored answers x 100, summed across eligible students (not an average of student percentages).",
            "Only the first evaluation attempt counts: the earliest completed run of each QuackSituate level/set, the earliest Response Rush and Dialogue Relay runs, the first Reply Coach attempt of each chapter, and the first Talk with Sumi session with per-turn scores, inside the selected period.",
            "QuackSituate games (Ahiru Rescue, Expression Match, Politeness) give one answer per question, so a run's correct/total already reflects the first scored answer of each item. In Reply Coach the first answer recorded for each dialogue node is used.",
            "Reply Coach and Response Rush answers rated BEST or ACCEPTABLE are correct; AWKWARD, IMPOLITE, RUDE and TIMEOUT are scored incorrect.",
            "Dialogue Relay: 25 dialogue choices plus the spoken bonus prompts, where a bonus counts as correct only when the reply is judged appropriate to the context (not by pronunciation).",
            "Talk with Sumi: each learner turn is one scored response; it is correct when Japanese was attempted and the context score is at least " + SUMI_CONTEXT_PASS + "/100. Pronunciation, accuracy, fluency and completeness scores are never used. Sessions saved before per-turn context scores were recorded are not measurable.",
            "SMART 3 uses QuackSituate Politeness plus Reply Coach questions an admin has explicitly tagged as politeness. Expression Match and Response Rush do not store per-question answers, so their items cannot be tagged and are not counted.",
            "Unfinished runs count: when a student's first attempt was never finished, the questions they actually answered before stopping are scored as correct or incorrect. Questions they never reached are not counted either way. Answers from unfinished runs are reported separately.",
            "Students without any records are 'No participation' and are not counted as zero.",
            "Completion = activities completed / (8 communication activities x eligible students) x 100: Ahiru Rescue, Expression Match, Politeness, Reply Coach, Response Rush, Dialogue Relay, Talk with Sumi and Guided Phrase Practice. Each activity counts once per student when finished at least once in the period.",
            "An activity is completed only from finished game progress records (QuackSituate runs marked completed, Reply Coach chapters marked COMPLETED, finished Response Rush and Dialogue Relay runs, Talk with Sumi sessions with at least one learner turn, completed Guided Phrase sessions). Started-but-unfinished activities count as not completed. Questionnaire answers are never used.",
            "A goal shows 'Insufficient Data' when its denominator is 0.");

    private final UserRepository users;
    private final StudentRepository students;
    private final ClassesRepository classes;
    private final SituationalAttemptRepository situational;
    private final ReplyCoachAttemptRepository replyCoach;
    private final ScoreRepository scores;
    private final ResponseRushProgressRepository rushProgress;
    private final QuackTalkSessionRepository quackTalk;
    private final ReplyCoachChapterRepository chapters;
    private final GuidedPhraseDailyPracticeRepository guidedPractice;
    private final SituationalRunRepository situationalRuns;
    private final DialogueRelayProgressRepository relayProgress;
    private final SituationalQuestionRepository situationalQuestions;

    public SmartGoalsService(UserRepository users, StudentRepository students, ClassesRepository classes,
            SituationalAttemptRepository situational, ReplyCoachAttemptRepository replyCoach, ScoreRepository scores,
            ResponseRushProgressRepository rushProgress, QuackTalkSessionRepository quackTalk,
            ReplyCoachChapterRepository chapters,
            GuidedPhraseDailyPracticeRepository guidedPractice, SituationalRunRepository situationalRuns,
            DialogueRelayProgressRepository relayProgress, SituationalQuestionRepository situationalQuestions) {
        this.situationalRuns = situationalRuns;
        this.relayProgress = relayProgress;
        this.situationalQuestions = situationalQuestions;
        this.chapters = chapters;
        this.guidedPractice = guidedPractice;
        this.users = users;
        this.students = students;
        this.classes = classes;
        this.situational = situational;
        this.replyCoach = replyCoach;
        this.scores = scores;
        this.rushProgress = rushProgress;
        this.quackTalk = quackTalk;
    }

    /** onlyStudentIds: when not null, the cohort is limited to these accounts (e.g. survey respondents matched by email). */
    public record Filters(LocalDate from, LocalDate to, String classCode, String studentId, String module, boolean includeUnapproved,
            Set<String> onlyStudentIds, String population) {
        public Filters(LocalDate from, LocalDate to, String classCode, String studentId, String module, boolean includeUnapproved) {
            this(from, to, classCode, studentId, module, includeUnapproved, null, "ALL_ELIGIBLE");
        }
    }

    /** Running tally for one objective (per student or for the cohort). */
    static final class Tally {
        int correct;
        int total;
        int unfinishedAnswers;
        boolean incomplete;
        Instant last;
        final Set<String> dates = new TreeSet<>();

        void add(int right, int scored, Instant at) {
            correct += right;
            total += scored;
            touch(at);
        }

        void addUnfinished(int right, int scored, Instant at) {
            add(right, scored, at);
            unfinishedAnswers += scored;
        }

        void touch(Instant at) {
            if (at == null) return;
            dates.add(at.atZone(ZONE).toLocalDate().toString());
            if (last == null || at.isAfter(last)) last = at;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("correct", correct);
            m.put("total", total);
            m.put("percentage", total > 0 ? round(correct * 100.0 / total) : null);
            m.put("status", total > 0 ? "MEASURED" : incomplete ? "INCOMPLETE" : "NO_PARTICIPATION");
            m.put("unfinishedAnswers", unfinishedAnswers);
            return m;
        }
    }

    /** One specific area (activity, level or chapter) inside an objective, summed over the cohort. */
    private static final class Area {
        int correct, total, started;
        final Set<String> students = new java.util.HashSet<>();
    }

    private static final Map<String, String> SOURCE_LABEL = Map.of(
            "RECOGNITION", "Ahiru Rescue", "EXPRESSION_MATCH", "Expression Match", "POLITENESS", "Politeness",
            "REPLY_COACH", "Reply Coach", "RESPONSE_RUSH", "Response Rush", "DIALOGUE_RELAY", "Dialogue Relay",
            "TALK_WITH_SUMI", "Talk with Sumi", "GUIDED_PHRASE", "Guided Phrase Practice");

    private static void area(Map<String, Map<String, Area>> areas, String goal, String label, int right, int scored, String email) {
        Area a = areas.computeIfAbsent(goal, k -> new LinkedHashMap<>()).computeIfAbsent(label, k -> new Area());
        a.correct += right;
        a.total += scored;
        a.students.add(email);
    }

    private static List<Map<String, Object>> areaList(Map<String, Area> areas) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (areas == null) return out;
        for (Map.Entry<String, Area> e : areas.entrySet()) {
            Area a = e.getValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", e.getKey());
            m.put("correct", a.correct);
            m.put("total", a.total);
            m.put("percentage", a.total > 0 ? round(a.correct * 100.0 / a.total) : null);
            m.put("students", a.students.size());
            m.put("started", a.started);
            out.add(m);
        }
        out.sort(Comparator.comparing(m -> m.get("percentage") == null ? 101.0 : (Double) m.get("percentage")));
        return out;
    }

    /** Where students stopped an unfinished first attempt: answers reached, out of the run length when known. */
    private static void stop(Map<String, List<int[]>> stops, String label, int reached, Integer outOf) {
        stops.computeIfAbsent(label, k -> new ArrayList<>()).add(new int[] { reached, outOf == null ? -1 : outOf });
    }

    private static List<Map<String, Object>> dropOff(Map<String, List<int[]>> stops) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, List<int[]>> e : stops.entrySet()) {
            List<int[]> list = e.getValue();
            Map<Integer, Integer> at = new java.util.TreeMap<>();
            int outOf = -1;
            for (int[] x : list) { at.merge(x[0], 1, Integer::sum); outOf = Math.max(outOf, x[1]); }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", e.getKey());
            m.put("students", list.size());
            m.put("outOf", outOf < 0 ? null : outOf);
            m.put("averageReached", round(list.stream().mapToInt(x -> x[0]).average().orElse(0)));
            List<Map<String, Object>> points = new ArrayList<>();
            for (Map.Entry<Integer, Integer> a : at.entrySet()) points.add(Map.of("answered", a.getKey(), "students", a.getValue()));
            points.sort((x, y) -> Integer.compare((Integer) y.get("students"), (Integer) x.get("students")));
            m.put("stopPoints", points);
            out.add(m);
        }
        out.sort((x, y) -> Integer.compare((Integer) y.get("students"), (Integer) x.get("students")));
        return out;
    }

    private static final class StudentRow {
        final User user;
        final String classCode;
        final String email;
        final Map<String, Tally> goals = new LinkedHashMap<>();
        int runsAttempted;
        int runsCompleted;
        int retries, hints, hintRecords, timeouts, incompleteActivities;
        int activitiesCompleted;
        final List<Map<String, Object>> activityStatus = new ArrayList<>();
        final Set<String> dates = new TreeSet<>();

        StudentRow(User user, String classCode) {
            this.user = user;
            this.classCode = classCode;
            this.email = user.getEmail().trim().toLowerCase(Locale.ROOT);
            for (String g : List.of("SMART1", "SMART2", "SMART3")) goals.put(g, new Tally());
        }
    }

    public Map<String, Object> report(Filters f) {
        Instant from = f.from() == null ? null : f.from().atStartOfDay(ZONE).toInstant();
        Instant to = f.to() == null ? null : f.to().plusDays(1).atStartOfDay(ZONE).toInstant();
        Set<String> sources = sourcesFor(f.module());
        Map<String, Integer> excluded = new LinkedHashMap<>();
        Map<String, Map<String, Area>> areas = new LinkedHashMap<>();
        Map<String, Integer> coachEvaluations = new LinkedHashMap<>();
        Map<String, List<int[]>> stops = new LinkedHashMap<>();
        Map<String, Integer> read = new LinkedHashMap<>();

        // ---- cohort
        Map<String, String> classOf = new java.util.HashMap<>();
        for (Student s : students.findAll()) {
            if (s.getEmail() != null && s.getClassCode() != null && !s.getClassCode().isBlank())
                classOf.put(s.getEmail().trim().toLowerCase(Locale.ROOT), s.getClassCode().trim());
        }
        Map<String, String> classTitles = new LinkedHashMap<>();
        for (Classes c : classes.findAll())
            if (c.getClassCodes() != null) classTitles.putIfAbsent(c.getClassCodes(), c.getClassTitle());

        List<User> studentUsers = users.findByRoleIgnoreCase("student").stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .filter(u -> f.includeUnapproved() || u.isApproved())
                .toList();
        List<Map<String, Object>> studentOptions = studentUsers.stream()
                .map(u -> Map.<String, Object>of("id", u.getId(), "code", code(u), "name", name(u)))
                .sorted(Comparator.comparing(m -> String.valueOf(m.get("name")).toLowerCase(Locale.ROOT)))
                .toList();

        Map<String, StudentRow> cohort = new LinkedHashMap<>();
        for (User u : studentUsers) {
            String cls = classOf.get(u.getEmail().trim().toLowerCase(Locale.ROOT));
            if (f.classCode() != null && !f.classCode().isBlank() && !f.classCode().equals(cls)) continue;
            if (f.studentId() != null && !f.studentId().isBlank() && !f.studentId().equals(u.getId())) continue;
            if (f.onlyStudentIds() != null && !f.onlyStudentIds().contains(u.getId())) continue;
            StudentRow row = new StudentRow(u, cls);
            cohort.put(row.email, row);
        }

        // ---- SMART 1 and 3: QuackSituate runs
        List<SituationalAttempt> sit = situational.findAll();
        read.put("situational_attempts", sit.size());
        Map<String, List<SituationalAttempt>> sitByStudent = groupByEmail(sit, SituationalAttempt::getEmail, cohort);
        List<SituationalRun> runs = situationalRuns.findAll();
        read.put("situational_runs_unfinished", runs.size());
        Map<String, List<SituationalRun>> runsByStudent = groupByEmail(runs, SituationalRun::getEmail, cohort);
        Map<String, Integer> runLength = new java.util.HashMap<>(); // questions per game/level, for "stopped at X of Y"
        for (String type : List.of("RECOGNITION", "EXPRESSION_MATCH", "POLITENESS")) {
            var qs = situationalQuestions.findByGameTypeIgnoreCaseAndActiveTrueOrderByOrderAsc(type);
            runLength.put(type + "|0", qs.size());
            for (var q : qs) runLength.merge(type + "|" + q.getLevel(), 1, Integer::sum);
        }
        for (StudentRow row : cohort.values()) {
            List<SituationalAttempt> mine = sitByStudent.getOrDefault(row.email, List.of());
            Map<String, SituationalAttempt> firstRun = new LinkedHashMap<>();
            Map<String, SituationalAttempt> partials = new LinkedHashMap<>();
            for (SituationalAttempt a : mine) {
                String type = upper(a.getGameType());
                if (!SOURCE_GOAL.containsKey(type) || !sources.contains(type) || !inPeriod(a.getCompletedAt(), from, to)) continue;
                row.runsAttempted++;
                addDay(row.dates, a.getCompletedAt());
                Tally tally = row.goals.get(SOURCE_GOAL.get(type));
                if (!a.isCompleted()) { tally.incomplete = true; partials.merge(type + "|" + a.getLevel(), a, (x, y) -> answeredIn(y) > answeredIn(x) ? y : x); continue; }
                row.runsCompleted++;
                if (a.getTotalQuestions() <= 0 || a.getCorrectAnswers() < 0 || a.getCorrectAnswers() > a.getTotalQuestions()) {
                    excluded.merge("situational: invalid correct/total", 1, Integer::sum);
                    continue;
                }
                String key = type + "|" + a.getLevel() + "|" + a.getSetNumber();
                SituationalAttempt seen = firstRun.get(key);
                if (seen == null || before(a.getCompletedAt(), seen.getCompletedAt())) {
                    if (seen != null) { excluded.merge("situational: repeat runs after the first", 1, Integer::sum); row.retries++; }
                    firstRun.put(key, a);
                } else {
                    excluded.merge("situational: repeat runs after the first", 1, Integer::sum);
                    row.retries++;
                }
            }
            for (SituationalAttempt a : firstRun.values()) {
                if (a.getHintsUsed() != null) { row.hints += a.getHintsUsed(); row.hintRecords++; }
                String type = upper(a.getGameType());
                row.goals.get(SOURCE_GOAL.get(type)).add(a.getCorrectAnswers(), a.getTotalQuestions(), a.getCompletedAt());
                area(areas, SOURCE_GOAL.get(type), SOURCE_LABEL.get(type) + (type.equals("RECOGNITION") || a.getLevel() <= 0 ? "" : " · Level " + a.getLevel()),
                        a.getCorrectAnswers(), a.getTotalQuestions(), row.email);
            }
            // Game/levels with a finished run use it; otherwise the unfinished run is the first attempt.
            Set<String> finished = new java.util.HashSet<>();
            for (SituationalAttempt a : firstRun.values()) {
                String type = upper(a.getGameType());
                finished.add(type.equals("RECOGNITION") ? "RECOGNITION|0" : type + "|" + a.getLevel());
            }
            Map<String, int[]> unfinished = new LinkedHashMap<>(); // key -> {correct, answered}
            Map<String, Instant> unfinishedAt = new java.util.HashMap<>();
            for (SituationalAttempt a : partials.values()) { // Politeness saves a partial result when the learner leaves
                String key = "POLITENESS|" + a.getLevel();
                int answered = answeredIn(a);
                if (finished.contains(key) || answered <= 0) continue;
                unfinished.put(key, new int[] { Math.min(a.getCorrectAnswers(), answered), answered });
                unfinishedAt.put(key, a.getCompletedAt());
            }
            for (SituationalRun r : runsByStudent.getOrDefault(row.email, List.of())) { // Ahiru Rescue / Expression Match current run
                String g = upper(r.getGameType());
                String key;
                int answered;
                if (g.equals("RECOGNITION")) { key = "RECOGNITION|0"; answered = r.getCorrectCount() + r.getEasyMistakes() + r.getHardMistakes(); }
                else if (g.startsWith("EXPRESSION_MATCH_LEVEL_")) {
                    int level;
                    try { level = Integer.parseInt(g.substring("EXPRESSION_MATCH_LEVEL_".length())); } catch (Exception e) { continue; }
                    key = "EXPRESSION_MATCH|" + level;
                    answered = r.getCorrectCount() + r.getEasyMistakes(); // the app stores Expression Match mistakes once in easyMistakes
                } else continue;
                String type = key.substring(0, key.indexOf('|'));
                if (!sources.contains(type) || !inPeriod(r.getUpdatedAt(), from, to) || finished.contains(key) || answered <= 0) continue;
                row.runsAttempted++;
                addDay(row.dates, r.getUpdatedAt());
                unfinished.put(key, new int[] { Math.min(r.getCorrectCount(), answered), answered });
                unfinishedAt.put(key, r.getUpdatedAt());
            }
            for (Map.Entry<String, int[]> e : unfinished.entrySet()) {
                String type = e.getKey().substring(0, e.getKey().indexOf('|'));
                int level = Integer.parseInt(e.getKey().substring(e.getKey().indexOf('|') + 1));
                if (!sources.contains(type)) continue;
                int[] v = e.getValue();
                row.goals.get(SOURCE_GOAL.get(type)).addUnfinished(v[0], v[1], unfinishedAt.get(e.getKey()));
                String label = SOURCE_LABEL.get(type) + (level <= 0 ? "" : " · Level " + level);
                area(areas, SOURCE_GOAL.get(type), label, v[0], v[1], row.email);
                stop(stops, label, v[1], runLength.get(type + "|" + level));
            }
        }

        // ---- SMART 2: Reply Coach (item level)
        List<ReplyCoachAttempt> coach = replyCoach.findAll();
        read.put("reply_coach_attempts", coach.size());
        if (sources.contains("REPLY_COACH")) {
            // Reply Coach questions an admin explicitly tagged as assessing politeness also count for SMART 3.
            Set<String> politenessNodes = new java.util.HashSet<>();
            Map<String, String> chapterTitles = new java.util.HashMap<>();
            for (ReplyCoachChapter chapter : chapters.findAll())
                for (ReplyCoachChapter.StoryNode node : chapter.getNodes() == null ? List.<ReplyCoachChapter.StoryNode>of() : chapter.getNodes())
                    if (node.isAssessesPoliteness() && node.getId() != null) politenessNodes.add(chapter.getId() + "|" + node.getId());
            for (ReplyCoachChapter chapter : chapters.findAll())
                if (chapter.getId() != null) chapterTitles.put(chapter.getId(), chapter.getTitle() == null || chapter.getTitle().isBlank() ? chapter.getId() : chapter.getTitle());
            read.put("reply_coach_politeness_tagged_questions", politenessNodes.size());
            Map<String, List<ReplyCoachAttempt>> coachByStudent = groupByEmail(coach, ReplyCoachAttempt::getEmail, cohort);
            for (StudentRow row : cohort.values()) {
                Map<String, ReplyCoachAttempt> firstAttempt = new LinkedHashMap<>();
                for (ReplyCoachAttempt a : coachByStudent.getOrDefault(row.email, List.of())) {
                    if (!inPeriod(a.getStartedAt(), from, to) || a.getChapterId() == null) continue;
                    row.runsAttempted++;
                    addDay(row.dates, a.getStartedAt());
                    if ("COMPLETED".equalsIgnoreCase(a.getStatus())) row.runsCompleted++;
                    ReplyCoachAttempt seen = firstAttempt.get(a.getChapterId());
                    if (seen == null || earlierCoach(a, seen)) firstAttempt.put(a.getChapterId(), a);
                    if (seen != null) row.retries++; // a second attempt at the same chapter
                }
                Tally tally = row.goals.get("SMART2");
                Tally polite = row.goals.get("SMART3");
                for (ReplyCoachAttempt a : firstAttempt.values()) {
                    Set<String> nodes = new java.util.HashSet<>();
                    int right = 0, scored = 0, politeRight = 0, politeScored = 0;
                    Instant last = null;
                    for (ReplyCoachAttempt.AnswerRecord ans : a.getAnswers() == null ? List.<ReplyCoachAttempt.AnswerRecord>of() : a.getAnswers()) {
                        if (ans.getNodeId() == null || ans.getEvaluation() == null || ans.getEvaluation().isBlank()) continue;
                        if (!nodes.add(ans.getNodeId())) { excluded.merge("reply coach: repeated answers to the same node", 1, Integer::sum); continue; }
                        scored++;
                        boolean good = isGood(ans.getEvaluation());
                        coachEvaluations.merge(upper(ans.getEvaluation()), 1, Integer::sum);
                        if ("TIMEOUT".equals(upper(ans.getEvaluation()))) row.timeouts++;
                        if (ans.getHintUsed() != null) { row.hintRecords++; if (ans.getHintUsed()) row.hints++; }
                        if (good) right++;
                        if (politenessNodes.contains(a.getChapterId() + "|" + ans.getNodeId())) {
                            politeScored++;
                            if (good) politeRight++;
                        }
                        if (ans.getAnsweredAt() != null && (last == null || ans.getAnsweredAt().isAfter(last))) last = ans.getAnsweredAt();
                    }
                    if (scored == 0) { tally.incomplete = true; continue; }
                    Instant at = last == null ? a.getStartedAt() : last;
                    String chapterName = chapterTitles.getOrDefault(a.getChapterId(), a.getChapterId());
                    if ("COMPLETED".equalsIgnoreCase(a.getStatus())) tally.add(right, scored, at);
                    else { tally.addUnfinished(right, scored, at); stop(stops, "Reply Coach · " + chapterName, scored, null); }
                    area(areas, "SMART2", "Reply Coach · " + chapterName, right, scored, row.email);
                    if (politeScored > 0) {
                        polite.add(politeRight, politeScored, at);
                        area(areas, "SMART3", "Reply Coach (politeness-tagged) · " + chapterName, politeRight, politeScored, row.email);
                    }
                }
            }
        }

        // ---- SMART 2: Response Rush and Dialogue Relay (run level)
        List<Score> allScores = scores.findAll();
        read.put("scores", allScores.size());
        Map<String, List<Score>> scoresByStudent = groupByEmail(allScores, Score::getEmail, cohort);
        Map<String, ResponseRushProgress> openRush = !sources.contains("RESPONSE_RUSH") ? Map.of() : rushProgress.findAll().stream()
                .filter(p -> p.getEmail() != null)
                .collect(Collectors.toMap(p -> p.getEmail().trim().toLowerCase(Locale.ROOT), p -> p, (a, b) -> a));
        Map<String, DialogueRelayProgress> openRelay = !sources.contains("DIALOGUE_RELAY") ? Map.of() : relayProgress.findAll().stream()
                .filter(p -> p.getEmail() != null)
                .collect(Collectors.toMap(p -> p.getEmail().trim().toLowerCase(Locale.ROOT), p -> p, (a, b) -> a));
        for (String[] game : new String[][] { { "RESPONSE_RUSH", "QUACKRESPONSE_RUSH", "response rush" }, { "DIALOGUE_RELAY", "QUACKRESPONSE_RELAY", "dialogue relay" } }) {
            if (!sources.contains(game[0])) continue;
            for (StudentRow row : cohort.values()) {
                Score first = null;
                for (Score s : scoresByStudent.getOrDefault(row.email, List.of())) {
                    if (!game[1].equalsIgnoreCase(s.getGame())) continue;
                    Instant at = scoreTime(s);
                    if (!inPeriod(at, from, to)) continue;
                    row.runsAttempted++;
                    addDay(row.dates, at);
                    if (!s.isCompleted()) continue;
                    row.runsCompleted++;
                    if (s.getTotalQuestions() <= 0 || s.getCorrectAnswers() > s.getTotalQuestions()) {
                        excluded.merge(game[2] + ": invalid correct/total", 1, Integer::sum);
                        continue;
                    }
                    if (first == null || before(at, scoreTime(first))) {
                        if (first != null) { excluded.merge(game[2] + ": repeat runs after the first", 1, Integer::sum); row.retries++; }
                        first = s;
                    } else {
                        excluded.merge(game[2] + ": repeat runs after the first", 1, Integer::sum);
                        row.retries++;
                    }
                }
                Tally tally = row.goals.get("SMART2");
                if (first != null) {
                    tally.add(first.getCorrectAnswers(), first.getTotalQuestions(), scoreTime(first));
                    area(areas, "SMART2", SOURCE_LABEL.get(game[0]), first.getCorrectAnswers(), first.getTotalQuestions(), row.email);
                }
                else if (game[0].equals("RESPONSE_RUSH")) {
                    ResponseRushProgress open = openRush.get(row.email);
                    if (open != null && !open.isCompleted() && open.getAnswers() != null && !open.getAnswers().isEmpty()
                            && inPeriod(open.getUpdatedAt(), from, to)) {
                        int answered = 0, right = 0;
                        for (Map<String, Object> ans : open.getAnswers()) {
                            Object ev = ans.get("evaluation");
                            if (ev == null || String.valueOf(ev).isBlank()) continue;
                            answered++;
                            if (isGood(String.valueOf(ev))) right++;
                        }
                        if (answered > 0) {
                            row.runsAttempted++;
                            tally.addUnfinished(right, answered, open.getUpdatedAt());
                            area(areas, "SMART2", "Response Rush", right, answered, row.email);
                            stop(stops, "Response Rush", answered, null);
                        } else tally.incomplete = true;
                    }
                } else {
                    DialogueRelayProgress open = openRelay.get(row.email);
                    if (open != null && inPeriod(open.getUpdatedAt(), from, to)) {
                        int answered = open.getAnswered() + open.getBonusAnswered();
                        int right = Math.min(answered, open.getCorrect() + open.getBonusCorrect());
                        if (answered > 0) {
                            row.runsAttempted++;
                            tally.addUnfinished(right, answered, open.getUpdatedAt());
                            area(areas, "SMART2", "Dialogue Relay", right, answered, row.email);
                            stop(stops, "Dialogue Relay", answered, 30);
                        }
                    }
                }
            }
        }

        // ---- SMART 2: Talk with Sumi (per learner turn, context score only)
        boolean needTalk = sources.contains("TALK_WITH_SUMI");
        List<QuackTalkSession> talks = needTalk ? quackTalk.findAll() : List.of();
        read.put("quack_talk_sessions", talks.size());
        Map<String, List<QuackTalkSession>> talksByStudent = groupByEmail(talks, QuackTalkSession::getEmail, cohort);
        if (sources.contains("TALK_WITH_SUMI")) {
            for (StudentRow row : cohort.values()) {
                QuackTalkSession first = null;
                for (QuackTalkSession t : talksByStudent.getOrDefault(row.email, List.of())) {
                    if (!"TALK_WITH_SUMI".equalsIgnoreCase(t.getRoomType()) || !inPeriod(t.getPracticedAt(), from, to)) continue;
                    row.runsAttempted++;
                    addDay(row.dates, t.getPracticedAt());
                    if (t.isCompleted()) row.runsCompleted++;
                    if (t.getContextEvaluations() == null || t.getContextEvaluations().isEmpty()) {
                        excluded.merge("talk with sumi: sessions without per-turn context scores (saved before tracking)", 1, Integer::sum);
                        continue;
                    }
                    if (first == null || before(t.getPracticedAt(), first.getPracticedAt())) {
                        if (first != null) { excluded.merge("talk with sumi: repeat sessions after the first", 1, Integer::sum); row.retries++; }
                        first = t;
                    } else { excluded.merge("talk with sumi: repeat sessions after the first", 1, Integer::sum); row.retries++; }
                }
                if (first == null) continue;
                int right = 0, scored = 0;
                for (Map<String, Object> turn : first.getContextEvaluations()) {
                    Object score = turn.get("contextScore");
                    if (!(score instanceof Number n)) continue;
                    scored++;
                    if (Boolean.TRUE.equals(turn.get("japaneseAttempted")) && n.intValue() >= SUMI_CONTEXT_PASS) right++;
                }
                if (scored > 0) {
                    row.goals.get("SMART2").add(right, scored, first.getPracticedAt());
                    area(areas, "SMART2", "Talk with Sumi", right, scored, row.email);
                }
            }
        }

        // ---- SMART 4: completion of the 8 communication activities (no assignments)
        List<GuidedPhraseDailyPractice> guided = sources.contains("GUIDED_PHRASE") ? guidedPractice.findAll() : List.of();
        read.put("guided_phrase_daily_practices", guided.size());
        Map<String, List<GuidedPhraseDailyPractice>> guidedByStudent = groupByEmail(guided, GuidedPhraseDailyPractice::getEmail, cohort);
        Map<String, List<ReplyCoachAttempt>> coachAll = groupByEmail(coach, ReplyCoachAttempt::getEmail, cohort);
        List<String> activities = COMPLETION_ACTIVITIES.stream().filter(sources::contains).toList();
        int completedTotal = 0, startedTotal = 0;
        Instant lastCompletion = null;
        int studentsWithAnyRecord = 0;
        for (StudentRow row : cohort.values()) {
            boolean any = false;
            for (String key : activities) {
                Instant done = completedAt(key, row.email, from, to, sitByStudent, coachAll, scoresByStudent, talksByStudent, guidedByStudent);
                String status;
                if (done != null) {
                    status = "COMPLETED";
                    row.activitiesCompleted++;
                    completedTotal++;
                    if (lastCompletion == null || done.isAfter(lastCompletion)) lastCompletion = done;
                } else if (started(key, row.email, from, to, sitByStudent, coachAll, scoresByStudent, talksByStudent, guidedByStudent, openRush)
                        || startedUnfinished(key, row.email, from, to, runsByStudent, openRelay)) {
                    status = "STARTED";
                    row.incompleteActivities++;
                    startedTotal++;
                } else status = "NOT_STARTED";
                if (!status.equals("NOT_STARTED")) any = true;
                Area act = areas.computeIfAbsent("SMART4", k -> new LinkedHashMap<>()).computeIfAbsent(SOURCE_LABEL.get(key), k -> new Area());
                act.total++;
                if (status.equals("COMPLETED")) { act.correct++; act.students.add(row.email); }
                if (status.equals("STARTED")) act.started++;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("key", key);
                item.put("status", status);
                item.put("completedAt", done);
                row.activityStatus.add(item);
            }
            if (any) studentsWithAnyRecord++;
        }
        int expected = activities.size() * cohort.size();

        // ---- cohort totals
        int eligible = cohort.size();
        List<Map<String, Object>> goals = new ArrayList<>();
        goals.add(goal("SMART1", "Situational Communication Accuracy", 80, cohort, "SMART1",
                "Game records: QuackSituate Ahiru Rescue and Expression Match runs",
                List.of("Ahiru Rescue runs that end in game over are not saved by the app, so only finished runs are measured; this can overstate accuracy.",
                        "Per-question answers are not stored for QuackSituate; each run's saved correct/total is used.")));
        goals.add(goal("SMART2", "Conversational Response Accuracy", 75, cohort, "SMART2",
                "Game records: Reply Coach (per answer), Response Rush and Dialogue Relay (per run), Talk with Sumi (per turn, context score)",
                List.of("Talk with Sumi counts only sessions saved after per-turn context scores started being recorded; earlier sessions kept only an average and are not measurable.",
                        "A Sumi turn counts as correct at a context score of " + SUMI_CONTEXT_PASS + "/100 or more with Japanese attempted. This pass mark is a design choice of the evaluation, not a score the app shows students.",
                        "Guided Phrase Practice and all pronunciation, fluency, accuracy and completeness scores are not used for response accuracy.")));
        goals.add(goal("SMART3", "Japanese Politeness Recognition Accuracy", 75, cohort, "SMART3",
                "Game records: QuackSituate Politeness runs and Reply Coach questions tagged as politeness",
                List.of("Reply Coach questions count only after an admin tags them as politeness in the Reply Coach editor; untagged questions are not assumed to measure politeness.",
                        "Expression Match and Response Rush save only run totals, not per-question answers, so politeness items inside them cannot be separated and are not counted.")));

        Map<String, Object> g4 = new LinkedHashMap<>();
        g4.put("key", "SMART4");
        g4.put("name", "Communication Activity Completion Rate");
        g4.put("target", 80);
        g4.put("correct", completedTotal);
        g4.put("total", expected);
        g4.put("percentage", expected > 0 ? round(completedTotal * 100.0 / expected) : null);
        g4.put("status", expected == 0 || studentsWithAnyRecord == 0 ? "INSUFFICIENT_DATA" : completedTotal * 100.0 / expected >= 80 ? "ACHIEVED" : "NOT_ACHIEVED");
        g4.put("eligibleStudents", eligible);
        g4.put("participatingStudents", studentsWithAnyRecord);
        g4.put("noParticipation", eligible - studentsWithAnyRecord);
        g4.put("incomplete", startedTotal);
        g4.put("activities", activities);
        g4.put("lastUpdated", lastCompletion);
        g4.put("evidence", "Game progress records for " + activities.size() + " communication activities per eligible student (" + activities.size() + " x " + eligible + " = " + expected + ")");
        g4.put("limitations", List.of(
                "Every eligible student is expected to complete each of the " + activities.size() + " activities once in the selected period; an activity a student never opened counts as not completed.",
                "'Incomplete' here is the number of activities that were started but not finished; they are counted as not completed.",
                "Completion is read only from game progress records, never from questionnaire answers."));
        goals.add(g4);
        for (Map<String, Object> g : goals) g.put("areas", areaList(areas.get(String.valueOf(g.get("key")))));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (StudentRow row : cohort.values()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("studentId", row.user.getId());
            r.put("code", code(row.user));
            r.put("name", name(row.user));
            r.put("classCode", row.classCode);
            r.put("classTitle", row.classCode == null ? null : classTitles.get(row.classCode));
            for (Map.Entry<String, Tally> e : row.goals.entrySet()) r.put(e.getKey(), e.getValue().toMap());
            Map<String, Object> s4 = new LinkedHashMap<>();
            s4.put("correct", row.activitiesCompleted);
            s4.put("total", activities.size());
            s4.put("percentage", activities.isEmpty() ? null : round(row.activitiesCompleted * 100.0 / activities.size()));
            s4.put("status", activities.isEmpty() ? "NO_PARTICIPATION" : "MEASURED");
            s4.put("activities", row.activityStatus);
            r.put("SMART4", s4);
            r.put("runsAttempted", row.runsAttempted);
            r.put("runsCompleted", row.runsCompleted);
            r.put("signals", signals(row));
            r.put("assessmentDates", new ArrayList<>(row.dates));
            rows.add(r);
        }
        rows.sort(Comparator.comparing(m -> String.valueOf(m.get("name")).toLowerCase(Locale.ROOT)));

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("from", f.from());
        filters.put("to", f.to());
        filters.put("classCode", f.classCode());
        filters.put("studentId", f.studentId());
        filters.put("module", f.module() == null || f.module().isBlank() ? "ALL" : f.module().toUpperCase(Locale.ROOT));
        filters.put("includeUnapproved", f.includeUnapproved());
        filters.put("population", f.population() == null ? "ALL_ELIGIBLE" : f.population());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("methodVersion", METHOD_VERSION);
        out.put("computedAt", Instant.now());
        out.put("filters", filters);
        out.put("rules", RULES);
        out.put("sources", sources);
        out.put("eligibleStudents", eligible);
        out.put("goals", goals);
        out.put("students", rows);
        out.put("classes", classTitles.entrySet().stream()
                .map(e -> { Map<String, Object> c = new LinkedHashMap<>(); c.put("classCode", e.getKey()); c.put("classTitle", e.getValue()); return c; })
                .toList());
        out.put("studentOptions", studentOptions);
        out.put("audit", Map.of("recordsRead", read, "recordsExcluded", excluded));
        out.put("replyCoachEvaluations", coachEvaluations);
        out.put("dropOff", dropOff(stops));
        Map<String, Object> cohortSignals = new LinkedHashMap<>();
        for (String k : List.of("incorrect", "retries", "hints", "timeouts", "incompleteActivities")) {
            int sum = 0;
            for (Map<String, Object> r : rows) {
                Object v = ((Map<?, ?>) r.get("signals")).get(k);
                if (v instanceof Number n) sum += n.intValue();
            }
            cohortSignals.put(k, sum);
        }
        cohortSignals.put("hintRecords", cohort.values().stream().mapToInt(x -> x.hintRecords).sum());
        cohortSignals.put("technicalFailures", null);
        cohortSignals.put("notes", List.of(
                "Incorrect = scored answers that were wrong in first evaluation attempts (SMART 1-3).",
                "Retries = runs, chapter attempts or Sumi sessions played again after the first one in the period; they never change the score.",
                "Hints are recorded only for Ahiru Rescue runs and Reply Coach answers saved after hint tracking was added; older records show no hint data.",
                "Timeouts = Reply Coach answers rated TIMEOUT. Response Rush and Dialogue Relay save only run totals, so their timeouts are counted as incorrect answers, not separately.",
                "Incomplete = activities a student started but did not finish in the period.",
                "Technical failures (failed speech or AI assessments, failed saves) are not stored by the app, so they cannot be counted."));
        out.put("signals", cohortSignals);
        return out;
    }

    private static Map<String, Object> signals(StudentRow row) {
        Map<String, Object> s = new LinkedHashMap<>();
        int incorrect = 0;
        for (String g : List.of("SMART1", "SMART2", "SMART3")) { Tally t = row.goals.get(g); incorrect += t.total - t.correct; }
        s.put("incorrect", incorrect);
        s.put("retries", row.retries);
        s.put("hints", row.hintRecords > 0 ? row.hints : null);
        s.put("timeouts", row.timeouts);
        s.put("incompleteActivities", row.incompleteActivities);
        s.put("technicalFailures", null);
        return s;
    }

    private Map<String, Object> goal(String key, String name, int target, Map<String, StudentRow> cohort, String goalKey,
            String evidence, List<String> limitations) {
        int correct = 0, total = 0, participating = 0, incomplete = 0, none = 0, unfinishedAnswers = 0, unfinishedStudents = 0;
        Instant last = null;
        for (StudentRow row : cohort.values()) {
            Tally t = row.goals.get(goalKey);
            unfinishedAnswers += t.unfinishedAnswers;
            if (t.unfinishedAnswers > 0) unfinishedStudents++;
            correct += t.correct;
            total += t.total;
            if (t.total > 0) participating++;
            else if (t.incomplete) incomplete++;
            else none++;
            if (t.last != null && (last == null || t.last.isAfter(last))) last = t.last;
        }
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("key", key);
        g.put("name", name);
        g.put("target", target);
        g.put("correct", correct);
        g.put("total", total);
        g.put("percentage", total > 0 ? round(correct * 100.0 / total) : null);
        g.put("status", total == 0 ? "INSUFFICIENT_DATA" : correct * 100.0 / total >= target ? "ACHIEVED" : "NOT_ACHIEVED");
        g.put("unfinishedAnswers", unfinishedAnswers);
        g.put("unfinishedStudents", unfinishedStudents);
        g.put("eligibleStudents", cohort.size());
        g.put("participatingStudents", participating);
        g.put("noParticipation", none);
        g.put("incomplete", incomplete);
        g.put("lastUpdated", last);
        g.put("evidence", evidence);
        g.put("limitations", limitations);
        return g;
    }

    /**
     * When the student first finished this activity inside the period, or null. Only finished records count:
     * QuackSituate runs marked completed, Reply Coach chapters marked COMPLETED, finished Response Rush and
     * Dialogue Relay runs, Talk with Sumi sessions with at least one learner turn, completed Guided Phrase sessions.
     */
    private Instant completedAt(String key, String email, Instant from, Instant to,
            Map<String, List<SituationalAttempt>> sit, Map<String, List<ReplyCoachAttempt>> coach,
            Map<String, List<Score>> scoreMap, Map<String, List<QuackTalkSession>> talks,
            Map<String, List<GuidedPhraseDailyPractice>> guided) {
        Instant best = null;
        switch (key) {
            case "RECOGNITION", "EXPRESSION_MATCH", "POLITENESS" -> {
                for (SituationalAttempt s : sit.getOrDefault(email, List.of()))
                    if (s.isCompleted() && key.equals(upper(s.getGameType())) && s.getCompletedAt() != null && inPeriod(s.getCompletedAt(), from, to))
                        best = earliest(best, s.getCompletedAt());
            }
            case "REPLY_COACH" -> {
                for (ReplyCoachAttempt r : coach.getOrDefault(email, List.of()))
                    if ("COMPLETED".equalsIgnoreCase(r.getStatus()) && r.getCompletedAt() != null && inPeriod(r.getCompletedAt(), from, to))
                        best = earliest(best, r.getCompletedAt());
            }
            case "RESPONSE_RUSH", "DIALOGUE_RELAY" -> {
                String game = key.equals("RESPONSE_RUSH") ? "QUACKRESPONSE_RUSH" : "QUACKRESPONSE_RELAY";
                for (Score s : scoreMap.getOrDefault(email, List.of())) {
                    Instant at = scoreTime(s);
                    if (s.isCompleted() && game.equalsIgnoreCase(s.getGame()) && at != null && inPeriod(at, from, to)) best = earliest(best, at);
                }
            }
            case "TALK_WITH_SUMI" -> {
                for (QuackTalkSession t : talks.getOrDefault(email, List.of()))
                    if (t.isCompleted() && t.getConversationTurns() > 0 && "TALK_WITH_SUMI".equalsIgnoreCase(t.getRoomType())
                            && t.getPracticedAt() != null && inPeriod(t.getPracticedAt(), from, to)) best = earliest(best, t.getPracticedAt());
            }
            case "GUIDED_PHRASE" -> {
                for (GuidedPhraseDailyPractice g : guided.getOrDefault(email, List.of())) {
                    Instant at = guidedDay(g);
                    if (g.getSessionsCompleted() > 0 && at != null && inPeriod(at, from, to))
                        best = earliest(best, g.getUpdatedAt() != null ? g.getUpdatedAt() : at);
                }
            }
            default -> { }
        }
        return best;
    }

    /** Whether the student opened the activity in the period (any record), even without finishing it. */
    private boolean started(String key, String email, Instant from, Instant to,
            Map<String, List<SituationalAttempt>> sit, Map<String, List<ReplyCoachAttempt>> coach,
            Map<String, List<Score>> scoreMap, Map<String, List<QuackTalkSession>> talks,
            Map<String, List<GuidedPhraseDailyPractice>> guided, Map<String, ResponseRushProgress> openRush) {
        return switch (key) {
            case "RECOGNITION", "EXPRESSION_MATCH", "POLITENESS" -> sit.getOrDefault(email, List.of()).stream()
                    .anyMatch(s -> key.equals(upper(s.getGameType())) && inPeriod(s.getCompletedAt(), from, to));
            case "REPLY_COACH" -> coach.getOrDefault(email, List.of()).stream().anyMatch(r -> inPeriod(r.getStartedAt(), from, to));
            case "RESPONSE_RUSH" -> {
                ResponseRushProgress p = openRush.get(email);
                yield (p != null && p.getAnswers() != null && !p.getAnswers().isEmpty() && inPeriod(p.getUpdatedAt(), from, to))
                        || scoreMap.getOrDefault(email, List.of()).stream().anyMatch(s -> "QUACKRESPONSE_RUSH".equalsIgnoreCase(s.getGame()) && inPeriod(scoreTime(s), from, to));
            }
            case "DIALOGUE_RELAY" -> scoreMap.getOrDefault(email, List.of()).stream()
                    .anyMatch(s -> "QUACKRESPONSE_RELAY".equalsIgnoreCase(s.getGame()) && inPeriod(scoreTime(s), from, to));
            case "TALK_WITH_SUMI" -> talks.getOrDefault(email, List.of()).stream()
                    .anyMatch(t -> "TALK_WITH_SUMI".equalsIgnoreCase(t.getRoomType()) && inPeriod(t.getPracticedAt(), from, to));
            case "GUIDED_PHRASE" -> guided.getOrDefault(email, List.of()).stream()
                    .anyMatch(g -> g.getSessionsStarted() > 0 && inPeriod(guidedDay(g), from, to));
            default -> false;
        };
    }

    /** Unfinished runs saved on the server (Ahiru Rescue, Expression Match, Dialogue Relay) also mean the activity was started. */
    private static boolean startedUnfinished(String key, String email, Instant from, Instant to,
            Map<String, List<SituationalRun>> runs, Map<String, DialogueRelayProgress> relay) {
        return switch (key) {
            case "RECOGNITION" -> runs.getOrDefault(email, List.of()).stream()
                    .anyMatch(r -> "RECOGNITION".equalsIgnoreCase(r.getGameType()) && inPeriod(r.getUpdatedAt(), from, to));
            case "EXPRESSION_MATCH" -> runs.getOrDefault(email, List.of()).stream()
                    .anyMatch(r -> upper(r.getGameType()).startsWith("EXPRESSION_MATCH_LEVEL_") && inPeriod(r.getUpdatedAt(), from, to));
            case "DIALOGUE_RELAY" -> relay.containsKey(email) && inPeriod(relay.get(email).getUpdatedAt(), from, to);
            default -> false;
        };
    }

    /** Questions answered in a Politeness partial save: the app's explicit count, or (older saves) the question reached. */
    private static int answeredIn(SituationalAttempt a) {
        if (a.getAnsweredCount() != null) return Math.max(0, a.getAnsweredCount());
        return Math.max(Math.max(0, a.getSetNumber()), Math.max(0, a.getCorrectAnswers()));
    }

    private static Instant guidedDay(GuidedPhraseDailyPractice g) {
        if (g.getPracticeDate() == null) return null;
        try { return LocalDate.parse(g.getPracticeDate()).atStartOfDay(ZONE).toInstant(); } catch (Exception e) { return null; }
    }

    private static Set<String> sourcesFor(String module) {
        String m = module == null ? "" : module.trim().toUpperCase(Locale.ROOT);
        if (m.isEmpty() || m.equals("ALL")) return ALL_SOURCES;
        if (MODULES.containsKey(m)) return MODULES.get(m);
        if (ALL_SOURCES.contains(m)) return Set.of(m);
        return ALL_SOURCES;
    }

    private static <T> Map<String, List<T>> groupByEmail(List<T> items, Function<T, String> email, Map<String, StudentRow> cohort) {
        Map<String, List<T>> out = new java.util.HashMap<>();
        for (T item : items) {
            String e = email.apply(item);
            if (e == null) continue;
            String key = e.trim().toLowerCase(Locale.ROOT);
            if (cohort.containsKey(key)) out.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        return out;
    }

    private static boolean earlierCoach(ReplyCoachAttempt a, ReplyCoachAttempt b) {
        if (a.getAttemptNumber() != b.getAttemptNumber()) return a.getAttemptNumber() < b.getAttemptNumber();
        return before(a.getStartedAt(), b.getStartedAt());
    }

    static boolean isGood(String evaluation) {
        String e = upper(evaluation);
        return e.equals("BEST") || e.equals("ACCEPTABLE");
    }

    /** Score keeps playedAt on newer records; older ones only have the ISO date string. */
    static Instant scoreTime(Score s) {
        if (s.getPlayedAt() != null) return s.getPlayedAt();
        String d = s.getDate();
        if (d == null || d.isBlank()) return null;
        try { return Instant.parse(d); } catch (Exception ignored) { }
        try { return OffsetDateTime.parse(d).toInstant(); } catch (Exception ignored) { }
        try { return LocalDate.parse(d.length() >= 10 ? d.substring(0, 10) : d).atStartOfDay(ZONE).toInstant(); } catch (Exception ignored) { }
        return null;
    }

    private static boolean inPeriod(Instant at, Instant from, Instant to) {
        if (at == null) return from == null && to == null;
        return (from == null || !at.isBefore(from)) && (to == null || at.isBefore(to));
    }

    private static boolean within(Instant at, Instant start, Instant end) {
        return at != null && !at.isBefore(start) && !at.isAfter(end);
    }

    private static Instant earliest(Instant a, Instant b) {
        return a == null || b.isBefore(a) ? b : a;
    }

    private static void addDay(Set<String> dates, Instant at) {
        if (at != null) dates.add(at.atZone(ZONE).toLocalDate().toString());
    }

    /** a strictly earlier than b; a record without a time never replaces one that has it. */
    private static boolean before(Instant a, Instant b) {
        return a != null && (b == null || a.isBefore(b));
    }

    private static String upper(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    static Double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static String name(User u) {
        String n = ((u.getFname() == null ? "" : u.getFname()) + " " + (u.getLname() == null ? "" : u.getLname())).trim();
        return n.isEmpty() ? "Student" : n;
    }

    /** Stable pseudonymous code for anonymized exports (not reversible to the email). */
    public static String code(User u) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(("japlearn-smart:" + u.getId()).getBytes(StandardCharsets.UTF_8));
            return "S-" + HexFormat.of().formatHex(hash, 0, 4).toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "S-UNKNOWN";
        }
    }
}
