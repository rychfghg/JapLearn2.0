package japlearn.demo.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.QuackmanContent;

public interface QuackmanContentRepository extends MongoRepository<QuackmanContent, String> {
}
