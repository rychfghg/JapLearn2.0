package japlearn.demo.Config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import japlearn.demo.Entity.ReplyCoachChapter;
import japlearn.demo.Repository.ReplyCoachChapterRepository;

class ReplyCoachContentRevisionTest {
    private ReplyCoachChapter seeded() throws Exception {
        var repository = mock(ReplyCoachChapterRepository.class);
        var result = new AtomicReference<ReplyCoachChapter>();
        when(repository.findByTitleIgnoreCase(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(call -> { ReplyCoachChapter c = call.getArgument(0); result.set(c); return c; });
        new ReplyCoachChapterSeeder().seedReplyCoachChapter(repository).run();
        return result.get();
    }
    private ReplyCoachChapter.StoryNode node(ReplyCoachChapter chapter, String id) {
        return chapter.getNodes().stream().filter(n -> id.equals(n.getId())).findFirst().orElseThrow();
    }
    @Test void freshChapterHasConnectedScenesDistinctHintsAndNarrationInsteadOfSpokenStageDirections() throws Exception {
        var chapter = seeded();
        assertThat(chapter.getStoryVersion()).isEqualTo(3);
        var decisions = chapter.getNodes().stream().filter(n -> "CHOICE".equals(n.getType())).toList();
        assertThat(decisions).hasSize(20);
        assertThat(decisions.stream().map(n -> n.getHint()).distinct().count()).isEqualTo(20);
        for (int i = 1; i <= 20; i++) {
            assertThat(node(chapter, "dialogue-" + i).getNextNodeId()).isEqualTo("decision-" + i);
            var merge = node(chapter, "merge-" + i);
            assertThat(merge.getType()).isEqualTo("NARRATION");
            assertThat(merge.getSpeaker()).isNull(); assertThat(merge.getJapanese()).isNull();
            assertThat(merge.getRomaji()).isNull(); assertThat(merge.isSpritesVisible()).isFalse();
            assertThat(node(chapter, "decision-" + i).getChoices()).hasSize(4);
        }
        assertThat(node(chapter, "dialogue-1").getText()).isEqualTo("You finally made it!");
        assertThat(node(chapter, "scene-13").getText()).contains("earlier online course");
        assertThat(node(chapter, "decision-8").getHint()).contains("nin and mei");
        assertThat(node(chapter, "decision-8").getChoices().stream().filter(c -> "8-acceptable".equals(c.getId())).findFirst().orElseThrow().getExplanation()).contains("also a correct, polite");
    }
    @Test void existingStockMigrationKeepsIdsAudioScoresAndResumeNodesAndRunsOnlyOnce() throws Exception {
        var chapter = seeded(); chapter.setId("existing"); chapter.setStoryVersion(2);
        var merge = node(chapter, "merge-1");
        merge.setText("Sumi and Haru acknowledge your response, and the three of you continue together.");
        merge.setType("DIALOGUE"); merge.setSpeaker("Sumi"); merge.setJapanese("いい経験になったね。続きを見に行こう！");
        var line = node(chapter, "dialogue-1"); line.setText("You made it! How was the trip?"); line.setAudioUrl("/existing-voice.mp3");
        var ids = chapter.getNodes().stream().map(n -> n.getId()).toList();
        var points = chapter.getNodes().stream().flatMap(n -> n.getChoices().stream()).map(c -> c.getPoints()).toList();
        assertThat(ReplyCoachContentRevision.apply(chapter)).isTrue();
        assertThat(chapter.getId()).isEqualTo("existing");
        assertThat(chapter.getNodes().stream().map(n -> n.getId()).toList()).containsExactlyElementsOf(ids);
        assertThat(chapter.getNodes().stream().flatMap(n -> n.getChoices().stream()).map(c -> c.getPoints()).toList()).containsExactlyElementsOf(points);
        assertThat(line.getAudioUrl()).isEqualTo("/existing-voice.mp3");
        assertThat(line.getJapanese()).isEqualTo("やっと来たね！");
        assertThat(node(chapter, "dialogue-1-companion")).isNotNull();
        assertThat(merge.getType()).isEqualTo("NARRATION");
        assertThat(ReplyCoachContentRevision.apply(chapter)).isFalse();
    }
    @Test void customHintsExplanationsDialogueAndTransitionsArePreserved() throws Exception {
        var chapter = seeded(); chapter.setStoryVersion(2);
        var line = node(chapter, "dialogue-1"); line.setText("Instructor-written translation"); line.setNextNodeId("dialogue-1-companion");
        node(chapter, "dialogue-1-companion").setText("Instructor-written companion dialogue");
        var decision = node(chapter, "decision-1"); decision.setHint("Instructor-written hint");
        decision.getChoices().get(0).setExplanation("Instructor-written explanation");
        var merge = node(chapter, "merge-1"); merge.setText("Instructor-written transition");
        assertThat(ReplyCoachContentRevision.apply(chapter)).isTrue();
        assertThat(line.getText()).isEqualTo("Instructor-written translation");
        assertThat(line.getNextNodeId()).isEqualTo("dialogue-1-companion");
        assertThat(decision.getHint()).isEqualTo("Instructor-written hint");
        assertThat(decision.getChoices().get(0).getExplanation()).isEqualTo("Instructor-written explanation");
        assertThat(merge.getText()).isEqualTo("Instructor-written transition");
    }
}
