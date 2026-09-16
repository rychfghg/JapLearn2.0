package japlearn.demo.Controller;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import japlearn.demo.Entity.*;
import japlearn.demo.Service.*;

@RestController @RequestMapping("/api/teacher/lessons")
public class TeacherLessonController {
 private final TeacherAuthorizationService auth; private final TeacherLessonService service; private final ObjectMapper mapper;
 public TeacherLessonController(TeacherAuthorizationService a,TeacherLessonService s,ObjectMapper m){auth=a;service=s;mapper=m;}
 @GetMapping public List<Lesson> list(@RequestParam String teacherEmail,@RequestHeader("X-Teacher-Token")String token){return service.teacherList(auth.requireTeacher(teacherEmail,token));}
 @PostMapping(consumes="multipart/form-data") public Lesson create(@RequestParam String teacherEmail,@RequestHeader("X-Teacher-Token")String token,@RequestPart("lesson")String json,@RequestPart(value="pdf",required=false)MultipartFile pdf)throws Exception{return service.create(auth.requireTeacher(teacherEmail,token),mapper.readValue(json,Lesson.class),pdf);}
 @GetMapping("/{id}") public Lesson detail(@RequestParam String teacherEmail,@RequestHeader("X-Teacher-Token")String token,@PathVariable String id){return service.owned(auth.requireTeacher(teacherEmail,token),id);}
 @GetMapping("/{id}/results") public List<LessonQuizAttempt> results(@RequestParam String teacherEmail,@RequestHeader("X-Teacher-Token")String token,@PathVariable String id){return service.results(auth.requireTeacher(teacherEmail,token),id);}
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@RequestParam String teacherEmail,@RequestHeader("X-Teacher-Token")String token,@PathVariable String id){service.delete(auth.requireTeacher(teacherEmail,token),id);return ResponseEntity.noContent().build();}
}
