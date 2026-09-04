package japlearn.demo.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.ResponseRushProgress;

public interface ResponseRushProgressRepository extends MongoRepository<ResponseRushProgress, String> {
    Optional<ResponseRushProgress> findByEmailIgnoreCase(String email);
    void deleteByEmailIgnoreCase(String email);
}
