package japlearn.demo.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import japlearn.demo.Config.QuackmanContentSeeder;
import japlearn.demo.Entity.QuackmanContent;
import japlearn.demo.Repository.AppSettingRepository;
import japlearn.demo.Repository.QuackmanContentRepository;

class QuackmanContentServiceTest {

    @Test
    void wordsMustBePlayableWithTheGameTiles() {
        for (String ok : List.of("sakura", "Neko", "sushi", "tsundoku", "kintsugi", "ikigai", "konnichiwa"))
            assertThat(QuackmanContentService.word(ok)).isEqualTo(ok.toLowerCase());
        for (String bad : List.of("", "a", "kyoto", "sake 2", "ni-hon", "qwerty", "sakuraaaaaaaaaa"))
            assertThatThrownBy(() -> QuackmanContentService.word(bad)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hintsAreShortPlainText() {
        assertThat(QuackmanContentService.hint("  A cute   pet. ")).isEqualTo("A cute pet.");
        assertThatThrownBy(() -> QuackmanContentService.hint(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuackmanContentService.hint("x".repeat(141))).isInstanceOf(IllegalArgumentException.class);
    }

    private static QuackmanContent c(String word, String hint) {
        QuackmanContent q = new QuackmanContent(); q.setRomajiWord(word); q.setDescription(hint); return q;
    }

    @Test
    void seedReplacesKomorebiAddsTwoAndRunsOnlyOnce() throws Exception {
        QuackmanContentRepository words = mock(QuackmanContentRepository.class);
        AppSettingRepository settings = mock(AppSettingRepository.class);
        List<QuackmanContent> saved = new ArrayList<>();
        QuackmanContent komorebi = c("komorebi", "A very long prompt...");
        when(words.findAll()).thenReturn(List.of(komorebi, c("tsundoku", "x"), c("kintsugi", "x"), c("ikigai", "x")));
        when(words.save(any())).thenAnswer(i -> { saved.add(i.getArgument(0)); return i.getArgument(0); });
        when(settings.existsById("seed:quackman-content-v1")).thenReturn(false);

        new QuackmanContentSeeder(words, settings).run();
        assertThat(komorebi.getRomajiWord()).isEqualTo("sakura");
        assertThat(komorebi.getDescription()).hasSizeLessThan(60);
        assertThat(saved).extracting(QuackmanContent::getRomajiWord).containsExactlyInAnyOrder("sakura", "neko", "sushi");
        verify(settings).save(any());

        QuackmanContentRepository again = mock(QuackmanContentRepository.class);
        AppSettingRepository done = mock(AppSettingRepository.class);
        when(done.existsById("seed:quackman-content-v1")).thenReturn(true);
        new QuackmanContentSeeder(again, done).run();
        verify(again, never()).save(any());
    }
}
