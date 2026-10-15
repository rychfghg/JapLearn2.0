package japlearn.demo.Service;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import japlearn.demo.Entity.ReplyCoachAttempt;

class ReplyCoachReadinessTest {
    private final ReplyCoachQuestionBank bank = new ReplyCoachQuestionBank();
    private final ReplyCoachReadiness service = new ReplyCoachReadiness(bank, 80);
    private ReplyCoachAttempt attempt() {
        var attempt = new ReplyCoachAttempt();
        attempt.setChapterTitle(ReplyCoachReadiness.CHAPTER); attempt.setStatus("COMPLETED");
        return attempt;
    }
    private void finish(ReplyCoachAttempt.ReadinessRun run, int correct) {
        while (run.answers.size() < run.questionIds.size()) {
            String id = run.questionIds.get(run.answers.size()); var q = bank.get(id);
            String choice = run.answers.size() < correct ? q.correctId() : q.choices().stream().filter(c -> !c.id().equals(q.correctId())).findFirst().orElseThrow().id();
            service.answer(run, id, choice);
        }
    }
    @Test void tenSkillsAreCoveredAndEightIndependentCorrectAnswersPass() {
        var attempt = attempt(); var run = service.start(attempt);
        assertThat(run.questionIds).hasSize(10).doesNotHaveDuplicates();
        assertThat(run.questionIds.stream().map(id -> bank.get(id).skill()).distinct().count()).isEqualTo(10);
        finish(run, 8); assertThat(service.passed(run)).isTrue();
        assertThat(service.view(attempt).get("ready")).isEqualTo(true);
    }
    @Test void sevenCorrectDoesNotPassAndSupportedAnswersCountAsNotCorrect() {
        var run = service.start(attempt()); finish(run, 7); assertThat(service.passed(run)).isFalse();
        // 10 correct, one with support: 9 independent correct (90%) passes.
        var oneSupported = service.start(attempt()); service.support(oneSupported, oneSupported.questionIds.get(0), "meaning");
        finish(oneSupported, 10); assertThat(service.passed(oneSupported)).isTrue();
        // 8 correct, one of them supported: only 7 independent correct (70%) does not pass.
        var notEnough = service.start(attempt()); service.support(notEnough, notEnough.questionIds.get(0), "meaning");
        finish(notEnough, 8); assertThat(service.passed(notEnough)).isFalse();
    }
    @Test void resumeReturnsSameRunAndDoesNotRevealAnswersBeforeCompletion() {
        var attempt = attempt(); var run = service.start(attempt);
        assertThat(service.start(attempt).id).isEqualTo(run.id);
        Map<?, ?> question = (Map<?, ?>) service.view(attempt).get("question");
        assertThat(question.containsKey("correctId")).isFalse();
        assertThat(question.containsKey("explanation")).isFalse();
        assertThatThrownBy(() -> service.answer(run, "another-question", "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.answer(run, run.questionIds.get(0), "forged")).isInstanceOf(IllegalArgumentException.class);
        assertThat(run.answers).isEmpty();
    }
    @Test void retestRequiresTargetedPracticeAndUsesAlternateVariants() {
        var attempt = attempt(); var first = service.start(attempt); finish(first, 7);
        assertThatThrownBy(() -> service.start(attempt)).isInstanceOf(IllegalArgumentException.class);
        first.answers.stream().filter(a -> !a.correct).forEach(answer -> {
            var result = new ReplyCoachAttempt.PracticeResult();
            result.key = first.id + ":practice-" + bank.get(answer.questionId).practiceNumber();
            attempt.getPracticeResults().add(result);
        });
        var second = service.start(attempt);
        assertThat(second.id).isNotEqualTo(first.id);
        assertThat(second.questionIds).doesNotContainAnyElementsOf(first.questionIds);
        assertThat(attempt.getReadinessRuns()).hasSize(2);
    }
    @Test void supportIsSavedBeforeBeingRevealedAndCannotBeHiddenAgain() {
        var attempt = attempt(); var run = service.start(attempt); String id = run.questionIds.get(0);
        service.support(run, id, "meaning"); service.support(run, id, "meaning");
        service.answer(run, id, bank.get(id).correctId());
        assertThat(run.answers.get(0).helpTypes).containsExactly("meaning");
        assertThatThrownBy(() -> service.answer(run, id, bank.get(id).correctId())).isInstanceOf(IllegalArgumentException.class);
        assertThat(run.answers).hasSize(1);
    }
    @Test void oldScoresAloneNeverProveReadinessAndPolicyIsConfigurable() {
        var attempt = attempt(); attempt.setFinalPercentage(100);
        assertThat(service.view(attempt).get("ready")).isEqualTo(false);
        var run = service.start(attempt); finish(run, 8);
        assertThat(new ReplyCoachReadiness(bank, 90).passed(run)).isFalse();
        assertThat(service.policy().get("provisional")).isEqualTo(true);
    }
    @Test void romajiIsVisibleByDefaultWithoutRecordingOptionalHelp() {
        var attempt = attempt(); var run = service.start(attempt);
        Map<?, ?> question = (Map<?, ?>) service.view(attempt).get("question");
        List<?> choices = (List<?>) question.get("choices");
        for (Object choice : choices) assertThat((String) ((Map<?, ?>) choice).get("romaji")).isNotBlank();
        assertThat(run.support).isEmpty();
        finish(run, 10); assertThat(service.passed(run)).isTrue();
    }
}
