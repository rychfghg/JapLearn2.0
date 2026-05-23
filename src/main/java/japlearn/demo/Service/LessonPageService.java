package japlearn.demo.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import japlearn.demo.Entity.LessonPage;
import japlearn.demo.Repository.LessonPageRepository;

@Service
public class LessonPageService {

	@Autowired
	LessonPageRepository lessonpagerepository;
	
	public LessonPage addLessonPage(LessonPage lessonPage) {
		return lessonpagerepository.save(lessonPage);	
	}
	
	public List<LessonPage> getAllLessonPage(String lessonId){
		return lessonpagerepository.findByLessonId(lessonId);
	}
	
	// Method to use when deleting all Lesson pages when a lesson is deleted
	public void deleteAllLessonPage(String lessonId) {
		lessonpagerepository.deleteByLessonId(lessonId);
	}
	
	// Method to use when deleting a lesson page
	public void deleteLessonPage(String pageId) {
		lessonpagerepository.deleteById(pageId);
	}
	
	public LessonPage updateLessonPage(String pageId, LessonPage newLessonPage) {
		Optional<LessonPage> fetchedlessonPage = lessonpagerepository.findById(pageId);
		
		if(fetchedlessonPage.isPresent()) {
			LessonPage lessonPage = fetchedlessonPage.get();
			
			lessonPage.setPage_title(newLessonPage.getPage_title());
			
			return lessonpagerepository.save(lessonPage);
		} else {
			throw new NoSuchElementException("LessonPage not found with ID: "+ pageId);
		}
	}
}
