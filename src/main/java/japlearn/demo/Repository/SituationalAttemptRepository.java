package japlearn.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.SituationalAttempt;

public interface SituationalAttemptRepository extends MongoRepository<SituationalAttempt, String> {
    List<SituationalAttempt> findByEmailIgnoreCaseOrderByCompletedAtDesc(String email);
    List<SituationalAttempt> findByEmailIgnoreCaseAndCompletedTrueOrderByCompletedAtDesc(String email);
    List<SituationalAttempt> findByGameTypeIgnoreCaseOrderByCompletedAtDesc(String gameType);
    Optional<SituationalAttempt> findTopByEmailIgnoreCaseAndGameTypeIgnoreCaseOrderByScoreDesc(String email, String gameType);
}
