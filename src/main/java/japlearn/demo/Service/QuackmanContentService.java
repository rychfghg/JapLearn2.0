package japlearn.demo.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import japlearn.demo.Entity.QuackmanContent;
import japlearn.demo.Repository.QuackmanContentRepository;

@Service
public class QuackmanContentService {

    /** The romaji tiles the Quackman game can show; a word must be spelled with these only. */
    static final Set<String> SYLLABLES = Set.of(
            "a", "i", "u", "e", "o", "ka", "ki", "ku", "ke", "ko", "sa", "shi", "su", "se", "so", "ta", "chi", "tsu", "te", "to",
            "na", "ni", "nu", "ne", "no", "ha", "hi", "fu", "he", "ho", "ma", "mi", "mu", "me", "mo", "ya", "yu", "yo", "ra", "ri",
            "ru", "re", "ro", "wa", "wo", "n", "ga", "gi", "gu", "ge", "go", "za", "ji", "zu", "ze", "zo", "da", "de",
            "do", "ba", "bi", "bu", "be", "bo", "pa", "pi", "pu", "pe", "po");
    public static final int MAX_HINT = 140;

    @Autowired
    private QuackmanContentRepository repository;

    public List<QuackmanContent> getAllContent() {
        return repository.findAll();
    }

    public QuackmanContent addContent(QuackmanContent content) {
        QuackmanContent clean = new QuackmanContent();
        clean.setRomajiWord(word(content == null ? null : content.getRomajiWord()));
        clean.setDescription(hint(content == null ? null : content.getDescription()));
        return repository.save(clean);
    }

    public QuackmanContent updateContent(String id, QuackmanContent updatedContent) {
        Optional<QuackmanContent> existingContent = repository.findById(id);
        if (existingContent.isPresent()) {
            QuackmanContent content = existingContent.get();
            content.setRomajiWord(word(updatedContent == null ? null : updatedContent.getRomajiWord()));
            content.setDescription(hint(updatedContent == null ? null : updatedContent.getDescription()));
            return repository.save(content);
        } else {
            throw new RuntimeException("Content with ID " + id + " not found");
        }
    }

    public void deleteContent(String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new RuntimeException("Content with ID " + id + " not found");
        }
    }

    /** Lower-case romaji that splits fully into game syllables (2 to 8 tiles). */
    static String word(String raw) {
        String w = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!w.matches("[a-z]{1,24}")) throw new IllegalArgumentException("Use romaji letters only (a to z), with no spaces.");
        int tiles = 0;
        for (int i = 0; i < w.length(); ) {
            boolean found = false;
            for (int len = 3; len > 0 && !found; len--) {
                if (i + len <= w.length() && SYLLABLES.contains(w.substring(i, i + len))) { i += len; tiles++; found = true; }
            }
            if (!found) throw new IllegalArgumentException("\"" + w + "\" cannot be spelled with the game's syllable tiles.");
        }
        if (tiles < 2 || tiles > 8) throw new IllegalArgumentException("Words must have 2 to 8 syllables.");
        return w;
    }

    /** A short, plain-text hint so it never covers the game screen. */
    static String hint(String raw) {
        String h = raw == null ? "" : raw.replaceAll("[\\p{Cntrl}<>]", " ").trim().replaceAll("\\s+", " ");
        if (h.isEmpty()) throw new IllegalArgumentException("Please write a hint.");
        if (h.length() > MAX_HINT) throw new IllegalArgumentException("Keep the hint under " + MAX_HINT + " characters so it fits the game screen.");
        return h;
    }
}
