package japlearn.demo.Repository;

import japlearn.demo.Entity.QuackslateGameCode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuackslateGameCodeRepository extends MongoRepository<QuackslateGameCode, Integer> {

    // Custom method to find a game by its game code
    Optional<QuackslateGameCode> findByGameCode(String gameCode);
    QuackslateGameCode findFirstByIsActiveTrue();
    
}
