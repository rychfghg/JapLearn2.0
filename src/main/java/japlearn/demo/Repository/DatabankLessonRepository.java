package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.DatabankLesson;


public interface DatabankLessonRepository extends MongoRepository<DatabankLesson, String>{
	List<DatabankLesson> findByClassId(String classId);
}
