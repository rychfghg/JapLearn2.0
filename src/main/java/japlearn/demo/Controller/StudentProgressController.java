package japlearn.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import japlearn.demo.Entity.StudentProgress;
import japlearn.demo.Service.StudentProgressService;
import japlearn.demo.Service.StudentService;
import japlearn.demo.Service.TeacherAuthorizationService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/progress")
public class StudentProgressController {

    @Autowired
    private StudentProgressService studentProgressService;

    @Autowired private StudentService studentService;
    @Autowired private TeacherAuthorizationService teacherAuthorization;

    @GetMapping
    public List<StudentProgress> getAllProgress() {
        return studentProgressService.getAllProgress();
    }

    @GetMapping("/teacher")
    public List<StudentProgress> getTeacherProgress(@RequestParam String teacherEmail,
            @org.springframework.web.bind.annotation.RequestHeader("X-Teacher-Token") String sessionToken) {
        String owner = teacherAuthorization.requireTeacher(teacherEmail, sessionToken);
        List<String> emails = studentService.getStudentsForTeacher(owner).stream()
                .map(student -> student.getEmail())
                .filter(email -> email != null && !email.isBlank())
                .toList();
        return studentProgressService.getProgressForStudents(emails);
    }

    // Endpoint to get progress for a student by email
    @GetMapping("/{email}")
    public ResponseEntity<StudentProgress> getProgress(@PathVariable String email) {
        try {
            StudentProgress progress = studentProgressService.getProgress(email);
            return new ResponseEntity<>(progress, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    // Endpoint to save progress (create new progress with initial settings)
    @PostMapping("/{email}")
    public StudentProgress saveProgress(@PathVariable String email) {
        return studentProgressService.saveProgress(email);
    }

    // Endpoint to update a single field for a student
    @PutMapping("/{email}/updateField")
    public StudentProgress updateSingleField(
            @PathVariable String email,
            @RequestParam String field,  // Name of the field to update
            @RequestParam boolean value) {  // New value for the field (true/false)
        return studentProgressService.updateSingleField(email, field, value);
    }

    // Endpoint to reset all progress for a student
    @DeleteMapping("/{email}")
    public StudentProgress resetProgress(@PathVariable String email) {
        return studentProgressService.resetProgress(email);
    }
}
