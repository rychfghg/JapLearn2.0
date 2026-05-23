package japlearn.demo.Service;

import japlearn.demo.Entity.QuackslateContent;
import japlearn.demo.Repository.QuackslateContentRepository;
import japlearn.demo.Repository.QuackslateGameCodeRepository; // Import game repository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuackslateContentService {

    private final QuackslateContentRepository quackslateContentRepository;
    private final QuackslateGameCodeRepository quackslateGameRepository; // Added repository for game

    @Autowired
    public QuackslateContentService(QuackslateContentRepository quackslateContentRepository, 
                                    QuackslateGameCodeRepository quackslateGameRepository) {
        this.quackslateContentRepository = quackslateContentRepository;
        this.quackslateGameRepository = quackslateGameRepository; // Initialize repository
    }

    // Add new content
    public QuackslateContent addQuackslateContent(QuackslateContent quackslateContent) {
        return quackslateContentRepository.save(quackslateContent);
    }

    // Get all content (shared across all class codes)
    public List<QuackslateContent> getAllQuackslateContent() {
        return quackslateContentRepository.findAll();
    }

    // Get content by ID
    public Optional<QuackslateContent> getQuackslateContentById(int id) {
        return quackslateContentRepository.findById(id);
    }

    // Get content by gameCode (optional)
    public List<QuackslateContent> getByGameCode(String gameCode) {
        return quackslateContentRepository.findByGameCode(gameCode);
    }

    // Update content
    public QuackslateContent updateQuackslateContent(QuackslateContent quackslateContent) {
        return quackslateContentRepository.save(quackslateContent);
    }

    // Delete content by ID
    public void deleteQuackslateContent(int id) {
        quackslateContentRepository.deleteById(id);
    }

    // Notify that the quiz has started based on the gameCode
    public boolean notifyGameStart(String gameCode) {
        // Here, you would have the logic to notify clients (e.g., using WebSocket or updating database state)
        // This is a placeholder method for demonstration purposes.

        // Check if the gameCode exists and notify connected clients
        return quackslateGameRepository.findByGameCode(gameCode).isPresent();
    }
}
