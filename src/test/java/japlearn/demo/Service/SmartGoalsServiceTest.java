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
    private final SmartGoalsService service = new SmartGoalsService(users, students, mock(ClassesRepository.class),
            situational, coach, mock(ScoreRepository.class), mock(ResponseRushProgressRepository.class),
            mock(QuackTalkSessionRepository.class), mock(SmartCommunicationAssignmentRepository.class));

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

        assertThatThrownBy(() -> surveys.upload(new SmartSurveyService.UploadRequest("form.csv", csv.toString(),
                List.of(0, 1, 4, 5, 6, 7, 8, 9, 10, 11), null, false), "admin")).isInstanceOf(ResponseStatusException.class);
    }
}
