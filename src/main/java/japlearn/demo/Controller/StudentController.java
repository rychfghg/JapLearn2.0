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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.Student;
import japlearn.demo.Service.StudentService;
import japlearn.demo.Service.TeacherAuthorizationService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/students")


public class StudentController {

    private final StudentService studentService;
    private final TeacherAuthorizationService teacherAuthorization;
    



    @Autowired
    public StudentController(StudentService studentService, TeacherAuthorizationService teacherAuthorization) {
        this.studentService = studentService;
        this.teacherAuthorization = teacherAuthorization;
    }


    @PostMapping("/joinClass")
    public ResponseEntity<String> joinClass(@RequestParam String email, @RequestParam String classCode,
            @RequestParam(required = false) String teacherEmail,
            @RequestHeader(value = "X-Teacher-Token", required = false) String sessionToken) {
    boolean success = teacherEmail == null
            ? studentService.joinClassCodeByEmail(email, classCode)
            : studentService.joinClassCodeByTeacher(teacherAuthorization.requireTeacher(teacherEmail, sessionToken), email, classCode);

    if (success) {
        return ResponseEntity.ok("Successfully joined the class.");
    } else {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Class code or student with the given email not found.");
    }
}

    @GetMapping("/getByClassCode")
    public ResponseEntity<List<Student>> getStudentsByClassCode(@RequestParam String classCode,
            @RequestParam(required = false) String teacherEmail,
            @RequestHeader(value = "X-Teacher-Token", required = false) String sessionToken) {
        List<Student> students = teacherEmail == null
                ? studentService.getStudentsByClassCode(classCode)
                : studentService.getStudentsByClassCodeForTeacher(teacherAuthorization.requireTeacher(teacherEmail, sessionToken), classCode);
        return ResponseEntity.ok(withoutPasswords(students));
    }
    
    // @GetMapping("/getStudentByEmail")
    // public Student getStudentByEmail(@RequestParam String email) {
    // 	return studentService.getStudentByEmail(email);
    // }
@GetMapping("/getStudentByEmail")
public ResponseEntity<?> getStudentByEmail(@RequestParam String email) {
    Student student = studentService.getStudentByEmail(email);

    if (student == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Student not found"));
    }

    student.setPassword(null);
    return ResponseEntity.ok(student);

    
}
@GetMapping("/getAllStudents")
public ResponseEntity<List<Student>> getAllStudents(@RequestParam(required = false) String teacherEmail,
        @RequestHeader(value = "X-Teacher-Token", required = false) String sessionToken) {
    List<Student> students = teacherEmail == null
            ? studentService.getAllStudents()
            : studentService.getStudentsForTeacher(teacherAuthorization.requireTeacher(teacherEmail, sessionToken));
    return ResponseEntity.ok(withoutPasswords(students));

}
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        Student student = studentService.verifyCredentials(email, password);
    
        if (student != null) {
            student.setPassword(null);
            return ResponseEntity.ok(student);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
        }
    }

    @DeleteMapping("/removeStudent")
    public ResponseEntity<String> removeStudent(@RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-Teacher-Token", required = false) String sessionToken) {
        String classCode = payload.get("classCode");
        String fname = payload.get("name").split(" ")[0];
        String lname = payload.get("name").split(" ")[1];
        String teacherEmail = payload.get("teacherEmail");
        boolean success = teacherEmail == null
                ? studentService.removeStudentByFullName(classCode, fname, lname)
                : studentService.removeStudentByTeacher(teacherAuthorization.requireTeacher(teacherEmail, sessionToken), classCode, fname, lname);
    
        if (success) {
            return ResponseEntity.ok("Successfully removed the student.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found.");
        }
    }

    private List<Student> withoutPasswords(List<Student> students) {
        students.forEach(student -> student.setPassword(null));
        return students;
    }
    
}
