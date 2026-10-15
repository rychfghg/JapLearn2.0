package japlearn.demo.Controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import japlearn.demo.Entity.*;
import japlearn.demo.Repository.*;
import japlearn.demo.Service.*;

class ReplyCoachReadinessControllerTest {
    private final ReplyCoachAttemptRepository attempts = mock(ReplyCoachAttemptRepository.class);
    private final ReplyCoachChapterRepository chapters = mock(ReplyCoachChapterRepository.class);
    private final StudentAuthorizationService auth = mock(StudentAuthorizationService.class);
    private final ReplyCoachQuestionBank bank = new ReplyCoachQuestionBank();
    private final ReplyCoachReadiness readiness = new ReplyCoachReadiness(bank, 80);
    private final ReplyCoachReadinessController controller = new ReplyCoachReadinessController(attempts, chapters, auth, readiness, bank);
    private final ReplyCoachAttempt attempt = new ReplyCoachAttempt();
    ReplyCoachReadinessControllerTest() {
        attempt.setEmail("student@example.com"); attempt.setChapterId("chapter");
        attempt.setChapterTitle(ReplyCoachReadiness.CHAPTER); attempt.setStatus("COMPLETED");
        when(attempts.findById("attempt")).thenReturn(Optional.of(attempt));
        when(attempts.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    private Map<String, String> request(String action) {
        return new HashMap<>(Map.of("action", action, "attemptId", "attempt", "email", "student@example.com"));
    }
    @Test void checksTheSessionAndRejectsAnotherStudentsAttempt() {
        assertThatThrownBy(() -> controller.get("attempt", "other@example.com", "token")).isInstanceOf(ResponseStatusException.class);
        verify(auth).requireOwnStudent("other@example.com", "token", true);
        verify(attempts, never()).save(any());
    }
    @Test void practiceIsSavedOnceAndDoesNotChangeTheChapterScore() {
        var answer = new ReplyCoachAttempt.AnswerRecord(); answer.setNodeId("decision-1"); attempt.getAnswers().add(answer);
        var q = bank.get("practice-1"); var request = request("practice");
        request.put("questionId", q.id()); request.put("selectedJapanese", q.choices().stream().filter(c -> c.id().equals(q.correctId())).findFirst().orElseThrow().japanese());
        request.put("helpTypes", "romaji,meaning");
        assertThat(controller.update(request, "token").get("correct")).isEqualTo(true);
        request.put("selectedJapanese", "forged change"); controller.update(request, "token");
        assertThat(attempt.getPracticeResults()).hasSize(1);
        assertThat(attempt.getPracticeResults().get(0).helpTypes).containsExactly("romaji", "meaning");
        assertThat(attempt.getScore()).isZero();
    }
    @Test void markingAnIncompleteChapterCompleteCannotBypassTheReadinessPrerequisite() {
        var chapter = new ReplyCoachChapter(); var node = new ReplyCoachChapter.StoryNode();
        node.setId("decision-1"); node.setType("CHOICE"); chapter.setNodes(List.of(node));
        when(chapters.findById("chapter")).thenReturn(Optional.of(chapter));
        assertThatThrownBy(() -> controller.update(request("start"), "token")).isInstanceOf(ResponseStatusException.class);
        assertThat(attempt.getReadinessRuns()).isEmpty();
    }
    @Test void suppliedCorrectnessCannotOverrideServerGrading() {
        var run = readiness.start(attempt); var q = bank.get(run.questionIds.get(0));
        var request = request("answer"); request.put("runId", run.id); request.put("questionId", q.id());
        request.put("choiceId", q.choices().stream().filter(c -> !c.id().equals(q.correctId())).findFirst().orElseThrow().id());
        request.put("correct", "true"); controller.update(request, "token");
        assertThat(run.answers.get(0).correct).isFalse();
        assertThat(attempt.getScore()).isZero();
    }
}
