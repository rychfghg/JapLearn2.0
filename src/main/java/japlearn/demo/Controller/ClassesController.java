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
import org.springframework.web.bind.annotation.RestController;
 
import japlearn.demo.Entity.Classes;
import japlearn.demo.Service.ClassesService;
 
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/classes")
public class ClassesController {
    private final ClassesService classService;
 
    @Autowired
    public ClassesController(ClassesService classService) {
        this.classService = classService;
    }
 
    @PostMapping("/addClass")
    public ResponseEntity<?> addClassCode(@RequestBody Classes newClassEntity) {
        return classService.addClass(newClassEntity.getClassCodes());
    }
 
   @DeleteMapping("/removeClass")
    public ResponseEntity<?> removeClassCode(@RequestParam String classCode) {
    try {
        classService.removeClass(classCode);
        return ResponseEntity.ok().body("{\"message\": \"Class removed successfully\"}");
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
     }
 }
 
    @GetMapping("/getAllClasses")
    public List<Classes> getAllClassCodes() {
        return classService.getAllClasses();
    }
}