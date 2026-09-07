package japlearn.demo.Entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quack_talk_sessions")
public class QuackTalkSession {
    @Id
    private String id;
    private String email;
    private String name;
    private String roomType;
    private String language;
    private String scenarioTitle;
    private int durationSeconds;
    private int conversationTurns;
    private boolean completed;
    private boolean evaluated;
    private Integer score;
    private Integer pronunciationScore;
    private Integer accuracyScore;
    private Integer fluencyScore;
    private Integer completenessScore;
    private Integer contextualAccuracy;
    private String feedbackSummary;
    private List<String> expressionsPracticed;
    private List<String> areasForImprovement;
    private Instant practicedAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getScenarioTitle() { return scenarioTitle; }
    public void setScenarioTitle(String scenarioTitle) { this.scenarioTitle = scenarioTitle; }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getConversationTurns() { return conversationTurns; }
    public void setConversationTurns(int conversationTurns) { this.conversationTurns = conversationTurns; }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getPronunciationScore() { return pronunciationScore; }
    public void setPronunciationScore(Integer pronunciationScore) { this.pronunciationScore = pronunciationScore; }
    public Integer getAccuracyScore() { return accuracyScore; }
    public void setAccuracyScore(Integer accuracyScore) { this.accuracyScore = accuracyScore; }
    public Integer getFluencyScore() { return fluencyScore; }
    public void setFluencyScore(Integer fluencyScore) { this.fluencyScore = fluencyScore; }
    public Integer getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(Integer completenessScore) { this.completenessScore = completenessScore; }
    public Integer getContextualAccuracy() { return contextualAccuracy; }
    public void setContextualAccuracy(Integer contextualAccuracy) { this.contextualAccuracy = contextualAccuracy; }
    public String getFeedbackSummary() { return feedbackSummary; }
    public void setFeedbackSummary(String feedbackSummary) { this.feedbackSummary = feedbackSummary; }
    public List<String> getExpressionsPracticed() { return expressionsPracticed; }
    public void setExpressionsPracticed(List<String> expressionsPracticed) { this.expressionsPracticed = expressionsPracticed; }
    public List<String> getAreasForImprovement() { return areasForImprovement; }
    public void setAreasForImprovement(List<String> areasForImprovement) { this.areasForImprovement = areasForImprovement; }

    public Instant getPracticedAt() {
        return practicedAt;
    }

    public void setPracticedAt(Instant practicedAt) {
        this.practicedAt = practicedAt;
    }
}
