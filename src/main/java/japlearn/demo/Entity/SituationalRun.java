package japlearn.demo.Entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "situational_runs")
@CompoundIndex(name = "email_game_unique", def = "{'email': 1, 'gameType': 1}", unique = true)
public class SituationalRun {
    @Id
    private String id;
    private String email;
    private String gameType;
    private int questionIndex;
    private int correctCount;
    private int easyMistakes;
    private int hardMistakes;
    private int hintsUsed;
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public int getQuestionIndex() { return questionIndex; }
    public void setQuestionIndex(int questionIndex) { this.questionIndex = questionIndex; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public int getEasyMistakes() { return easyMistakes; }
    public void setEasyMistakes(int easyMistakes) { this.easyMistakes = easyMistakes; }
    public int getHardMistakes() { return hardMistakes; }
    public void setHardMistakes(int hardMistakes) { this.hardMistakes = hardMistakes; }
    public int getHintsUsed() { return hintsUsed; }
    public void setHintsUsed(int hintsUsed) { this.hintsUsed = hintsUsed; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
