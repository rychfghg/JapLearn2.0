package japlearn.demo.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.SituationalRun;

public interface SituationalRunRepository extends MongoRepository<SituationalRun, String> {
    Optional<SituationalRun> findByEmailIgnoreCaseAndGameTypeIgnoreCase(String email, String gameType);
    void deleteByEmailIgnoreCaseAndGameTypeIgnoreCase(String email, String gameType);
}
