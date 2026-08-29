package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.ReplyCoachChapter;

public interface ReplyCoachChapterRepository extends MongoRepository<ReplyCoachChapter, String> {
    List<ReplyCoachChapter> findByStatusIgnoreCaseOrderByOrderAsc(String status);
}
