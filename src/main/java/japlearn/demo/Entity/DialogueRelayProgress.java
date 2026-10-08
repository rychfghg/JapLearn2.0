package japlearn.demo.Entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The learner's current, unfinished Dialogue Relay run (one per learner), saved as they answer so
 * an abandoned run can still be evaluated. Removed when the run is finished (the Score record takes over).
 */
@Document(collection = "dialogue_relay_progress")
public class DialogueRelayProgress {
    @Id private String id;
    @Indexed(unique = true) private String email;
    /** Dialogue choices answered so far (out of 25) and how many were correct. */
    private int answered;
    private int correct;
    /** Spoken bonus prompts assessed so far (out of 5) and how many were appropriate to the context. */
    private int bonusAnswered;
    private int bonusCorrect;
    private Instant startedAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAnswered() { return answered; }
    public void setAnswered(int answered) { this.answered = answered; }
    public int getCorrect() { return correct; }
    public void setCorrect(int correct) { this.correct = correct; }
    public int getBonusAnswered() { return bonusAnswered; }
    public void setBonusAnswered(int bonusAnswered) { this.bonusAnswered = bonusAnswered; }
    public int getBonusCorrect() { return bonusCorrect; }
    public void setBonusCorrect(int bonusCorrect) { this.bonusCorrect = bonusCorrect; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
