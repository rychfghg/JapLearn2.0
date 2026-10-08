package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.SmartRefactoringPriority;

public interface SmartRefactoringPriorityRepository extends MongoRepository<SmartRefactoringPriority, String> {
    List<SmartRefactoringPriority> findAllByOrderByRankAscUpdatedAtAsc();
}
