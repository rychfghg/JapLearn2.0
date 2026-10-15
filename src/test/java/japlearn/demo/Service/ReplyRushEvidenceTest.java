package japlearn.demo.Service;
import japlearn.demo.Entity.Score;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReplyRushEvidenceTest {
    Score round(String mode) throws Exception {
        Score score=new Score();score.setGame("QUACKRESPONSE_RUSH");score.setMode(mode);score.setAssessmentVersion(2);
        var bank=new ObjectMapper().readTree(getClass().getResourceAsStream("/reply-rush-v2.json"));
        List<Map<String,Object>> events=new ArrayList<>();
        bank.fieldNames().forEachRemaining(id->{if(!id.startsWith("n_practice_library_"))events.add(new HashMap<>(Map.of("nodeId",id,"outcome","CORRECT","choiceId","a","timingEligible",true,"responseMs",1500)));});
        score.setRushOutcomes(events);return score;
    }
    @Test void practiceNeverProducesSpeedAndCorrectnessIsServerDerived()throws Exception {
        var score=round("RUSH_PRACTICE_V2");score.getRushOutcomes().get(0).put("choiceId","d");
        ReplyRushEvidence.normalize(score);
        assertEquals(19,score.getCorrectAnswers());assertEquals(20,score.getTotalQuestions());
        assertEquals(1,score.getRushMetrics().get("incorrect"));assertNull(score.getRushMetrics().get("meanResponseMs"));
    }
    @Test void timeoutIsNotAnIncorrectAnswer()throws Exception {
        var score=round("RUSH_TIMED_V2");var event=score.getRushOutcomes().get(0);event.put("outcome","TIMEOUT");event.remove("choiceId");
        ReplyRushEvidence.normalize(score);
        assertEquals(19,score.getTotalQuestions());assertEquals(1,score.getRushMetrics().get("timeouts"));assertEquals(0,score.getRushMetrics().get("incorrect"));assertEquals(100L,score.getRushMetrics().get("answeredAccuracy"));
    }
    @Test void interruptionDoesNotCreateWrongAnswerOrSpeedSample()throws Exception {
        var score=round("RUSH_TIMED_V2");String node=(String)score.getRushOutcomes().get(0).get("nodeId");
        score.getRushOutcomes().add(Map.of("nodeId",node,"outcome","TECHNICAL_INTERRUPTION","reason","audio"));
        ReplyRushEvidence.normalize(score);
        assertEquals(20,score.getCorrectAnswers());assertEquals(19,score.getRushMetrics().get("speedSamples"));assertEquals(1,score.getRushMetrics().get("technicalInterruptions"));
    }
    @Test void rejectDuplicateQuestionUnknownChoiceAndIncompleteRound()throws Exception {
        var duplicate=round("RUSH_PRACTICE_V2");duplicate.getRushOutcomes().add(duplicate.getRushOutcomes().get(0));assertThrows(IllegalArgumentException.class,()->ReplyRushEvidence.normalize(duplicate));
        var unknown=round("RUSH_PRACTICE_V2");unknown.getRushOutcomes().get(0).put("choiceId","forged");assertThrows(IllegalArgumentException.class,()->ReplyRushEvidence.normalize(unknown));
        var incomplete=round("RUSH_PRACTICE_V2");incomplete.getRushOutcomes().remove(0);assertThrows(IllegalArgumentException.class,()->ReplyRushEvidence.normalize(incomplete));
    }
    @Test void practiceCannotHaveTimeouts()throws Exception {
        var score=round("RUSH_PRACTICE_V2");score.getRushOutcomes().get(0).put("outcome","TIMEOUT");assertThrows(IllegalArgumentException.class,()->ReplyRushEvidence.normalize(score));
    }
    @Test void newUntimedReviewIsFirstAttemptOnlyAndCannotAlterRound()throws Exception {
        var score=round("RUSH_TIMED_V2");score.getRushOutcomes().get(0).put("choiceId","d");String id=(String)score.getRushOutcomes().get(0).get("nodeId");ReplyRushEvidence.normalize(score);
        var bank=new ObjectMapper().readTree(getClass().getResourceAsStream("/reply-rush-v2.json"));String best=bank.get(id).path("followup").asText(),wrong=bank.get(id).path("alternate").asText();
        ReplyRushEvidence.review(score,List.of(Map.of("nodeId",id,"selectedJapanese",wrong)));
        ReplyRushEvidence.review(score,List.of(Map.of("nodeId",id,"selectedJapanese",best)));
        assertEquals(1,score.getRushFollowups().size());assertEquals(false,score.getRushFollowups().get(0).get("correct"));assertEquals(19,score.getScore());
    }
    @Test void legacyResultsAreNotRelabeled() {
        var score=new Score();score.setGame("QUACKRESPONSE_RUSH");score.setScore(12);ReplyRushEvidence.normalize(score);assertEquals(12,score.getScore());assertFalse(ReplyRushEvidence.isTimed(score));
    }
    @Test void shortPracticeHasFourQuestionsAndCannotMixInTimedQuestions()throws Exception {
        var shortRun=round("RUSH_PRACTICE_SHORT_V3");shortRun.setAssessmentVersion(3);
        List<Map<String,Object>> answers=new ArrayList<>();
        for(String skill:List.of("greet","purpose","clarify","confirm"))answers.add(new HashMap<>(Map.of("nodeId","n_practice_library_"+skill+"_choice","choiceId","a","outcome","CORRECT")));
        shortRun.setRushOutcomes(answers);ReplyRushEvidence.normalize(shortRun);
        assertEquals(4,shortRun.getTotalQuestions());assertEquals(4,shortRun.getCorrectAnswers());assertNull(shortRun.getRushMetrics().get("meanResponseMs"));
        shortRun.getRushOutcomes().add(Map.of("nodeId","n_ward_greeting_choice","choiceId","a","outcome","CORRECT"));
        assertThrows(IllegalArgumentException.class,()->ReplyRushEvidence.normalize(shortRun));
    }
}
