package japlearn.demo.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quackman_content")
public class QuackmanContent {

    @Id
    private String id;
    private String romajiWord;
    private String description;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRomajiWord() {
        return romajiWord;
    }

    public void setRomajiWord(String romajiWord) {
        this.romajiWord = romajiWord;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
