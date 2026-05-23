package japlearn.demo.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.QuackamoleContent;

public interface QuackamoleContentRepository extends MongoRepository<QuackamoleContent, String> {
}
