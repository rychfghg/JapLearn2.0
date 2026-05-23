package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.DatabankLessonPage;

public interface DatabankLessonPageRepository extends MongoRepository<DatabankLessonPage, String>{
	List<DatabankLessonPage> findByLessonId(String LessonId);
	
	void deleteByLessonId(String lessonId);
}
