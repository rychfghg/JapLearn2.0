package japlearn.demo.Controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import japlearn.demo.Entity.ReplyCoachAttempt;
import japlearn.demo.Entity.ReplyCoachChapter;
import japlearn.demo.Repository.ReplyCoachAttemptRepository;
import japlearn.demo.Repository.ReplyCoachChapterRepository;

class ReplyCoachControllerTest {
    private final ReplyCoachChapterRepository chapters = mock(ReplyCoachChapterRepository.class);
    private final ReplyCoachAttemptRepository attempts = mock(ReplyCoachAttemptRepository.class);
    private final ReplyCoachController controller = new ReplyCoachController(chapters, attempts, null, null);
    private final ReplyCoachAttempt attempt = new ReplyCoachAttempt();

    ReplyCoachControllerTest() {
        attempt.setChapterId("chapter");
        ReplyCoachChapter chapter = new ReplyCoachChapter();
        ReplyCoachChapter.StoryNode node = new ReplyCoachChapter.StoryNode();
        node.setId("decision-1"); node.setType("CHOICE");
        ReplyCoachChapter.ChoiceOption choice = new ReplyCoachChapter.ChoiceOption();
        choice.setId("best"); choice.setEvaluation("BEST"); choice.setPoints(5);
        choice.setJapanese("こんにちは"); choice.setRomaji("Konnichiwa"); choice.setNextNodeId("reaction");
        node.setChoices(List.of(choice)); chapter.setNodes(List.of(node));
        when(chapters.findById("chapter")).thenReturn(Optional.of(chapter));
        when(attempts.findById("attempt")).thenReturn(Optional.of(attempt));
        when(attempts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void combinesPersistedAndSubmittedHelpWithoutChangingTheAnswerScore() {
        controller.progress("attempt", Map.of("supportNodeId", "decision-1", "helpType", "replay"));
        controller.answer("attempt", Map.of("nodeId", "decision-1", "choiceId", "best", "helpTypes", "romaji,meaning,unknown", "supportTrackingVersion", "1"));
        var answer = attempt.getAnswers().get(0);
        assertThat(answer.getSupported()).isTrue();
        assertThat(answer.getHelpTypes()).containsExactly("meaning", "replay", "romaji");
        assertThat(attempt.getScore()).isEqualTo(5);
        assertThat(attempt.getCurrentNodeId()).isEqualTo("reaction");
    }

    @Test void recordsIndependentOnlyForClientsWithCompleteSupportTracking() {
        controller.answer("attempt", Map.of("nodeId", "decision-1", "choiceId", "best", "supportTrackingVersion", "1"));
        assertThat(attempt.getAnswers().get(0).getSupported()).isFalse();
        assertThat(attempt.getAnswers().get(0).getHelpTypes()).isEmpty();
        assertThat(controller.answer("attempt", Map.of("nodeId", "decision-1", "choiceId", "best")).getStatusCode().value()).isEqualTo(400);
        assertThat(attempt.getAnswers()).hasSize(1);
    }

    @Test void legacyClientsStayUnknownRatherThanBeingRelabeledIndependent() {
        controller.answer("attempt", Map.of("nodeId", "decision-1", "choiceId", "best", "hintUsed", "false"));
        assertThat(attempt.getAnswers().get(0).getSupported()).isNull();
    }

    @Test void reviewingDialogueDoesNotMoveTheSavedStoryPosition() {
        attempt.setCurrentNodeId("decision-1");
        controller.progress("attempt", Map.of("supportNodeId", "decision-1", "helpType", "dialogueReview"));
        assertThat(attempt.getCurrentNodeId()).isEqualTo("decision-1");
        assertThat(attempt.getSupportUsage().get("decision-1")).containsExactly("dialogueReview");
    }
}
