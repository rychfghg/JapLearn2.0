package japlearn.demo.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import japlearn.demo.Entity.DatabankLessonPage;
import japlearn.demo.Repository.DatabankLessonPageRepository;

@Service
public class DatabankLessonPageService {

	@Autowired
	DatabankLessonPageRepository databanklessonpagerepository;
	
	public DatabankLessonPage addLessonPage(DatabankLessonPage lessonPage) {
		return databanklessonpagerepository.save(lessonPage);	
	}
	
	public List<DatabankLessonPage> getAllLessonPage(String lessonId){
		return databanklessonpagerepository.findByLessonId(lessonId);
	}
	
	// Method to use when deleting all Lesson pages when a lesson is deleted
	public void deleteAllLessonPage(String lessonId) {
		databanklessonpagerepository.deleteByLessonId(lessonId);
	}
	
	// Method to use when deleting a lesson page
	public void deleteLessonPage(String pageId) {
		databanklessonpagerepository.deleteById(pageId);
	}
	
	public DatabankLessonPage updateLessonPage(String pageId, DatabankLessonPage newLessonPage) {
		Optional<DatabankLessonPage> fetchedlessonPage = databanklessonpagerepository.findById(pageId);
		
		if(fetchedlessonPage.isPresent()) {
			DatabankLessonPage lessonPage = fetchedlessonPage.get();
			
			lessonPage.setPage_title(newLessonPage.getPage_title());
			
			return databanklessonpagerepository.save(lessonPage);
		} else {
			throw new NoSuchElementException("LessonPage not found with ID: "+ pageId);
		}
	}
}
