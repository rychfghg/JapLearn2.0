package japlearn.demo.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import japlearn.demo.Entity.ReplyCoachAttempt;
import japlearn.demo.Entity.SituationalAttempt;
import japlearn.demo.Entity.SmartSurveyUpload;
import japlearn.demo.Entity.Student;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.ClassesRepository;
import japlearn.demo.Repository.QuackTalkSessionRepository;
import japlearn.demo.Repository.ReplyCoachAttemptRepository;
import japlearn.demo.Repository.ResponseRushProgressRepository;
import japlearn.demo.Repository.ScoreRepository;
import japlearn.demo.Repository.SituationalAttemptRepository;
import japlearn.demo.Repository.SmartCommunicationAssignmentRepository;
import japlearn.demo.Repository.SmartSurveyUploadRepository;
import japlearn.demo.Repository.StudentRepository;
import japlearn.demo.Repository.UserRepository;

@SuppressWarnings("unchecked")
class SmartGoalsServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final SituationalAttemptRepository situational = mock(SituationalAttemptRepository.class);
    private final ReplyCoachAttemptRepository coach = mock(ReplyCoachAttemptRepository.class);
    private final SmartCommunicationAssignmentRepository assignmentRepo = mock(SmartCommunicationAssignmentRepository.class);
    private final ScoreRepository scoreRepo = mock(ScoreRepository.class);
    private final QuackTalkSessionRepository talkRepo = mock(QuackTalkSessionRepository.class);
    private final japlearn.demo.Repository.ReplyCoachChapterRepository chapterRepo = mock(japlearn.demo.Repository.ReplyCoachChapterRepository.class);
    private final SmartGoalsService service = new SmartGoalsService(users, students, mock(ClassesRepository.class),
            situational, coach, scoreRepo, mock(ResponseRushProgressRepository.class),
            talkRepo, assignmentRepo,
            chapterRepo, mock(japlearn.demo.Repository.GuidedPhraseDailyPracticeRepository.class));

    private static User student(String id, String email) {
        User u = new User();
        u.setId(id); u.setEmail(email); u.setRole("student"); u.setFname("A"); u.setLname(id); u.setApproved(true);
        return u;
    }

    private static SituationalAttempt run(String email, String type, int correct, int total, boolean completed, String at) {
        SituationalAttempt a = new SituationalAttempt();
        a.setEmail(email); a.setGameType(type); a.setCorrectAnswers(correct); a.setTotalQuestions(total);
        a.setCompleted(completed); a.setLevel(1); a.setCompletedAt(Instant.parse(at));
        return a;
    }

    private Map<String, Object> goal(Map<String, Object> report, String key) {
        return ((List<Map<String, Object>>) report.get("goals")).stream().filter(g -> key.equals(g.get("key"))).findFirst().orElseThrow();
    }

    @Test
    void usesFirstRunOnlyAndKeepsNonParticipantsOutOfTheDenominator() {
        when(users.findByRoleIgnoreCase("student")).thenReturn(List.of(student("1", "ana@gmail.com"), student("2", "ben@gmail.com"), student("3", "cy@gmail.com")));
        when(students.findAll()).thenReturn(List.of());
        when(situational.findAll()).thenReturn(List.of(
                run("ana@gmail.com", "RECOGNITION", 6, 10, true, "2026-09-01T00:00:00Z"),
                run("ana@gmail.com", "RECOGNITION", 10, 10, true, "2026-09-02T00:00:00Z"), // replay: ignored
                run("ben@gmail.com", "RECOGNITION", 9, 10, true, "2026-09-03T00:00:00Z"),
                run("cy@gmail.com", "POLITENESS", 2, 10, false, "2026-09-03T00:00:00Z")));
        when(coach.findAll()).thenReturn(List.of());

        Map<String, Object> report = service.report(new SmartGoalsService.Filters(null, null, null, null, null, false));
        Map<String, Object> s1 = goal(report, "SMART1");
        assertThat(s1.get("correct")).isEqualTo(15);
        assertThat(s1.get("total")).isEqualTo(20);
        assertThat(s1.get("percentage")).isEqualTo(75.0);
        assertThat(s1.get("status")).isEqualTo("NOT_ACHIEVED");
        assertThat(s1.get("noParticipation")).isEqualTo(1);

        Map<String, Object> s3 = goal(report, "SMART3");
        assertThat(s3.get("status")).isEqualTo("INSUFFICIENT_DATA");
        assertThat(s3.get("percentage")).isNull();
        assertThat(s3.get("incomplete")).isEqualTo(1);
        assertThat(goal(report, "SMART4").get("status")).isEqualTo("INSUFFICIENT_DATA");
    }

    @Test
    void replyCoachCountsFirstAnswerPerNodeInFirstAttempt() {
        when(users.findByRoleIgnoreCase("student")).thenReturn(List.of(student("1", "ana@gmail.com")));
        when(students.findAll()).thenReturn(List.of(new Student()));
        when(situational.findAll()).thenReturn(List.of());
        ReplyCoachAttempt first = new ReplyCoachAttempt();
        first.setEmail("ana@gmail.com"); first.setChapterId("ch1"); first.setAttemptNumber(1); first.setStartedAt(Instant.parse("2026-09-01T00:00:00Z"));
        first.setAnswers(new ArrayList<>(List.of(answer("n1", "BEST"), answer("n1", "RUDE"), answer("n2", "AWKWARD"), answer("n3", "ACCEPTABLE"))));
        ReplyCoachAttempt second = new ReplyCoachAttempt();
        second.setEmail("ana@gmail.com"); second.setChapterId("ch1"); second.setAttemptNumber(2); second.setStartedAt(Instant.parse("2026-09-05T00:00:00Z"));
        second.setAnswers(new ArrayList<>(List.of(answer("n1", "BEST"), answer("n2", "BEST"), answer("n3", "BEST"))));
        when(coach.findAll()).thenReturn(List.of(second, first));

        Map<String, Object> s2 = goal(service.report(new SmartGoalsService.Filters(null, null, null, null, "QUACKRESPONSE", false)), "SMART2");
        assertThat(s2.get("correct")).isEqualTo(2);
        assertThat(s2.get("total")).isEqualTo(3);
    }

    private static ReplyCoachAttempt.AnswerRecord answer(String node, String evaluation) {
        ReplyCoachAttempt.AnswerRecord a = new ReplyCoachAttempt.AnswerRecord();
        a.setNodeId(node); a.setEvaluation(evaluation);
        return a;
    }

    @Test
    void surveyCountsAgreeAmongValidAnswersAndFlagsDuplicates() {
        SmartSurveyUploadRepository repo = mock(SmartSurveyUploadRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        UserRepository userRepo = mock(UserRepository.class);
        when(userRepo.findByRoleIgnoreCase("student")).thenReturn(List.of(student("1", "ana@gmail.com")));
        SmartSurveyService surveys = new SmartSurveyService(repo, userRepo);

        StringBuilder csv = new StringBuilder("Timestamp,Email Address");
        for (int q = 1; q <= 10; q++) csv.append(",\"Q").append(q).append(", statement\"");
        csv.append("\n1,ana@gmail.com").append(",Agree".repeat(9)).append(",Disagree");
        csv.append("\n2,ANA@gmail.com").append(",Strongly Disagree".repeat(10));   // duplicate: excluded
        csv.append("\n3,someone@else.com").append(",Strongly Agree".repeat(9)).append(",");  // unmatched, blank Q10
        List<Integer> cols = List.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        SmartSurveyUpload u = surveys.upload(new SmartSurveyService.UploadRequest("form.csv", csv.toString(), cols, 1, false), "admin");

        assertThat(u.getDuplicateRows()).isEqualTo(1);
        assertThat(u.getMatchedRows()).isEqualTo(1);
        assertThat(u.getUnmatchedRows()).isEqualTo(1);
        Map<String, Object> summary = surveys.summary(u);
        Map<String, Object> q1 = ((List<Map<String, Object>>) summary.get("questions")).get(0);
        Map<String, Object> q10 = ((List<Map<String, Object>>) summary.get("questions")).get(9);
        assertThat(q1.get("percentage")).isEqualTo(100.0);
        assertThat(q10.get("valid")).isEqualTo(1);
        assertThat(q10.get("percentage")).isEqualTo(0.0);
        assertThat(q1.get("statement")).isEqualTo("Q1, statement");

        // Per-student comparison: matched by account, actual vs survey kept separate, missing = insufficient.
        when(repo.findById("s1")).thenReturn(java.util.Optional.of(u));
        u.setId("s1");
        Map<String, Object> perf = new java.util.LinkedHashMap<>(Map.of("studentId", "1", "code", "S-1", "name", "A 1"));
        perf.put("SMART1", Map.of("correct", 9, "total", 10, "percentage", 90.0, "status", "MEASURED"));
        perf.put("SMART2", Map.of("correct", 0, "total", 0, "status", "NO_PARTICIPATION"));
        perf.put("SMART3", Map.of("correct", 3, "total", 5, "percentage", 60.0, "status", "MEASURED"));
        perf.put("SMART4", Map.of("correct", 0, "total", 0, "status", "NOT_ASSIGNED"));
        Map<String, Object> cmp = surveys.comparison("s1", Map.of("students", List.of(perf)));
        Map<String, Object> ana = ((List<Map<String, Object>>) cmp.get("students")).get(0);
        Map<String, Object> s1 = (Map<String, Object>) ana.get("SMART1");
        assertThat(s1.get("surveyPercentage")).isEqualTo(100.0);
        assertThat(s1.get("difference")).isEqualTo(-10.0);
        assertThat(s1.get("meetsTarget")).isEqualTo(true);
        assertThat(((Map<String, Object>) ana.get("SMART2")).get("difference")).isNull();
        assertThat(((Map<String, Object>) ana.get("SMART3")).get("meetsTarget")).isEqualTo(false);
        assertThat(((Map<String, Object>) ana.get("SMART4")).get("surveyPercentage")).isEqualTo(50.0);
        assertThat(((Map<String, Object>) cmp.get("flags")).get("unmatchedRows")).isEqualTo(1);

        assertThatThrownBy(() -> surveys.upload(new SmartSurveyService.UploadRequest("form.csv", csv.toString(),
                List.of(0, 1, 4, 5, 6, 7, 8, 9, 10, 11), null, false), "admin")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void countsRelaySumiTurnsAndTaggedPolitenessQuestions() {
        when(users.findByRoleIgnoreCase("student")).thenReturn(List.of(student("1", "ana@gmail.com")));
        when(students.findAll()).thenReturn(List.of());
        when(situational.findAll()).thenReturn(List.of());
        japlearn.demo.Entity.Score relay = new japlearn.demo.Entity.Score();
        relay.setEmail("ana@gmail.com"); relay.setGame("QUACKRESPONSE_RELAY"); relay.setCorrectAnswers(24); relay.setTotalQuestions(30);
        relay.setCompleted(true); relay.setDate("2026-09-01T00:00:00Z");
        when(scoreRepo.findAll()).thenReturn(List.of(relay));
        japlearn.demo.Entity.QuackTalkSession old = new japlearn.demo.Entity.QuackTalkSession();
        old.setEmail("ana@gmail.com"); old.setRoomType("TALK_WITH_SUMI"); old.setCompleted(true); old.setContextualAccuracy(90);
        old.setPracticedAt(Instant.parse("2026-08-01T00:00:00Z")); // no per-turn scores: not measurable
        japlearn.demo.Entity.QuackTalkSession sumi = new japlearn.demo.Entity.QuackTalkSession();
        sumi.setEmail("ana@gmail.com"); sumi.setRoomType("TALK_WITH_SUMI"); sumi.setCompleted(true); sumi.setPracticedAt(Instant.parse("2026-09-02T00:00:00Z"));
        sumi.setPronunciationScore(10);
        sumi.setContextEvaluations(List.of(Map.of("contextScore", 80, "japaneseAttempted", true),
                Map.of("contextScore", 95, "japaneseAttempted", false), Map.of("contextScore", 40, "japaneseAttempted", true)));
        when(talkRepo.findAll()).thenReturn(List.of(old, sumi));
        japlearn.demo.Entity.ReplyCoachChapter chapter = new japlearn.demo.Entity.ReplyCoachChapter();
        chapter.setId("ch1");
        japlearn.demo.Entity.ReplyCoachChapter.StoryNode tagged = new japlearn.demo.Entity.ReplyCoachChapter.StoryNode();
        tagged.setId("n2"); tagged.setAssessesPoliteness(true);
        chapter.setNodes(new ArrayList<>(List.of(tagged)));
        when(chapterRepo.findAll()).thenReturn(List.of(chapter));
        ReplyCoachAttempt attempt = new ReplyCoachAttempt();
        attempt.setEmail("ana@gmail.com"); attempt.setChapterId("ch1"); attempt.setAttemptNumber(1); attempt.setStartedAt(Instant.parse("2026-09-03T00:00:00Z"));
        attempt.setAnswers(new ArrayList<>(List.of(answer("n1", "BEST"), answer("n2", "IMPOLITE"))));
        when(coach.findAll()).thenReturn(List.of(attempt));

        Map<String, Object> report = service.report(new SmartGoalsService.Filters(null, null, null, null, null, false));
        Map<String, Object> s2 = goal(report, "SMART2");
        assertThat(s2.get("correct")).isEqualTo(24 + 1 + 1); // relay + one passing Sumi turn + one good Reply Coach answer
        assertThat(s2.get("total")).isEqualTo(30 + 3 + 2);
        Map<String, Object> s3 = goal(report, "SMART3");
        assertThat(s3.get("correct")).isEqualTo(0);
        assertThat(s3.get("total")).isEqualTo(1);
        assertThat(s3.get("status")).isEqualTo("NOT_ACHIEVED");
    }


    @Test
    void completionNeedsEveryRequiredUnitFinishedAndIgnoresStartedRuns() {
        when(users.findByRoleIgnoreCase("student")).thenReturn(List.of(student("1", "ana@gmail.com"), student("2", "ben@gmail.com"), student("3", "cy@gmail.com")));
        List<Student> inClass = new ArrayList<>();
        for (String e : List.of("ana@gmail.com", "ben@gmail.com")) { Student st = new Student(); st.setEmail(e); st.setClassCode("C1"); inClass.add(st); }
        when(students.findAll()).thenReturn(inClass); // cy has no class, so is not assigned
        SituationalAttempt l2 = run("ana@gmail.com", "POLITENESS", 5, 10, true, "2026-09-02T00:00:00Z"); l2.setLevel(2);
        when(situational.findAll()).thenReturn(List.of(
                run("ana@gmail.com", "POLITENESS", 8, 10, true, "2026-09-01T00:00:00Z"), l2,
                run("ben@gmail.com", "POLITENESS", 8, 10, true, "2026-09-01T00:00:00Z"),
                run("ben@gmail.com", "POLITENESS", 8, 10, true, "2026-09-01T05:00:00Z"), // same level again: one unit
                run("cy@gmail.com", "POLITENESS", 9, 10, true, "2026-09-01T00:00:00Z")));
        when(coach.findAll()).thenReturn(List.of());
        japlearn.demo.Entity.SmartCommunicationAssignment a = new japlearn.demo.Entity.SmartCommunicationAssignment();
        a.setId("a1"); a.setClassCode("C1"); a.setActivityKey("POLITENESS"); a.setRequiredCount(2);
        a.setStartsAt(Instant.parse("2026-08-31T00:00:00Z")); a.setDueAt(Instant.parse("2026-09-10T00:00:00Z"));
        when(assignmentRepo.findAllByOrderByDueAtDesc()).thenReturn(List.of(a));

        Map<String, Object> report = service.report(new SmartGoalsService.Filters(null, null, null, null, null, false));
        Map<String, Object> s4 = goal(report, "SMART4");
        assertThat(s4.get("correct")).isEqualTo(1);
        assertThat(s4.get("total")).isEqualTo(2);
        assertThat(s4.get("percentage")).isEqualTo(50.0);
        assertThat(s4.get("status")).isEqualTo("NOT_ACHIEVED");
    }

}
