package japlearn.demo.Controller;

import java.util.List;
import java.util.Optional;

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

import japlearn.demo.Entity.DatabankLesson;
import japlearn.demo.Service.DatabankLessonService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/DatabankLesson")
public class DatabankLessonController {

	@Autowired
	DatabankLessonService databanklessonservice;
	
	@PostMapping("/createDatabankLesson")
	public DatabankLesson createLesson(@RequestBody DatabankLesson lesson) {
		return databanklessonservice.createLesson(lesson);
	}
	
	@GetMapping("/getDatabankLessonByClass/{classId}")
	public List<DatabankLesson> getLessonsByClass(@PathVariable String classId){
		return databanklessonservice.getLessonsByClass(classId);
	}
	
	@GetMapping("/getAllDatabankLessons")
	public List<DatabankLesson> getAllDatabankLessons() {
		return databanklessonservice.getAllDatabankLessons();
	}
	
	@GetMapping("/getDatabankLesson/{lessonId}")
	public Optional<DatabankLesson> getDatabankLesson(@PathVariable String lessonId) {
		return databanklessonservice.getDatabankLesson(lessonId);
	}
	
	@DeleteMapping("/deleteDatabankLesson")
	public void deleteLesson(@RequestParam String classId) {
		databanklessonservice.deleteLesson(classId); 
	}
	
	@PutMapping("/editDatabankLesson/{lessonId}")
	public DatabankLesson editLesson(@PathVariable String lessonId, @RequestBody DatabankLesson newLesson) {
		return databanklessonservice.updateLesson(lessonId, newLesson);
	}
}
