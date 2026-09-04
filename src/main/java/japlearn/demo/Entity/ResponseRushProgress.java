package japlearn.demo.Entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "response_rush_progress")
public class ResponseRushProgress {
    @Id
    private String id;
    @Indexed(unique = true)
    private String email;
    private String currentNodeId;
    private List<Map<String, Object>> answers = new ArrayList<>();
    private int timeLeft;
    private int bestPercentage;
    private boolean completed;
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }
    public List<Map<String, Object>> getAnswers() { return answers; }
    public void setAnswers(List<Map<String, Object>> answers) { this.answers = answers == null ? new ArrayList<>() : answers; }
    public int getTimeLeft() { return timeLeft; }
    public void setTimeLeft(int timeLeft) { this.timeLeft = timeLeft; }
    public int getBestPercentage() { return bestPercentage; }
    public void setBestPercentage(int bestPercentage) { this.bestPercentage = bestPercentage; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
