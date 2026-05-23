package japlearn.demo.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import japlearn.demo.Entity.QuackamoleContent;
import japlearn.demo.Repository.QuackamoleContentRepository;

@Service
public class QuackamoleContentService {

    private final QuackamoleContentRepository repository;

    public QuackamoleContentService(QuackamoleContentRepository repository) {
        this.repository = repository;
    }

    public List<QuackamoleContent> getAllContent() {
        // Return all QuackamoleContent entities from the database
        return repository.findAll();
    }

    public QuackamoleContent addCharacter(String kana, String romaji) {
        // Assuming there is only one document in the collection
        QuackamoleContent content = repository.findAll().stream().findFirst().orElse(new QuackamoleContent());
        if (content.getKana() == null || content.getRomaji() == null) {
            content.setKana(new java.util.ArrayList<>());
            content.setRomaji(new java.util.ArrayList<>());
        }
        content.getKana().add(kana);
        content.getRomaji().add(romaji);
        return repository.save(content);
    }

    public QuackamoleContent removeCharacter(String kana, String romaji) {
        // Assuming there is only one document in the collection
        QuackamoleContent content = repository.findAll().stream().findFirst().orElseThrow(() -> new RuntimeException("Content not found"));
        content.getKana().remove(kana);
        content.getRomaji().remove(romaji);
        return repository.save(content);
    }
}
