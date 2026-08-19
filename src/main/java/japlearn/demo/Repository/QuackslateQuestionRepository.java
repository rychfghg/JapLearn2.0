package japlearn.demo.Repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import japlearn.demo.Entity.QuackslateQuestion;

public interface QuackslateQuestionRepository extends MongoRepository<QuackslateQuestion, String> {
    List<QuackslateQuestion> findByApprovedTrueAndSystemAvailableTrue();
}
