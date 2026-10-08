package japlearn.demo.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.DialogueRelayProgress;

public interface DialogueRelayProgressRepository extends MongoRepository<DialogueRelayProgress, String> {
    Optional<DialogueRelayProgress> findByEmailIgnoreCase(String email);
    void deleteByEmailIgnoreCase(String email);
}
