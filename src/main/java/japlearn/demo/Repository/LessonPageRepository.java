package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.LessonPage;

public interface LessonPageRepository extends MongoRepository<LessonPage, String>{
	List<LessonPage> findByLessonId(String LessonId);
	
	void deleteByLessonId(String lessonId);
}
