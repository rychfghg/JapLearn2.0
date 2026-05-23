package japlearn.demo.Entity;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "students")


public class Student extends User {


    private String classCode;
    

    public Student() {
        super();
    }

    public Student(String id, String fname, String lname, String email, String password, String role,  String classCode) {
        super(id, fname, lname, email, password, role); 
        this.classCode = "";
        
    }

    public Student(User user, String classCode) {
        super(user.getId(), user.getFname(), user.getLname(), user.getEmail(), user.getPassword(), user.getRole());
        this.classCode = classCode;
        this.setEmailConfirmed(user.isEmailConfirmed()); 
        this.setApproved(user.isApproved()); 
    }
    

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    
    
}
