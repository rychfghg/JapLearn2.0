package japlearn.demo.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import japlearn.demo.Entity.Lesson;

public interface LessonRepository extends MongoRepository<Lesson, String>{
	List<Lesson> findByClassId(String classId);
}
