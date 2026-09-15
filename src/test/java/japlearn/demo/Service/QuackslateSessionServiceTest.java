package japlearn.demo.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import japlearn.demo.Entity.QuackslateGameCode;
import japlearn.demo.Entity.QuackslateQuestion;
import japlearn.demo.Repository.QuackslateQuestionRepository;
import japlearn.demo.Repository.QuackslateContentRepository;
import japlearn.demo.Repository.QuackslateGameCodeRepository;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;

class QuackslateSessionServiceTest {
    @Test
    void unusedCodeIsDeletedButJoinedCodeKeepsItsHistory() {
        var games = mock(QuackslateGameCodeRepository.class);
        var content = mock(QuackslateContentRepository.class);
        var service = new QuackslateSessionService(null, games, null, content, null, null, null);
        var game = new QuackslateGameCode("ABCDE");
        game.setOwnerTeacherEmail("teacher@example.com");
        when(games.findByGameCode("ABCDE")).thenReturn(Optional.of(game));
        when(content.findByGameCode("ABCDE")).thenReturn(List.of());
        service.deleteSession("teacher@example.com", "abcde");
        verify(games).delete(game);
        game.setJoinedStudentEmails(List.of("learner@example.com"));
        assertThrows(ResponseStatusException.class, () -> service.deleteSession("teacher@example.com", "ABCDE"));
        verify(games, times(1)).delete(game);
        assertThrows(ResponseStatusException.class, () -> service.deleteSession("another@example.com", "ABCDE"));
    }
    @Test
    void bankIncludesSystemAndOwnQuestionsButNotOtherTeachers() {
        var repository = mock(QuackslateQuestionRepository.class);
        var service = new QuackslateSessionService(null, null, repository, null, null, null, null);
        var system = new QuackslateQuestion(); system.setCreatedBy("SYSTEM"); system.setApproved(true);
        var own = new QuackslateQuestion(); own.setCreatedBy("teacher@example.com");
        var other = new QuackslateQuestion(); other.setCreatedBy("other@example.com"); other.setApproved(true);
        when(repository.findAll()).thenReturn(List.of(system, own, other));
        assertEquals(List.of(system, own), service.availableQuestions("teacher@example.com"));
    }

    @Test
    void orderedTilesSaveAndMismatchedAnswerIsRejected() {
        var repository = mock(QuackslateQuestionRepository.class);
        var service = new QuackslateSessionService(null, null, repository, null, null, null, null);
        var question = new QuackslateQuestion();
        question.setPrompt("Introduce yourself"); question.setTranslation("私はレイです");
        question.setCorrectAnswer("私は レイです"); question.setOptions(new String[]{" 私は ", "レイです"});
        when(repository.save(any(QuackslateQuestion.class))).thenAnswer(call -> call.getArgument(0));
        assertEquals("teacher@example.com", service.addQuestion("teacher@example.com", question).getCreatedBy());
        assertEquals("私は", question.getOptions()[0]);
        question.setCorrectAnswer("missing word");
        assertThrows(ResponseStatusException.class, () -> service.addQuestion("teacher@example.com", question));
        verify(repository, times(1)).save(any(QuackslateQuestion.class));
    }
    @Test
    void scheduledWindowOpensAndClosesAtConfiguredInstants() {
        QuackslateGameCode session = new QuackslateGameCode();
        Instant start = Instant.parse("2026-09-15T14:10:00Z");
        Instant end = Instant.parse("2026-09-15T14:40:00Z");
        assertEquals("DRAFT", QuackslateSessionService.status(session, start));
        session.setStartsAt(start);
        session.setEndsAt(end);
        session.setPublished(true);
        assertEquals("UPCOMING", QuackslateSessionService.status(session, start.minusSeconds(1)));
        assertEquals("LIVE", QuackslateSessionService.status(session, start));
        assertEquals("LIVE", QuackslateSessionService.status(session, end.minusSeconds(1)));
        assertEquals("ENDED", QuackslateSessionService.status(session, end));
    }
}
