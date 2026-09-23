package japlearn.demo.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "quackslateGameCode")
public class QuackslateGameCode {

    @Id
    private String id;
    @Indexed
    private String gameCode;
    private String content;
    private int currentQuestionIndex = 0;
    private boolean isActive;
    @Indexed
    private String ownerTeacherEmail;
    private List<String> questionIds = new ArrayList<>();
    private List<String> joinedStudentEmails = new ArrayList<>();
    private Instant startsAt;
    private Instant endsAt;
    private boolean published;

    public QuackslateGameCode() {}

    public QuackslateGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGameCode() {
        return gameCode;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }
    public String getOwnerTeacherEmail() { return ownerTeacherEmail; }
    public void setOwnerTeacherEmail(String ownerTeacherEmail) { this.ownerTeacherEmail = ownerTeacherEmail; }
    public List<String> getQuestionIds() { return questionIds == null ? List.of() : questionIds; }
    public void setQuestionIds(List<String> questionIds) { this.questionIds = questionIds == null ? new ArrayList<>() : new ArrayList<>(questionIds); }
    public List<String> getJoinedStudentEmails() { return joinedStudentEmails == null ? List.of() : joinedStudentEmails; }
    public void setJoinedStudentEmails(List<String> joinedStudentEmails) { this.joinedStudentEmails = joinedStudentEmails == null ? new ArrayList<>() : new ArrayList<>(joinedStudentEmails); }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
