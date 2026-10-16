package japlearn.demo.Service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import japlearn.demo.Entity.*;

class ExpressionMatchEvidenceTest {
    private ExpressionQuestionResult row(String id, String mode, String outcome) {
        return new ExpressionQuestionResult(id,"またあした",0,mode,outcome,
            "CORRECT".equals(outcome)?"top":"bottom","top","Meeting a manager","See you tomorrow","Meeting a manager",true,2,"2026-10-16T00:00:00Z");
    }
    @Test void technicalFailureIsNotAnIncorrectAnswer() {
        var attempt=new SituationalAttempt();attempt.setTotalQuestions(2);attempt.setCompleted(true);
        attempt.setCorrectAnswers(0);attempt.setScore(0);
        attempt.setQuestionResults(List.of(row("one","PICTURE","TECHNICAL_INTERRUPTION"),row("one","PICTURE","CORRECT"),row("two","CAPTION_ONLY","INCORRECT")));
        ExpressionMatchEvidence.normalize(attempt);
        assertEquals(1,attempt.getCorrectAnswers());assertEquals(1,attempt.getWrongAnswers());
        assertEquals(2,attempt.getAnsweredCount());assertEquals(10,attempt.getScore());assertEquals(50,attempt.getAccuracy());
        var groups=ExpressionMatchEvidence.breakdown(List.of(attempt));
        assertEquals(2,groups.size());assertEquals(1,groups.get(0).get("technicalInterruptions"));
        assertEquals(0,groups.get(0).get("incorrect"));assertEquals(1,groups.get(1).get("incorrect"));
        assertEquals(List.of("Meeting a manager"),groups.get(1).get("confusedContexts"));
    }
    @Test void technicalOnlyEvidenceNeverCompletesALevel() {
        var attempt=new SituationalAttempt();attempt.setTotalQuestions(1);attempt.setCompleted(true);
        attempt.setQuestionResults(List.of(row("one","PICTURE","TECHNICAL_INTERRUPTION")));
        assertThrows(IllegalArgumentException.class,()->ExpressionMatchEvidence.normalize(attempt));
        attempt.setCompleted(false);ExpressionMatchEvidence.normalize(attempt);
        assertEquals(0,attempt.getScore());assertEquals(0,attempt.getWrongAnswers());
    }
    @Test void rejectsDuplicateScoredQuestions() {
        var attempt=new SituationalAttempt();attempt.setQuestionResults(List.of(row("one","PICTURE","CORRECT"),row("one","PICTURE","INCORRECT")));
        assertThrows(IllegalArgumentException.class,()->ExpressionMatchEvidence.normalize(attempt));
    }
    @Test void recordsRoundTripForAttemptsAndResumableRuns() throws Exception {
        var mapper=new ObjectMapper().findAndRegisterModules();
        var rows=List.of(row("one","CAPTION_ONLY","INCORRECT"));
        var attempt=new SituationalAttempt();attempt.setQuestionResults(rows);
        assertEquals(rows,mapper.readValue(mapper.writeValueAsString(attempt),SituationalAttempt.class).getQuestionResults());
        var run=new SituationalRun();run.setExpressionMatchVersion(2);run.setQuestionIds(List.of("one"));run.setQuestionResults(rows);
        var restored=mapper.readValue(mapper.writeValueAsString(run),SituationalRun.class);
        assertEquals(rows,restored.getQuestionResults());assertEquals(List.of("one"),restored.getQuestionIds());assertEquals(2,restored.getExpressionMatchVersion());
    }
}
