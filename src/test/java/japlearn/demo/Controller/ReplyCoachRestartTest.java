package japlearn.demo.Controller;

import japlearn.demo.Entity.ReplyCoachAttempt;
import japlearn.demo.Entity.ReplyCoachChapter;
import japlearn.demo.Repository.ReplyCoachAttemptRepository;
import japlearn.demo.Repository.ReplyCoachChapterRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReplyCoachRestartTest {
    @Test void explicitRestartCreatesFreshAttemptAndPreservesOldHistory() {
        var chapters=mock(ReplyCoachChapterRepository.class);
        var attempts=mock(ReplyCoachAttemptRepository.class);
        var controller=new ReplyCoachController(chapters,attempts,null,null);
        var chapter=new ReplyCoachChapter();chapter.setStatus("PUBLISHED");chapter.setTitle("Test");chapter.setStartNodeId("opening");
        var old=new ReplyCoachAttempt();old.setId("old");old.setStatus("IN_PROGRESS");old.setCurrentNodeId("decision-4");
        when(chapters.findById("chapter")).thenReturn(Optional.of(chapter));
        when(attempts.findTopByEmailIgnoreCaseAndChapterIdAndStatusIgnoreCaseOrderByUpdatedAtDesc("s@example.com","chapter","IN_PROGRESS")).thenReturn(Optional.of(old));
        when(attempts.save(any(ReplyCoachAttempt.class))).thenAnswer(call->call.getArgument(0));
        var resumed=controller.start(Map.of("email","s@example.com","chapterId","chapter"));
        assertSame(old,resumed.getBody());verify(attempts,never()).save(any());
        var response=controller.start(Map.of("email","s@example.com","chapterId","chapter","restart","true"));
        var fresh=(ReplyCoachAttempt)response.getBody();
        assertNotSame(old,fresh);assertEquals("opening",fresh.getCurrentNodeId());assertEquals("ABANDONED",old.getStatus());
        assertTrue(fresh.getAnswers().isEmpty());verify(attempts,times(2)).save(any());verify(attempts,never()).delete(any());
    }
}
