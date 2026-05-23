package japlearn.demo.Service;
 
import java.util.List;
import java.util.Optional;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
 
import japlearn.demo.Entity.Classes;
import japlearn.demo.Entity.Student;
import japlearn.demo.Entity.User;
import japlearn.demo.Repository.ClassesRepository;
import japlearn.demo.Repository.StudentRepository;
import japlearn.demo.Repository.UserRepository;
 
@Service
public class StudentService {
 
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private final StudentRepository studentRepository;
    @Autowired
    private ClassesRepository classesRepository;
 
    private final BCryptPasswordEncoder passwordEncoder;
 
    @Autowired
    private MongoTemplate mongoTemplate;
 
    @Autowired
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
 
    public boolean joinClassCodeByEmail(String email, String classCode) {
        Optional<Classes> existingClass = classesRepository.findByClassCodes(classCode);
        User user = userRepository.findByEmail(email);
    
        if (existingClass.isPresent() && user != null) {
            Student student = new Student(user, classCode); // Using the new constructor
            studentRepository.save(student);
            return true;
        }
        return false;
    }
 
    public List<Student> getStudentsByClassCode(String classCode) {
        return studentRepository.findByClassCode(classCode);
    }
    
    public Student getStudentByEmail(String email) {
    	return studentRepository.findByEmail(email);
    }
 
    public Student verifyCredentials(String email, String password) {
        Student student = studentRepository.findByEmail(email);
        if (student != null && passwordEncoder.matches(password, student.getPassword())) {
            return student;
        } else {
            return null;
        }
    }
 
    public boolean removeStudentByFullName(String classCode, String fname, String lname) {
        Query query = new Query(Criteria.where("classCode").is(classCode)
                                       .and("fname").is(fname)
                                       .and("lname").is(lname));
        Student student = mongoTemplate.findOne(query, Student.class);
        if (student != null) {
            studentRepository.delete(student);
            return true;
        }
        return false;
    }
}
 