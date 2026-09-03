package japlearn.demo.Repository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import japlearn.demo.Entity.GuidedPracticeUsage;
public interface GuidedPracticeUsageRepository extends MongoRepository<GuidedPracticeUsage,String> {
 Optional<GuidedPracticeUsage> findByEmailIgnoreCaseAndDate(String email, LocalDate date);
}
