package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.SituationalQuestion;

public interface SituationalQuestionRepository extends MongoRepository<SituationalQuestion, String> {
    List<SituationalQuestion> findByGameTypeIgnoreCaseOrderByOrderAsc(String gameType);
    List<SituationalQuestion> findByGameTypeIgnoreCaseAndActiveTrueOrderByOrderAsc(String gameType);
}
