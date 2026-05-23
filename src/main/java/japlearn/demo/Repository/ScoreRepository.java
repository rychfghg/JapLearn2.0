package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.Score;

public interface ScoreRepository extends MongoRepository<Score, String> {
    List<Score> findByDate(String date);
}
