package japlearn.demo.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import japlearn.demo.Entity.StudentProgress;

@Repository
public interface StudentProgressRepository extends MongoRepository<StudentProgress, String> {
    
    // Custom method to find progress by student's email
    Optional<StudentProgress> findByEmail(String email);
}
