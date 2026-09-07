package japlearn.demo.Entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "guided_phrase_daily_practices")
@CompoundIndex(name = "guided_phrase_learner_day", def = "{'email':1,'practiceDate':1}", unique = true)
public class GuidedPhraseDailyPractice {
    @Id private String id;
    private String email;
    private String practiceDate;
    private int sessionsStarted;
    private int sessionsCompleted;
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPracticeDate() { return practiceDate; }
    public void setPracticeDate(String practiceDate) { this.practiceDate = practiceDate; }
    public int getSessionsStarted() { return sessionsStarted; }
    public void setSessionsStarted(int sessionsStarted) { this.sessionsStarted = sessionsStarted; }
    public int getSessionsCompleted() { return sessionsCompleted; }
    public void setSessionsCompleted(int sessionsCompleted) { this.sessionsCompleted = sessionsCompleted; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
