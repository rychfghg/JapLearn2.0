package japlearn.demo.Controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import japlearn.demo.Entity.QuackTalkSession;
import japlearn.demo.Repository.QuackTalkSessionRepository;
import japlearn.demo.Repository.UserRepository;
import japlearn.demo.Service.GuidedPhrasePronunciationService;
import japlearn.demo.Service.LiveConversationProvider;

class TalkWithSumiLevelTest {

    private final QuackTalkSessionRepository sessions = mock(QuackTalkSessionRepository.class);
    private final TalkWithSumiController controller = new TalkWithSumiController(mock(LiveConversationProvider.class),
            mock(GuidedPhrasePronunciationService.class), sessions, mock(UserRepository.class),
            mock(org.springframework.data.mongodb.core.MongoTemplate.class));

    private static QuackTalkSession sumi(int... scores) {
        QuackTalkSession s = new QuackTalkSession();
        s.setRoomType("TALK_WITH_SUMI");
        s.setContextEvaluations(java.util.Arrays.stream(scores).mapToObj(v -> Map.<String, Object>of("contextScore", v, "japaneseAttempted", true)).toList());
        return s;
    }

    private String levelFor(QuackTalkSession... history) {
        ReflectionTestUtils.setField(controller, "developingFrom", 60);
        ReflectionTestUtils.setField(controller, "confidentFrom", 80);
        when(sessions.findByEmailIgnoreCaseOrderByPracticedAtDesc("ana@gmail.com")).thenReturn(List.of(history));
        return controller.learnerLevel("ana@gmail.com");
    }

    @Test
    void beginnerWithoutEnoughHistory() {
        assertThat(levelFor()).isEqualTo("BEGINNER");
        assertThat(levelFor(sumi(90, 95))).isEqualTo("BEGINNER");
    }

    @Test
    void levelFollowsRecentContextScores() {
        assertThat(levelFor(sumi(40, 50, 55))).isEqualTo("BEGINNER");
        assertThat(levelFor(sumi(65, 70, 72))).isEqualTo("DEVELOPING");
        assertThat(levelFor(sumi(85, 90), sumi(80))).isEqualTo("CONFIDENT");
    }

    @Test
    void ignoresGuidedPhraseAndUnmeasuredSessions() {
        QuackTalkSession guided = sumi(100, 100, 100);
        guided.setRoomType("GUIDED_PHRASE");
        QuackTalkSession old = new QuackTalkSession();
        old.setRoomType("TALK_WITH_SUMI");
        assertThat(levelFor(guided, old)).isEqualTo("BEGINNER");
    }
}
