package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.QuackTalkSession;

public interface QuackTalkSessionRepository extends MongoRepository<QuackTalkSession, String> {
    List<QuackTalkSession> findByEmailIgnoreCaseOrderByPracticedAtDesc(String email);
    List<QuackTalkSession> findAllByOrderByPracticedAtDesc();
    List<QuackTalkSession> findByEmailIgnoreCaseAndRoomTypeAndStatus(String email, String roomType, String status);
    List<QuackTalkSession> findByRoomTypeAndStatus(String roomType, String status);
}
