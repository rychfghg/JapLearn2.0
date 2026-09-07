package japlearn.demo.Repository;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import japlearn.demo.Entity.GuidedPhraseDailyUsage;
public interface GuidedPhraseDailyUsageRepository extends MongoRepository<GuidedPhraseDailyUsage,String>{
    Optional<GuidedPhraseDailyUsage> findByEmailIgnoreCaseAndUsageDate(String email,String usageDate);
}
