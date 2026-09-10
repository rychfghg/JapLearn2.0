package japlearn.demo.Service;
 
import java.util.List;
import java.util.Optional; // Import Optional
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // Import ResponseEntity
import org.springframework.stereotype.Service;
 
import japlearn.demo.Entity.Classes;
import japlearn.demo.Repository.ClassesRepository;
 
@Service
public class ClassesService {
 
    @Autowired
    private ClassesRepository classRepository;
 
    // Method to add class codes ensuring no duplicates
    public ResponseEntity<?> addClass(String classCode, String teacherEmail) {
        if (teacherEmail == null || teacherEmail.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Teacher account is required\"}");
        }
        // Check if class code already exists
        Optional<Classes> existingClass = classRepository.findByClassCodes(classCode);
        if (existingClass.isPresent()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Class code already exists\"}");
        }
 
        Classes newClassEntity = new Classes();
        newClassEntity.setClassCodes(classCode);
        newClassEntity.setOwnerTeacherEmail(teacherEmail.trim().toLowerCase());
        classRepository.save(newClassEntity);
        return ResponseEntity.ok(newClassEntity);
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
