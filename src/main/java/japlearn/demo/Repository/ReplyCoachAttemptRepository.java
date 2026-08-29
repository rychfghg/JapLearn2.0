package japlearn.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.ReplyCoachAttempt;

public interface ReplyCoachAttemptRepository extends MongoRepository<ReplyCoachAttempt, String> {
    List<ReplyCoachAttempt> findByEmailIgnoreCaseOrderByUpdatedAtDesc(String email);
    List<ReplyCoachAttempt> findByEmailIgnoreCaseAndStatusIgnoreCaseOrderByUpdatedAtDesc(String email, String status);
    Optional<ReplyCoachAttempt> findTopByEmailIgnoreCaseAndChapterIdOrderByAttemptNumberDesc(String email, String chapterId);
    Optional<ReplyCoachAttempt> findTopByEmailIgnoreCaseAndChapterIdAndStatusIgnoreCaseOrderByUpdatedAtDesc(String email, String chapterId, String status);
    long countByEmailIgnoreCaseAndChapterId(String email, String chapterId);
}
