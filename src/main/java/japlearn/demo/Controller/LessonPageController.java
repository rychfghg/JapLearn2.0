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

import japlearn.demo.Entity.LessonPage;
import japlearn.demo.Service.LessonPageService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/lessonPage")
public class LessonPageController {
	
	@Autowired
	LessonPageService lessonpageservice;
	
	@PostMapping ("/addLessonPage")
	public LessonPage addLessonPage(@RequestBody LessonPage lessonPage) {
		return lessonpageservice.addLessonPage(lessonPage);
	}
	
	@GetMapping ("/getAllLessonPage/{lessonId}")
	public List<LessonPage> getAllLessonPage(@PathVariable String lessonId) {
		return lessonpageservice.getAllLessonPage(lessonId);
	}
	
	@DeleteMapping ("/deleteAlllessonPage")
	public void deleteAllLessonPage(@RequestParam String lessonId) {
		lessonpageservice.deleteAllLessonPage(lessonId);
	}
	
	@DeleteMapping ("/deleteLessonPage")
	public void deleteLessonPage(@RequestParam String pageId) {
		lessonpageservice.deleteLessonPage(pageId);
	}
	
	@PutMapping ("/updateLessonPage/{lessonPageId}")
	public LessonPage updateLessonpage(@PathVariable String lessonPageId,@RequestBody LessonPage lessonpage) {
		return lessonpageservice.updateLessonPage(lessonPageId, lessonpage);
	}
}
