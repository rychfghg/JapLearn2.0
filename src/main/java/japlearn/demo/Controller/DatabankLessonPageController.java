package japlearn.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.DatabankLessonPage;
import japlearn.demo.Service.DatabankLessonPageService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/DatabankLessonPage")
public class DatabankLessonPageController {

	@Autowired
	DatabankLessonPageService databanklessonpageservice;
	
	@PostMapping ("/addDatabankLessonPage")
	public DatabankLessonPage addLessonPage(@RequestBody DatabankLessonPage lessonPage) {
		return databanklessonpageservice.addLessonPage(lessonPage);
	}
	
	@GetMapping ("/getAllDatabankLessonPage/{lessonId}")
	public List<DatabankLessonPage> getAllLessonPage(@PathVariable String lessonId) {
		return databanklessonpageservice.getAllLessonPage(lessonId);
	}
	
	@DeleteMapping ("/deleteAllDatabanklessonPage")
	public void deleteAllLessonPage(@RequestParam String lessonId) {
		databanklessonpageservice.deleteAllLessonPage(lessonId);
	}
	
	@DeleteMapping ("/deleteDatabankLessonPage")
	public void deleteLessonPage(@RequestParam String pageId) {
		databanklessonpageservice.deleteLessonPage(pageId);
	}
	
	@PutMapping ("/updateDatabankLessonPage/{lessonPageId}")
	public DatabankLessonPage updateLessonpage(@PathVariable String lessonPageId,@RequestBody DatabankLessonPage lessonpage) {
		return databanklessonpageservice.updateLessonPage(lessonPageId, lessonpage);
	}
}
