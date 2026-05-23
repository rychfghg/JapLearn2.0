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
    public ResponseEntity<?> addClass(String classCode) {
        // Check if class code already exists
        Optional<Classes> existingClass = classRepository.findByClassCodes(classCode);
        if (existingClass.isPresent()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Class code already exists\"}");
        }
 
        Classes newClassEntity = new Classes();
        newClassEntity.setClassCodes(classCode);
        classRepository.save(newClassEntity);
        return ResponseEntity.ok(newClassEntity);
    }
 
    // Method to remove clSass codes handling possible multiple entries
    public void removeClass(String classCode) {
        List<Classes> classesToRemove = classRepository.findAllByClassCodes(classCode);
        if (!classesToRemove.isEmpty()) {
            classRepository.deleteAll(classesToRemove);
        } else {
            throw new RuntimeException("Class not found");
        }
    }
 
    // Retrieve all class codes
    public List<Classes> getAllClasses() {
        return classRepository.findAll();
    }
}