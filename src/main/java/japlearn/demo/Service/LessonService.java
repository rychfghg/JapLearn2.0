package japlearn.demo.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import japlearn.demo.Entity.Lesson;
import japlearn.demo.Entity.LessonPage;
import japlearn.demo.Repository.LessonRepository;

@Service
public class LessonService {

	@Autowired
	LessonRepository lessonrepository;
	
	public Lesson createLesson(Lesson lesson) {
		return lessonrepository.save(lesson);
	}
	
	public List<Lesson> getLessonsByClass(String classId) {
		return lessonrepository.findByClassId(classId);
	}
	
	public void deleteLesson(String classId) {
		lessonrepository.deleteById(classId);
	}
	
	public Lesson updateLesson(String lessonId, Lesson newLesson) {
		Optional<Lesson> fetchedLesson = lessonrepository.findById(lessonId);
		
		if(fetchedLesson.isPresent()) {
			Lesson lesson = fetchedLesson.get();
			lesson.setLesson_title(newLesson.getLesson_title());
			lesson.setLesson_type(newLesson.getLesson_type());
			
			return lessonrepository.save(lesson);
		} else {
			throw new NoSuchElementException("Lesson not found with ID: " + lessonId);
		}
	}
}
