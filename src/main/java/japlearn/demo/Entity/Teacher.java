package japlearn.demo.Entity;

import java.util.List;

public class Teacher extends User {

    private List<String> manageClasses;

    public Teacher() {
        super();
    }
    
    public Teacher(String id, String fname, String lname, String email, String password, String role, List<String> manageClasses) {
        super(id, fname, lname, email, password, role);
        this.manageClasses = manageClasses;
    }
    
    public List<String> getManageClasses() {
        return manageClasses;
    }

    public void setManageClasses(List<String> manageClasses) {
        this.manageClasses = manageClasses;
    }
    
    
}
