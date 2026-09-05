package japlearn.demo.Repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import japlearn.demo.Entity.DialogueRelayBonusAssessment;

public interface DialogueRelayBonusAssessmentRepository extends MongoRepository<DialogueRelayBonusAssessment, String> {
    List<DialogueRelayBonusAssessment> findByEmailIgnoreCaseOrderByAssessedAtDesc(String email);
}
