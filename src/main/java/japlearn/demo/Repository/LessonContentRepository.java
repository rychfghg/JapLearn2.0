package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.LessonContent;

public interface LessonContentRepository extends MongoRepository<LessonContent,String>{
	List<LessonContent> findByLessonPageId(String lessonPageId);
	
	void deleteByLessonPageId(String lessonPageId);
}
