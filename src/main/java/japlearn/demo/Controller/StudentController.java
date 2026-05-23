package japlearn.demo.Controller;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.Student;
import japlearn.demo.Service.StudentService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/students")


public class StudentController {

    private final StudentService studentService;
    



    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }


    @PostMapping("/joinClass")
    public ResponseEntity<String> joinClass(@RequestParam String email, @RequestParam String classCode) {
    boolean success = studentService.joinClassCodeByEmail(email, classCode);

    if (success) {
        return ResponseEntity.ok("Successfully joined the class.");
    } else {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Class code or student with the given email not found.");
    }
}

    @GetMapping("/getByClassCode")
    public ResponseEntity<List<Student>> getStudentsByClassCode(@RequestParam String classCode) {
        List<Student> students = studentService.getStudentsByClassCode(classCode);
        return ResponseEntity.ok(students);
    }
    
    @GetMapping("/getStudentByEmail")
    public Student getStudentByEmail(@RequestParam String email) {
    	return studentService.getStudentByEmail(email);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        Student student = studentService.verifyCredentials(email, password);
    
        if (student != null) {
            return ResponseEntity.ok(student);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
        }
    }

    @DeleteMapping("/removeStudent")
    public ResponseEntity<String> removeStudent(@RequestBody Map<String, String> payload) {
        String classCode = payload.get("classCode");
        String fname = payload.get("name").split(" ")[0];
        String lname = payload.get("name").split(" ")[1];
        boolean success = studentService.removeStudentByFullName(classCode, fname, lname);
    
        if (success) {
            return ResponseEntity.ok("Successfully removed the student.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found.");
        }
    }
    
}