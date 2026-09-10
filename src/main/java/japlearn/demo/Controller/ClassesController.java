package japlearn.demo.Controller;
 
import java.util.List;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
 
import japlearn.demo.Entity.Classes;
import japlearn.demo.Service.ClassesService;
import japlearn.demo.Service.TeacherAuthorizationService;
 
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/classes")
public class ClassesController {
    private final ClassesService classService;
    private final TeacherAuthorizationService teacherAuthorization;
 
    @Autowired
    public ClassesController(ClassesService classService, TeacherAuthorizationService teacherAuthorization) {
        this.classService = classService;
        this.teacherAuthorization = teacherAuthorization;
    }
 
    @PostMapping("/addClass")
    public ResponseEntity<?> addClassCode(@RequestBody Classes newClassEntity, @RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String sessionToken) {
        return classService.addClass(newClassEntity.getClassCodes(), teacherAuthorization.requireTeacher(teacherEmail, sessionToken));
    }
 
   @DeleteMapping("/removeClass")
    public ResponseEntity<?> removeClassCode(@RequestParam String classCode, @RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String sessionToken) {
    try {
        classService.removeClass(classCode, teacherAuthorization.requireTeacher(teacherEmail, sessionToken));
        return ResponseEntity.ok().body("{\"message\": \"Class removed successfully\"}");
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
     }
 }
 
    @GetMapping("/getAllClasses")
    public List<Classes> getAllClassCodes(@RequestParam String teacherEmail,
            @RequestHeader("X-Teacher-Token") String sessionToken) {
        return classService.getClassesForTeacher(teacherAuthorization.requireTeacher(teacherEmail, sessionToken));
    }
}
