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

import japlearn.demo.Entity.Lesson;
import japlearn.demo.Service.LessonService;

@RestController
@RequestMapping("/api/lesson")
public class LessonController {

	@Autowired
	LessonService lessonservice;
	
	@PostMapping("/createLesson")
	public Lesson createLesson(@RequestBody Lesson lesson) {
		return lessonservice.createLesson(lesson);
	}
	
	@GetMapping("/getLessonByClass/{classId}")
	public List<Lesson> getLessonsByClass(@PathVariable String classId){
		// Students load this by class code; never include the teacher's email in the public response.
		List<Lesson> lessons = lessonservice.getLessonsByClass(classId);
		lessons.forEach((lesson) -> lesson.setOwnerTeacherEmail(null));
		return lessons;
	}
	
	@DeleteMapping("/deleteLesson")
	public void deleteLesson(@RequestParam String classId) {
		lessonservice.deleteLesson(classId); 
	}
	
	@PutMapping("/editLesson/{lessonId}")
	public Lesson editLesson(@PathVariable String lessonId, @RequestBody Lesson newLesson) {
		return lessonservice.updateLesson(lessonId, newLesson);
	}
}
