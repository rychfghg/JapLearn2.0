package japlearn.demo.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import japlearn.demo.Entity.QuackmanContent;
import japlearn.demo.Repository.QuackmanContentRepository;

@Service
public class QuackmanContentService {

    @Autowired
    private QuackmanContentRepository repository;

    public List<QuackmanContent> getAllContent() {
        return repository.findAll();
    }

    public QuackmanContent addContent(QuackmanContent content) {
        return repository.save(content);
    }

    public QuackmanContent updateContent(String id, QuackmanContent updatedContent) {
        Optional<QuackmanContent> existingContent = repository.findById(id);
        if (existingContent.isPresent()) {
            QuackmanContent content = existingContent.get();
            content.setRomajiWord(updatedContent.getRomajiWord());
            content.setDescription(updatedContent.getDescription());
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
}
