package japlearn.demo.Controller;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import japlearn.demo.Entity.*;
import japlearn.demo.Service.*;

@RestController @RequestMapping("/api/student/lessons")
public class StudentLessonController {
 private final StudentAuthorizationService auth; private final TeacherLessonService service;
 public StudentLessonController(StudentAuthorizationService a,TeacherLessonService s){auth=a;service=s;}
 public record Submission(String email,List<String> answers){}
 @GetMapping public List<Lesson> list(@RequestParam String email,@RequestHeader("X-Student-Token")String token){String e=auth.requireStudent(email,token);return service.studentList(e).stream().map(this::safe).toList();}
 @GetMapping("/{id}") public Lesson detail(@RequestParam String email,@RequestHeader("X-Student-Token")String token,@PathVariable String id){return safe(service.studentLesson(auth.requireStudent(email,token),id));}
 @GetMapping(value="/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE) public ResponseEntity<Resource> pdf(@RequestParam String email,@RequestHeader("X-Student-Token")String token,@PathVariable String id){Resource resource=service.studentPdf(auth.requireStudent(email,token),id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header("Content-Disposition","inline; filename=lesson.pdf").body(resource);}
 @GetMapping(value="/{id}/pages/{page}",produces=MediaType.IMAGE_PNG_VALUE) public ResponseEntity<byte[]> page(@RequestParam String email,@RequestHeader("X-Student-Token")String token,@PathVariable String id,@PathVariable int page){return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(service.studentPdfPage(auth.requireStudent(email,token),id,page));}
 @PostMapping("/{id}/submit") public LessonQuizAttempt submit(@RequestHeader("X-Student-Token")String token,@PathVariable String id,@RequestBody Submission body){return service.submit(auth.requireStudent(body.email(),token),id,body.answers());}
 private Lesson safe(Lesson source){Lesson copy=new Lesson();copy.setId(source.getId());copy.setClassId(source.getClassId());copy.setClassIds(source.getClassIds());copy.setLesson_title(source.getLesson_title());copy.setLesson_type(source.getLesson_type());copy.setLesson_description(source.getLesson_description());copy.setSourceFileName(source.getSourceFileName());copy.setPdfPageCount(source.getPdfPageCount());copy.setQuizTimerSeconds(source.getQuizTimerSeconds());copy.setPublished(source.isPublished());copy.setCreatedAt(source.getCreatedAt());copy.setQuiz(source.getQuiz().stream().map(q->{Lesson.QuizQuestion v=new Lesson.QuizQuestion();v.setPrompt(q.getPrompt());v.setType(q.getType());v.setChoices(q.getChoices());return v;}).toList());return copy;}
}
