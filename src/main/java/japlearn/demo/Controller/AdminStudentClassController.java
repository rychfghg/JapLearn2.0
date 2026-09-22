package japlearn.demo.Controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.Classes;
import japlearn.demo.Entity.Student;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.ClassesRepository;
import japlearn.demo.Repository.StudentRepository;
import japlearn.demo.Repository.UserRepository;

/**
 * Lets the admin portal see and change which class each student belongs to.
 *
 * Every route under /api/admin/ is restricted to administrator sessions by
 * PortalAuthorizationFilter.
 */
@RestController
@RequestMapping("/api/admin/student-classes")
public class AdminStudentClassController {

    private final ClassesRepository classes;
    private final StudentRepository students;
    private final UserRepository users;

    public AdminStudentClassController(ClassesRepository classes, StudentRepository students, UserRepository users) {
        this.classes = classes;
        this.students = students;
        this.users = users;
    }

    /** Every class, plus each student's current class code keyed by email. */
    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, String>> classList = classes.findAll().stream()
                .filter(item -> item.getClassCodes() != null && !item.getClassCodes().isBlank())
                .map(item -> {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("classCode", item.getClassCodes());
                    row.put("classTitle", item.getClassTitle() == null ? "" : item.getClassTitle());
                    row.put("ownerTeacherEmail", item.getOwnerTeacherEmail() == null ? "" : item.getOwnerTeacherEmail());
                    return row;
                })
                .toList();

        Map<String, String> assignments = new LinkedHashMap<>();
        for (Student student : students.findAll()) {
            if (student.getEmail() != null && student.getClassCode() != null && !student.getClassCode().isBlank()) {
                assignments.put(student.getEmail().trim().toLowerCase(Locale.ROOT), student.getClassCode());
            }
        }

        return Map.of("classes", classList, "assignments", assignments);
    }

    /** Assigns a student to a class, or removes them from their class when the code is blank. */
    @PutMapping("/{userId}")
    public ResponseEntity<?> assign(@PathVariable String userId, @RequestBody Map<String, String> request) {
        User user = users.findById(userId).orElse(null);
        if (user == null || !"student".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Student account not found."));
        }

        String classCode = request == null || request.get("classCode") == null ? "" : request.get("classCode").trim();
        Student student = students.findByEmail(user.getEmail());

        if (classCode.isEmpty()) {
            // Remove the student from their class without touching the account itself.
            if (student != null) {
                student.setClassCode(null);
                students.save(student);
            }
            return ResponseEntity.ok(Map.of("classCode", ""));
        }

        Optional<Classes> target = classes.findByClassCodes(classCode);
        if (target.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "That class code does not exist."));
        }

        if (student == null) {
            // Same record the app creates when a student joins a class themselves.
            student = new Student(user, classCode);
        } else {
            student.setClassCode(classCode);
            student.setFname(user.getFname());
            student.setLname(user.getLname());
        }
        students.save(student);
        return ResponseEntity.ok(Map.of("classCode", classCode));
    }
}
