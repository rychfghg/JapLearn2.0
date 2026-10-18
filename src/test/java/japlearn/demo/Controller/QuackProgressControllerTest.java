package japlearn.demo.Controller;

import japlearn.demo.Entity.*;
import japlearn.demo.Repository.*;
import japlearn.demo.Service.LearningHistoryService;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class QuackProgressControllerTest {
    @Test void completedZeroIsMeasuredButTimedSpeedDoesNotAffectMastery(){
        var situations=mock(SituationalAttemptRepository.class);var replies=mock(ReplyCoachAttemptRepository.class);var talk=mock(QuackTalkSessionRepository.class);var scores=mock(ScoreRepository.class);var history=mock(LearningHistoryService.class);
        var zero=new SituationalAttempt();zero.setGameType("RECOGNITION");zero.setCompleted(true);zero.setAccuracy(0);
        var timed=new Score();timed.setGame("QUACKRESPONSE_RUSH");timed.setMode("RUSH_TIMED_V2");timed.setScore(10);timed.setMaxScore(10);timed.setCompleted(true);
        when(situations.findByEmailIgnoreCaseOrderByCompletedAtDesc("s@example.com")).thenReturn(List.of(zero));when(replies.findByEmailIgnoreCaseOrderByUpdatedAtDesc(anyString())).thenReturn(List.of());when(talk.findByEmailIgnoreCaseOrderByPracticedAtDesc(anyString())).thenReturn(List.of());when(scores.findByEmailIgnoreCaseOrderByDateDesc(anyString())).thenReturn(List.of(timed));when(history.history(anyString(),anyList(),anyList(),anyList(),anyList())).thenReturn(List.of(Map.of("status","COMPLETED")));
        var result=new QuackProgressController(situations,replies,talk,scores,history).analytics("s@example.com");
        @SuppressWarnings("unchecked") var modules=(List<Map<String,Object>>)result.get("moduleAccuracy");assertEquals(1,modules.size());assertEquals("Recognition",modules.get(0).get("label"));assertEquals(0,modules.get(0).get("value"));assertEquals(0,result.get("overallMastery"));assertTrue(String.valueOf(result.get("progressSummary")).contains("across 1 practiced"));assertEquals(1L,result.get("completedActivities"));
    }
}
