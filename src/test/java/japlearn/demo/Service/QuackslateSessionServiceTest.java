package japlearn.demo.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import japlearn.demo.Entity.QuackslateGameCode;

class QuackslateSessionServiceTest {
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
