package japlearn.demo.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import japlearn.demo.Entity.DatabankLesson;
import japlearn.demo.Repository.DatabankLessonRepository;

@Service
public class DatabankLessonService {

	@Autowired
	DatabankLessonRepository databankLessonrepository;
	
	public DatabankLesson createLesson(DatabankLesson lesson) {
		return databankLessonrepository.save(lesson);
	}
	
	public List<DatabankLesson> getLessonsByClass(String classId) {
		return databankLessonrepository.findByClassId(classId);
	}
	
	public List<DatabankLesson> getAllDatabankLessons() {
		return databankLessonrepository.findAll();
	}
	
	public Optional<DatabankLesson> getDatabankLesson(String lessonId) {
		return databankLessonrepository.findById(lessonId);
	}
	
	public void deleteLesson(String classId) {
		databankLessonrepository.deleteById(classId);
	}
	
	public DatabankLesson updateLesson(String lessonId, DatabankLesson newLesson) {
		Optional<DatabankLesson> fetchedLesson = databankLessonrepository.findById(lessonId);
		
		if(fetchedLesson.isPresent()) {
			DatabankLesson lesson = fetchedLesson.get();
			lesson.setLesson_title(newLesson.getLesson_title());
			lesson.setLesson_type(newLesson.getLesson_type());
			
			return databankLessonrepository.save(lesson);
		} else {
			throw new NoSuchElementException("Lesson not found with ID: " + lessonId);
		}
	}
}
