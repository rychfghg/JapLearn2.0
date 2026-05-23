package japlearn.demo.Repository;

import japlearn.demo.Entity.QuackslateContent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuackslateContentRepository extends MongoRepository<QuackslateContent, Integer> {
    
    // Method to get all content for a specific game code (optional)
    List<QuackslateContent> findByGameCode(String gameCode);

    // Method to get all content (this will be shared across all class codes)
    @Override
    List<QuackslateContent> findAll();
}
