package japlearn.demo.Config;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import japlearn.demo.Entity.AppSetting;
import japlearn.demo.Entity.QuackmanContent;
import japlearn.demo.Repository.AppSettingRepository;
import japlearn.demo.Repository.QuackmanContentRepository;

/**
 * One-time Quackman content update (runs once, then records that it ran, so words an admin
 * later edits or deletes in the portal are never changed back):
 * - replaces the very long "komorebi" opening prompt with an easier word and short hint,
 * - adds two beginner words so there are six,
 * - fills the default six words if the collection is empty.
 */
@Component
public class QuackmanContentSeeder implements CommandLineRunner {

    private static final String MARKER = "seed:quackman-content-v1";

    private record Word(String romaji, String hint) {}

    private static final Word SAKURA = new Word("sakura", "Pink flowers that bloom in Japan every spring.");
    private static final List<Word> ADDED = List.of(
            new Word("neko", "A cute pet that says \"meow.\""),
            new Word("sushi", "Rice with raw fish, a famous Japanese food."));
    private static final List<Word> DEFAULTS = List.of(
            SAKURA,
            new Word("tsundoku", "Buying books and letting them pile up unread. It combines the love of collecting books with the intention to read them “someday.”"),
            new Word("kintsugi", "The art of repairing broken pottery with gold, embracing cracks and imperfections as part of the object’s history and beauty."),
            new Word("ikigai", "A reason for being — the thing that gives your life purpose, motivation, or joy each day."),
            ADDED.get(0), ADDED.get(1));

    private final QuackmanContentRepository words;
    private final AppSettingRepository settings;

    public QuackmanContentSeeder(QuackmanContentRepository words, AppSettingRepository settings) {
        this.words = words;
        this.settings = settings;
    }

    @Override
    public void run(String... args) {
        try {
            if (settings.existsById(MARKER)) return;
            List<QuackmanContent> current = words.findAll();
            if (current.isEmpty()) {
                for (Word w : DEFAULTS) words.save(content(w));
            } else {
                boolean hasSakura = current.stream().anyMatch(c -> is(c, "sakura"));
                for (QuackmanContent c : current) {
                    if (!is(c, "komorebi")) continue;
                    if (hasSakura) words.delete(c);
                    else { c.setRomajiWord(SAKURA.romaji()); c.setDescription(SAKURA.hint()); words.save(c); hasSakura = true; }
                }
                if (!hasSakura) words.save(content(SAKURA));
                for (Word w : ADDED)
                    if (current.stream().noneMatch(c -> is(c, w.romaji()))) words.save(content(w));
            }
            settings.save(new AppSetting(MARKER, true));
        } catch (Exception e) {
            // Never block startup over sample content; it is retried on the next start.
            System.err.println("Quackman content seed skipped: " + e.getMessage());
        }
    }

    private static boolean is(QuackmanContent c, String romaji) {
        return c.getRomajiWord() != null && c.getRomajiWord().trim().toLowerCase(Locale.ROOT).equals(romaji);
    }

    private static QuackmanContent content(Word w) {
        QuackmanContent c = new QuackmanContent();
        c.setRomajiWord(w.romaji());
        c.setDescription(w.hint());
        return c;
    }
}
