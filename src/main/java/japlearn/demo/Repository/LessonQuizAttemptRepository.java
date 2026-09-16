package japlearn.demo.Repository;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import japlearn.demo.Entity.LessonQuizAttempt;
public interface LessonQuizAttemptRepository extends MongoRepository<LessonQuizAttempt,String>{
 List<LessonQuizAttempt> findByLessonIdOrderBySubmittedAtDesc(String lessonId);
 List<LessonQuizAttempt> findByStudentEmailIgnoreCaseOrderBySubmittedAtDesc(String studentEmail);
}
