package japlearn.demo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.Classes;

public interface ClassesRepository extends MongoRepository<Classes, String> {
    // Change the method to return Optional for safer handling
    Optional<Classes> findByClassCodes(String classCodes);
 
    // Add method to find all entries by class code
    List<Classes> findAllByClassCodes(String classCodes);
}
 
