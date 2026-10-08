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
import japlearn.demo.Entity.SmartCommunicationAssignment;
import japlearn.demo.Entity.Student;
import japlearn.demo.Entity.User;
import japlearn.demo.Entity.GuidedPhraseDailyPractice;
import japlearn.demo.Entity.ReplyCoachChapter;
import japlearn.demo.Repository.ClassesRepository;
import japlearn.demo.Repository.GuidedPhraseDailyPracticeRepository;
import japlearn.demo.Repository.ReplyCoachChapterRepository;
import japlearn.demo.Repository.QuackTalkSessionRepository;
import japlearn.demo.Repository.ReplyCoachAttemptRepository;
import japlearn.demo.Repository.ResponseRushProgressRepository;
import japlearn.demo.Repository.ScoreRepository;
import japlearn.demo.Repository.SituationalAttemptRepository;
import japlearn.demo.Repository.SmartCommunicationAssignmentRepository;
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
    /** Activities a class can be assigned for objective 4. QUACKTALK = any QuackTalk session (older assignments). */
    public static final Set<String> ASSIGNABLE = Set.of(
            "RECOGNITION", "EXPRESSION_MATCH", "POLITENESS", "REPLY_COACH", "RESPONSE_RUSH", "DIALOGUE_RELAY",
            "TALK_WITH_SUMI", "GUIDED_PHRASE", "QUACKTALK");
    private static final Map<String, Set<String>> MODULES = Map.of(
            "QUACKSITUATE", Set.of("RECOGNITION", "EXPRESSION_MATCH", "POLITENESS"),
            "QUACKRESPONSE", Set.of("REPLY_COACH", "RESPONSE_RUSH", "DIALOGUE_RELAY"),
            "QUACKTALK", Set.of("TALK_WITH_SUMI", "GUIDED_PHRASE", "QUACKTALK"));

    public static final List<String> RULES = List.of(
            "Accuracy = correct scored answers / total scored answers x 100, summed across eligible students (not an average of student percentages).",
            "Only the first evaluation attempt counts: the earliest completed run of each QuackSituate level/set, the earliest Response Rush and Dialogue Relay runs, the first Reply Coach attempt of each chapter, and the first Talk with Sumi session with per-turn scores, inside the selected period.",
            "QuackSituate games (Ahiru Rescue, Expression Match, Politeness) give one answer per question, so a run's correct/total already reflects the first scored answer of each item. In Reply Coach the first answer recorded for each dialogue node is used.",
            "Reply Coach and Response Rush answers rated BEST or ACCEPTABLE are correct; AWKWARD, IMPOLITE, RUDE and TIMEOUT are scored incorrect.",
            "Dialogue Relay: 25 dialogue choices plus the spoken bonus prompts, where a bonus counts as correct only when the reply is judged appropriate to the context (not by pronunciation).",
            "Talk with Sumi: each learner turn is one scored response; it is correct when Japanese was attempted and the context score is at least " + SUMI_CONTEXT_PASS + "/100. Pronunciation, accuracy, fluency and completeness scores are never used. Sessions saved before per-turn context scores were recorded are not measurable.",
            "SMART 3 uses QuackSituate Politeness plus Reply Coach questions an admin has explicitly tagged as politeness. Expression Match and Response Rush do not store per-question answers, so their items cannot be tagged and are not counted.",
            "Students without records are 'No participation' and students with only unfinished runs are 'Incomplete'; neither is counted as zero.",
            "Completion = completed assigned activities / assigned activities. Only assignments created on this dashboard count, each once per student, and only after their due date. Voluntary play is never counted as an assignment.",
            "An assignment is completed only from finished game records inside its window (QuackSituate runs marked completed, Reply Coach chapters marked COMPLETED, finished Response Rush and Dialogue Relay runs, Talk with Sumi sessions with at least one learner turn, completed Guided Phrase sessions). Started or in-progress records never count. When an assignment requires several levels, chapters, runs or sessions, each distinct one counts once and all must be finished.",
            "A goal shows 'Insufficient Data' when its denominator is 0.");

    private final UserRepository users;
    private final StudentRepository students;
    private final ClassesRepository classes;
    private final SituationalAttemptRepository situational;
    private final ReplyCoachAttemptRepository replyCoach;
    private final ScoreRepository scores;
    private final ResponseRushProgressRepository rushProgress;
    private final QuackTalkSessionRepository quackTalk;
    private final SmartCommunicationAssignmentRepository assignments;
    private final ReplyCoachChapterRepository chapters;
    private final GuidedPhraseDailyPracticeRepository guidedPractice;

    public SmartGoalsService(UserRepository users, StudentRepository students, ClassesRepository classes,
            SituationalAttemptRepository situational, ReplyCoachAttemptRepository replyCoach, ScoreRepository scores,
            ResponseRushProgressRepository rushProgress, QuackTalkSessionRepository quackTalk,
            SmartCommunicationAssignmentRepository assignments, ReplyCoachChapterRepository chapters,
            GuidedPhraseDailyPracticeRepository guidedPractice) {
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
        this.assignments = assignments;
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
        boolean incomplete;
        Instant last;
        final Set<String> dates = new TreeSet<>();

        void add(int right, int scored, Instant at) {
            correct += right;
            total += scored;
            touch(at);
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
            return m;
        }
    }

    private static final class StudentRow {
        final User user;
        final String classCode;
        final String email;
        final Map<String, Tally> goals = new LinkedHashMap<>();
        int runsAttempted;
        int runsCompleted;
        int assigned;
        int assignedCompleted;
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
        for (StudentRow row : cohort.values()) {
            List<SituationalAttempt> mine = sitByStudent.getOrDefault(row.email, List.of());
            Map<String, SituationalAttempt> firstRun = new LinkedHashMap<>();
            for (SituationalAttempt a : mine) {
                String type = upper(a.getGameType());
                if (!SOURCE_GOAL.containsKey(type) || !sources.contains(type) || !inPeriod(a.getCompletedAt(), from, to)) continue;
                row.runsAttempted++;
                addDay(row.dates, a.getCompletedAt());
                Tally tally = row.goals.get(SOURCE_GOAL.get(type));
                if (!a.isCompleted()) { tally.incomplete = true; continue; }
                row.runsCompleted++;
                if (a.getTotalQuestions() <= 0 || a.getCorrectAnswers() < 0 || a.getCorrectAnswers() > a.getTotalQuestions()) {
                    excluded.merge("situational: invalid correct/total", 1, Integer::sum);
                    continue;
                }
                String key = type + "|" + a.getLevel() + "|" + a.getSetNumber();
                SituationalAttempt seen = firstRun.get(key);
                if (seen == null || before(a.getCompletedAt(), seen.getCompletedAt())) {
                    if (seen != null) excluded.merge("situational: repeat runs after the first", 1, Integer::sum);
                    firstRun.put(key, a);
                } else {
                    excluded.merge("situational: repeat runs after the first", 1, Integer::sum);
                }
            }
            for (SituationalAttempt a : firstRun.values())
                row.goals.get(SOURCE_GOAL.get(upper(a.getGameType()))).add(a.getCorrectAnswers(), a.getTotalQuestions(), a.getCompletedAt());
        }

        // ---- SMART 2: Reply Coach (item level)
        List<ReplyCoachAttempt> coach = replyCoach.findAll();
        read.put("reply_coach_attempts", coach.size());
        if (sources.contains("REPLY_COACH")) {
            // Reply Coach questions an admin explicitly tagged as assessing politeness also count for SMART 3.
            Set<String> politenessNodes = new java.util.HashSet<>();
            for (ReplyCoachChapter chapter : chapters.findAll())
                for (ReplyCoachChapter.StoryNode node : chapter.getNodes() == null ? List.<ReplyCoachChapter.StoryNode>of() : chapter.getNodes())
                    if (node.isAssessesPoliteness() && node.getId() != null) politenessNodes.add(chapter.getId() + "|" + node.getId());
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
                        if (good) right++;
                        if (politenessNodes.contains(a.getChapterId() + "|" + ans.getNodeId())) {
                            politeScored++;
                            if (good) politeRight++;
                        }
                        if (ans.getAnsweredAt() != null && (last == null || ans.getAnsweredAt().isAfter(last))) last = ans.getAnsweredAt();
                    }
                    if (scored == 0) { tally.incomplete = true; continue; }
                    Instant at = last == null ? a.getStartedAt() : last;
                    tally.add(right, scored, at);
                    if (politeScored > 0) polite.add(politeRight, politeScored, at);
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
                        if (first != null) excluded.merge(game[2] + ": repeat runs after the first", 1, Integer::sum);
                        first = s;
                    } else {
                        excluded.merge(game[2] + ": repeat runs after the first", 1, Integer::sum);
                    }
                }
                Tally tally = row.goals.get("SMART2");
                if (first != null) tally.add(first.getCorrectAnswers(), first.getTotalQuestions(), scoreTime(first));
                else if (game[0].equals("RESPONSE_RUSH")) {
                    ResponseRushProgress open = openRush.get(row.email);
                    if (open != null && !open.isCompleted() && open.getAnswers() != null && !open.getAnswers().isEmpty()
                            && inPeriod(open.getUpdatedAt(), from, to)) tally.incomplete = true;
                }
            }
        }

        // ---- SMART 2: Talk with Sumi (per learner turn, context score only)
        boolean needTalk = sources.contains("TALK_WITH_SUMI") || sources.contains("QUACKTALK");
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
                        if (first != null) excluded.merge("talk with sumi: repeat sessions after the first", 1, Integer::sum);
                        first = t;
                    } else excluded.merge("talk with sumi: repeat sessions after the first", 1, Integer::sum);
                }
                if (first == null) continue;
                int right = 0, scored = 0;
                for (Map<String, Object> turn : first.getContextEvaluations()) {
                    Object score = turn.get("contextScore");
                    if (!(score instanceof Number n)) continue;
                    scored++;
                    if (Boolean.TRUE.equals(turn.get("japaneseAttempted")) && n.intValue() >= SUMI_CONTEXT_PASS) right++;
                }
                if (scored > 0) row.goals.get("SMART2").add(right, scored, first.getPracticedAt());
            }
        }

        // ---- SMART 4: assigned communication activities
        List<GuidedPhraseDailyPractice> guided = sources.contains("GUIDED_PHRASE") ? guidedPractice.findAll() : List.of();
        Map<String, List<GuidedPhraseDailyPractice>> guidedByStudent = groupByEmail(guided, GuidedPhraseDailyPractice::getEmail, cohort);
        Map<String, List<SituationalAttempt>> sitAll = sitByStudent;
        Map<String, List<ReplyCoachAttempt>> coachAll = groupByEmail(coach, ReplyCoachAttempt::getEmail, cohort);
        Instant now = Instant.now();
        Set<String> cohortClasses = cohort.values().stream().map(r -> r.classCode).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Map<String, Object>> counted = new ArrayList<>();
        List<Map<String, Object>> open = new ArrayList<>();
        int assignedTotal = 0, completedTotal = 0;
        Instant lastCompletion = null;
        Set<String> assignedStudents = new java.util.HashSet<>();
        for (SmartCommunicationAssignment a : assignments.findAllByOrderByDueAtDesc()) {
            if (a.getClassCode() == null || !cohortClasses.contains(a.getClassCode()) || !sources.contains(upper(a.getActivityKey()))) continue;
            if (a.getDueAt() == null || a.getStartsAt() == null) continue;
            if ((from != null && a.getDueAt().isBefore(from)) || (to != null && !a.getDueAt().isBefore(to))) continue;
            int assignedHere = 0, doneHere = 0;
            for (StudentRow row : cohort.values()) {
                if (!a.getClassCode().equals(row.classCode)) continue;
                assignedHere++;
                Instant doneAt = completedAt(a, row.email, sitAll, coachAll, scoresByStudent, talksByStudent, guidedByStudent);
                boolean closed = !a.getDueAt().isAfter(now);
                if (closed) {
                    row.assigned++;
                    assignedStudents.add(row.email);
                    if (doneAt != null) {
                        row.assignedCompleted++;
                        if (lastCompletion == null || doneAt.isAfter(lastCompletion)) lastCompletion = doneAt;
                    }
                }
                if (doneAt != null) doneHere++;
            }
            Map<String, Object> view = assignmentView(a, classTitles);
            view.put("assignedStudents", assignedHere);
            view.put("completedStudents", doneHere);
            if (a.getDueAt().isAfter(now)) open.add(view);
            else {
                counted.add(view);
                assignedTotal += assignedHere;
                completedTotal += doneHere;
            }
        }

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
        g4.put("total", assignedTotal);
        g4.put("percentage", assignedTotal > 0 ? round(completedTotal * 100.0 / assignedTotal) : null);
        g4.put("status", assignedTotal == 0 ? "INSUFFICIENT_DATA" : completedTotal * 100.0 / assignedTotal >= 80 ? "ACHIEVED" : "NOT_ACHIEVED");
        g4.put("eligibleStudents", eligible);
        g4.put("participatingStudents", assignedStudents.size());
        g4.put("noParticipation", null);
        g4.put("incomplete", null);
        g4.put("lastUpdated", lastCompletion);
        g4.put("evidence", "Assignments created on this dashboard; completion read from existing game records inside each assignment window");
        g4.put("limitations", List.of(
                "The system had no class assignment records for communication games before this dashboard. Historical completion is Not Measurable; only assignments created here (new records, dated) are counted.",
                "Students are assigned by their current class. A student who changed class is counted in the new class.",
                "Open assignments (due date not reached) are listed but not counted yet."));
        goals.add(g4);

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
            s4.put("correct", row.assignedCompleted);
            s4.put("total", row.assigned);
            s4.put("percentage", row.assigned > 0 ? round(row.assignedCompleted * 100.0 / row.assigned) : null);
            s4.put("status", row.assigned > 0 ? "MEASURED" : "NOT_ASSIGNED");
            r.put("SMART4", s4);
            r.put("runsAttempted", row.runsAttempted);
            r.put("runsCompleted", row.runsCompleted);
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
        out.put("assignments", Map.of("counted", counted, "open", open));
        out.put("audit", Map.of("recordsRead", read, "recordsExcluded", excluded));
        return out;
    }

    private Map<String, Object> goal(String key, String name, int target, Map<String, StudentRow> cohort, String goalKey,
            String evidence, List<String> limitations) {
        int correct = 0, total = 0, participating = 0, incomplete = 0, none = 0;
        Instant last = null;
        for (StudentRow row : cohort.values()) {
            Tally t = row.goals.get(goalKey);
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
     * When (if at all) the student met this assignment's completion requirement inside its window.
     * Only finished records count; started or in-progress records never do. Each distinct unit (a level/set,
     * a chapter, a run, a session, a practice session) counts once, and the assignment is complete when the
     * student has finished the required number of units. Returns the time the last required unit was finished.
     */
    private Instant completedAt(SmartCommunicationAssignment a, String email,
            Map<String, List<SituationalAttempt>> sit, Map<String, List<ReplyCoachAttempt>> coach,
            Map<String, List<Score>> scoreMap, Map<String, List<QuackTalkSession>> talks,
            Map<String, List<GuidedPhraseDailyPractice>> guided) {
        Instant start = a.getStartsAt(), due = a.getDueAt();
        String key = upper(a.getActivityKey());
        Map<String, Instant> units = new java.util.HashMap<>();
        java.util.function.BiConsumer<String, Instant> unit = (id, at) -> units.merge(id, at, (x, y) -> y.isBefore(x) ? y : x);
        switch (key) {
            case "RECOGNITION", "EXPRESSION_MATCH", "POLITENESS" -> {
                for (SituationalAttempt s : sit.getOrDefault(email, List.of()))
                    if (s.isCompleted() && key.equals(upper(s.getGameType()))
                            && (a.getLevel() == null || a.getLevel() == s.getLevel())
                            && within(s.getCompletedAt(), start, due)) unit.accept(s.getLevel() + "|" + s.getSetNumber(), s.getCompletedAt());
            }
            case "REPLY_COACH" -> {
                for (ReplyCoachAttempt r : coach.getOrDefault(email, List.of()))
                    if ("COMPLETED".equalsIgnoreCase(r.getStatus()) && r.getChapterId() != null
                            && (a.getChapterId() == null || a.getChapterId().equals(r.getChapterId()))
                            && within(r.getCompletedAt(), start, due)) unit.accept(r.getChapterId(), r.getCompletedAt());
            }
            case "RESPONSE_RUSH", "DIALOGUE_RELAY" -> {
                String game = key.equals("RESPONSE_RUSH") ? "QUACKRESPONSE_RUSH" : "QUACKRESPONSE_RELAY";
                for (Score s : scoreMap.getOrDefault(email, List.of())) {
                    Instant at = scoreTime(s);
                    if (s.isCompleted() && game.equalsIgnoreCase(s.getGame()) && within(at, start, due))
                        unit.accept(s.getId() != null ? s.getId() : String.valueOf(at), at);
                }
            }
            case "QUACKTALK", "TALK_WITH_SUMI" -> {
                // A session with no learner turns was opened but not actually done.
                for (QuackTalkSession t : talks.getOrDefault(email, List.of()))
                    if (t.isCompleted() && t.getConversationTurns() > 0
                            && (key.equals("QUACKTALK") || "TALK_WITH_SUMI".equalsIgnoreCase(t.getRoomType()))
                            && within(t.getPracticedAt(), start, due)) unit.accept(t.getId() != null ? t.getId() : String.valueOf(t.getPracticedAt()), t.getPracticedAt());
            }
            case "GUIDED_PHRASE" -> {
                // One record per learner per day: each completed (not merely started) session is a unit.
                LocalDate first = start.atZone(ZONE).toLocalDate(), last = due.atZone(ZONE).toLocalDate();
                for (GuidedPhraseDailyPractice g : guided.getOrDefault(email, List.of())) {
                    if (g.getSessionsCompleted() <= 0 || g.getPracticeDate() == null) continue;
                    LocalDate day;
                    try { day = LocalDate.parse(g.getPracticeDate()); } catch (Exception e) { continue; }
                    if (day.isBefore(first) || day.isAfter(last)) continue;
                    Instant at = g.getUpdatedAt() != null ? g.getUpdatedAt() : day.atStartOfDay(ZONE).toInstant();
                    for (int i = 0; i < g.getSessionsCompleted(); i++) unit.accept(day + "#" + i, at);
                }
            }
            default -> { }
        }
        int required = Math.max(1, a.getRequiredCount() == null ? 1 : a.getRequiredCount());
        if (units.size() < required) return null;
        return units.values().stream().sorted().skip(required - 1).findFirst().orElse(null);
    }

    public static Map<String, Object> assignmentView(SmartCommunicationAssignment a, Map<String, String> classTitles) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("classCode", a.getClassCode());
        m.put("classTitle", classTitles == null ? null : classTitles.get(a.getClassCode()));
        m.put("activityKey", a.getActivityKey());
        m.put("level", a.getLevel());
        m.put("chapterId", a.getChapterId());
        m.put("requiredCount", a.getRequiredCount() == null ? 1 : a.getRequiredCount());
        m.put("startsAt", a.getStartsAt());
        m.put("dueAt", a.getDueAt());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    private static Set<String> sourcesFor(String module) {
        String m = module == null ? "" : module.trim().toUpperCase(Locale.ROOT);
        if (m.isEmpty() || m.equals("ALL")) return ASSIGNABLE;
        if (MODULES.containsKey(m)) return MODULES.get(m);
        if (ASSIGNABLE.contains(m)) return Set.of(m);
        return ASSIGNABLE;
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
