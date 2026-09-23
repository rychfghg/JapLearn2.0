package japlearn.demo.Entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "scores")
@CompoundIndexes({
    @CompoundIndex(name = "score_email_attempt", def = "{'email':1,'clientAttemptId':1}"),
    @CompoundIndex(name = "score_session_sheet", def = "{'gameCode':1,'game':1,'mode':1}")
})
public class Score {
    @Id
    private String id; // MongoDB's default ID
    private String name;
    @Indexed
    private String email;
    private String date;
    private String game;
    private int score;
    private int maxScore;
    private int correctAnswers;
    private int totalQuestions;
    private boolean completed = true;
    private String mode;
    private String clientAttemptId;
    private String gameCode;
    private Instant playedAt;

    public Score() {
    }

    public Score(String name, String email, String date, int score) {
        this.name = name;
        this.email = email;
        this.date = date;
        this.score = score;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getGame() { return game; }

    public void setGame(String game) { this.game = game; }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getClientAttemptId() { return clientAttemptId; }
    public void setClientAttemptId(String clientAttemptId) { this.clientAttemptId = clientAttemptId; }
    public String getGameCode() { return gameCode; }
    public void setGameCode(String gameCode) { this.gameCode = gameCode; }
    public Instant getPlayedAt() { return playedAt; }
    public void setPlayedAt(Instant playedAt) { this.playedAt = playedAt; }
}
