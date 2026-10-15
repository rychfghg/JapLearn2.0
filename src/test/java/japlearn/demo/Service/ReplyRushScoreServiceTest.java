package japlearn.demo.Service;
import japlearn.demo.Entity.Score;
import japlearn.demo.Repository.ScoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ReplyRushScoreServiceTest {
    @Test void timedChallengeAllowsSkippingPracticeAndDuplicateSyncIsIdempotent()throws Exception {
        var repo=mock(ScoreRepository.class);var service=new ScoreService();ReflectionTestUtils.setField(service,"scoreRepository",repo);
        Score timed=new ReplyRushEvidenceTest().round("RUSH_TIMED_V2");timed.setEmail("a@example.com");timed.setClientAttemptId("run-1");
        when(repo.findByEmailIgnoreCaseAndClientAttemptId("a@example.com","run-1")).thenReturn(Optional.empty());
        when(repo.save(any(Score.class))).thenAnswer(call->call.getArgument(0));
        assertEquals("RUSH_TIMED_V2",service.saveHighScore(timed).getMode());
        when(repo.findByEmailIgnoreCaseAndClientAttemptId("a@example.com","run-1")).thenReturn(Optional.of(timed));
        assertSame(timed,service.saveHighScore(timed));
        assertSame(timed,service.saveScore(timed));
        verify(repo,times(2)).save(any(Score.class));
        verify(repo,never()).findByEmailIgnoreCaseOrderByDateDesc(anyString());
    }
}
