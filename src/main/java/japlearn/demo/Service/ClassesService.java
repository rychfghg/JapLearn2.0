package japlearn.demo.Service;
 
import java.util.List;
import java.security.SecureRandom;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // Import ResponseEntity
import org.springframework.stereotype.Service;
 
import japlearn.demo.Entity.Classes;
import japlearn.demo.Repository.ClassesRepository;
 
@Service
public class ClassesService {
    private static final String CLASS_CODE_PREFIX = "NIHONGGO";
    private static final int CLASS_CODE_NUMBER_RANGE = 10_000;
    private final SecureRandom secureRandom = new SecureRandom();
 
    @Autowired
    private ClassesRepository classRepository;
 
    // Method to add class codes ensuring no duplicates
    public ResponseEntity<?> addClass(String classTitle, String teacherEmail) {
        if (teacherEmail == null || teacherEmail.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Teacher account is required\"}");
        }
        if (classTitle == null || classTitle.trim().isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Classroom title is required\"}");
        }

        Classes newClassEntity = new Classes();
        newClassEntity.setClassCodes(generateUniqueCode());
        newClassEntity.setClassTitle(classTitle.trim());
        newClassEntity.setOwnerTeacherEmail(teacherEmail.trim().toLowerCase());
        classRepository.save(newClassEntity);
        return ResponseEntity.ok(newClassEntity);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 100; attempt++) {
            String candidate = CLASS_CODE_PREFIX + String.format("%04d", secureRandom.nextInt(CLASS_CODE_NUMBER_RANGE));
            if (classRepository.findByClassCodes(candidate).isEmpty()) return candidate;
        }
        throw new IllegalStateException("A unique classroom code could not be generated. Please try again.");
    }
 
    // Method to remove clSass codes handling possible multiple entries
    public void removeClass(String classCode, String teacherEmail) {
        Classes ownedClass = classRepository
                .findByClassCodesAndOwnerTeacherEmailIgnoreCase(classCode, teacherEmail)
                .orElseThrow(() -> new RuntimeException("Class not found in this teacher account"));
        classRepository.delete(ownedClass);
    }
 
    // Retrieve all class codes
    public List<Classes> getClassesForTeacher(String teacherEmail) {
        if (teacherEmail == null || teacherEmail.isBlank()) return List.of();
        return classRepository.findAllByOwnerTeacherEmailIgnoreCase(teacherEmail.trim());
    }

    public boolean teacherOwnsClass(String teacherEmail, String classCode) {
        return teacherEmail != null && classCode != null
                && classRepository.findByClassCodesAndOwnerTeacherEmailIgnoreCase(classCode, teacherEmail).isPresent();
    }
}
