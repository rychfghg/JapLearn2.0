package japlearn.demo.Config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import japlearn.demo.Entity.AppSetting;
import japlearn.demo.Entity.SituationalQuestion;
import japlearn.demo.Repository.AppSettingRepository;
import japlearn.demo.Repository.SituationalQuestionRepository;

/**
 * One-time Politeness content fix (runs once, then records that it ran, so later portal edits are kept):
 * the internship self-introduction used a real surname; it now uses the neutral name やまだ (Yamada).
 */
@Component
public class PolitenessNameSeeder implements CommandLineRunner {

    private static final String MARKER = "seed:politeness-neutral-name-v1";
    private static final String OLD_JP = "バコロド", NEW_JP = "やまだ";
    private static final String OLD_ROMAJI = "Bakorodo", NEW_ROMAJI = "Yamada";

    private final SituationalQuestionRepository questions;
    private final AppSettingRepository settings;

    public PolitenessNameSeeder(SituationalQuestionRepository questions, AppSettingRepository settings) {
        this.questions = questions;
        this.settings = settings;
    }

    @Override
    public void run(String... args) {
        try {
            if (settings.existsById(MARKER)) return;
            for (SituationalQuestion q : questions.findByGameTypeIgnoreCaseOrderByOrderAsc("POLITENESS")) {
                boolean changed = false;
                for (SituationalQuestion.ResponseChoice c : q.getChoices()) {
                    if (c.getJapanese() != null && c.getJapanese().contains(OLD_JP)) { c.setJapanese(c.getJapanese().replace(OLD_JP, NEW_JP)); changed = true; }
                    if (c.getRomaji() != null && c.getRomaji().contains(OLD_ROMAJI)) { c.setRomaji(c.getRomaji().replace(OLD_ROMAJI, NEW_ROMAJI)); changed = true; }
                }
                if (q.getCorrectAnswer() != null && q.getCorrectAnswer().contains(OLD_JP)) { q.setCorrectAnswer(q.getCorrectAnswer().replace(OLD_JP, NEW_JP)); changed = true; }
                if (changed) questions.save(q);
            }
            settings.save(new AppSetting(MARKER, true));
        } catch (Exception e) {
            // Never block startup over content; it is retried on the next start.
            System.err.println("Politeness name update skipped: " + e.getMessage());
        }
    }
}
