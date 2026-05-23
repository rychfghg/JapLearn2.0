package japlearn.demo.Entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quackamole_content")
public class QuackamoleContent {

    @Id
    private String id;
    private List<String> kana;
    private List<String> romaji;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getKana() {
        return kana;
    }

    public void setKana(List<String> kana) {
        this.kana = kana;
    }

    public List<String> getRomaji() {
        return romaji;
    }

    public void setRomaji(List<String> romaji) {
        this.romaji = romaji;
    }
}
