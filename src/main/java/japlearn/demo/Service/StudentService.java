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

    public boolean joinClassCodeByTeacher(String teacherEmail, String email, String classCode) {
        if (!teacherOwnsClass(teacherEmail, classCode)) return false;
        return joinClassCodeByEmail(email, classCode);
    }
 
    public List<Student> getStudentsByClassCode(String classCode) {
        return studentRepository.findByClassCode(classCode);
    }

    public List<Student> getStudentsByClassCodeForTeacher(String teacherEmail, String classCode) {
        if (!teacherOwnsClass(teacherEmail, classCode)) return List.of();
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

    public boolean removeStudentByTeacher(String teacherEmail, String classCode, String fname, String lname) {
        if (!teacherOwnsClass(teacherEmail, classCode)) return false;
        return removeStudentByFullName(classCode, fname, lname);
    }

    public List<Student> getAllStudents() {
    return studentRepository.findAll();
}

    public List<Student> getStudentsForTeacher(String teacherEmail) {
        List<String> classCodes = classesRepository.findAllByOwnerTeacherEmailIgnoreCase(teacherEmail).stream()
                .map(Classes::getClassCodes)
                .toList();
        return classCodes.isEmpty() ? List.of() : studentRepository.findByClassCodeIn(classCodes);
    }

    private boolean teacherOwnsClass(String teacherEmail, String classCode) {
        return teacherEmail != null && classCode != null
                && classesRepository.findByClassCodesAndOwnerTeacherEmailIgnoreCase(classCode, teacherEmail).isPresent();
    }
}
