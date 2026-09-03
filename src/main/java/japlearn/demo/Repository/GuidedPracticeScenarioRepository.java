package japlearn.demo.Repository;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import japlearn.demo.Entity.GuidedPracticeScenario;
public interface GuidedPracticeScenarioRepository extends MongoRepository<GuidedPracticeScenario,String> {
 List<GuidedPracticeScenario> findByPublishedTrueOrderByTitleAsc();
}
