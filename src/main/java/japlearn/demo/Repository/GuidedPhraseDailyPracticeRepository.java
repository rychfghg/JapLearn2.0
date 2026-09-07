package japlearn.demo.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.GuidedPhraseDailyPractice;

public interface GuidedPhraseDailyPracticeRepository extends MongoRepository<GuidedPhraseDailyPractice, String> {
    Optional<GuidedPhraseDailyPractice> findByEmailIgnoreCaseAndPracticeDate(String email, String practiceDate);
}
