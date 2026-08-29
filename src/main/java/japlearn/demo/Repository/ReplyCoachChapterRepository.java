package japlearn.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.ReplyCoachChapter;

public interface ReplyCoachChapterRepository extends MongoRepository<ReplyCoachChapter, String> {
    List<ReplyCoachChapter> findByStatusIgnoreCaseOrderByOrderAsc(String status);
    Optional<ReplyCoachChapter> findByTitleIgnoreCase(String title);
}
