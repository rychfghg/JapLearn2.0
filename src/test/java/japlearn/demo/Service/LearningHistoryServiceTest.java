package japlearn.demo.Service;

import java.time.Instant;
import java.util.*;
import japlearn.demo.Entity.*;
import japlearn.demo.Repository.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LearningHistoryServiceTest {
    private final SituationalRunRepository runs=mock(SituationalRunRepository.class);
    private final ResponseRushProgressRepository rush=mock(ResponseRushProgressRepository.class);
    private final DialogueRelayProgressRepository relay=mock(DialogueRelayProgressRepository.class);
    private final LearningHistoryService history=new LearningHistoryService(runs,rush,relay);
    private void emptyCheckpoints(){when(runs.findByEmailIgnoreCase("s@example.com")).thenReturn(List.of());when(rush.findByEmailIgnoreCase("s@example.com")).thenReturn(Optional.empty());when(relay.findByEmailIgnoreCase("s@example.com")).thenReturn(Optional.empty());}
    @Test void failedAndTimedAttemptsRemainVisibleAndChronological(){
        emptyCheckpoints();
        var failed=new SituationalAttempt();failed.setId("failed");failed.setGameType("RECOGNITION");failed.setEndReason("GAME_OVER");failed.setCompleted(false);failed.setCompletedAt(Instant.parse("2026-10-18T01:00:00Z"));
        var timed=new Score();timed.setId("timed");timed.setGame("QUACKRESPONSE_RUSH");timed.setMode("RUSH_TIMED_V2");timed.setScore(0);timed.setMaxScore(4);timed.setDate("2026-10-18T02:00:00Z");timed.setCompleted(true);
        var result=history.history("s@example.com",List.of(failed),List.of(),List.of(),List.of(timed));
        assertEquals(2,result.size());assertEquals("Response Rush",result.get(0).get("title"));assertEquals(true,result.get(0).get("timed"));assertEquals(0,result.get(0).get("score"));assertEquals("FAILED",result.get(1).get("status"));
    }
    @Test void firstIncorrectAnswerCheckpointIsVisibleButStaleFinishedRunIsNot(){
        emptyCheckpoints();var run=new SituationalRun();run.setId("run");run.setGameType("RECOGNITION");run.setEasyMistakes(1);run.setUpdatedAt(Instant.parse("2026-10-18T01:00:00Z"));when(runs.findByEmailIgnoreCase("s@example.com")).thenReturn(List.of(run));
        assertEquals("IN_PROGRESS",history.history("s@example.com",List.of(),List.of(),List.of(),List.of()).get(0).get("status"));
        var done=new SituationalAttempt();done.setId("done");done.setGameType("RECOGNITION");done.setCompleted(true);done.setCompletedAt(Instant.parse("2026-10-18T02:00:00Z"));
        assertEquals(1,history.history("s@example.com",List.of(done),List.of(),List.of(),List.of()).size());
    }
    @Test void unfinishedCoachAndUnscoredTalkAreNotDiscarded(){
        emptyCheckpoints();var coach=new ReplyCoachAttempt();coach.setId("coach");coach.setStatus("IN_PROGRESS");coach.setUpdatedAt(Instant.now());
        var talk=new QuackTalkSession();talk.setId("talk");talk.setRoomType("TALK_WITH_SUMI");talk.setCompleted(true);talk.setEvaluated(false);
        var result=history.history("s@example.com",List.of(),List.of(coach),List.of(talk),List.of());assertEquals(2,result.size());assertTrue(result.stream().anyMatch(r->"IN_PROGRESS".equals(r.get("status"))));assertNull(result.stream().filter(r->"talk:talk".equals(r.get("id"))).findFirst().orElseThrow().get("score"));
    }
}
