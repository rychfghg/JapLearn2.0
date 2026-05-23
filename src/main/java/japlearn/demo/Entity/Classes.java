package japlearn.demo.Entity;
 
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
 
@Document(collection = "classcodes")
public class Classes {
    @Id
    private String id; // MongoDB's default ID
    private String classCodes; // This is your custom class code field
 
    public Classes() {
    }
 
    // Getter and setter for id
    public String getId() {
        return id;
    }
 
    public void setId(String id) {
        this.id = id;
    }
 
    // Getter and setter for classCodes
    public String getClassCodes() {
        return classCodes;
    }
 
    public void setClassCodes(String classCodes) {
        this.classCodes = classCodes;
    }
}
 