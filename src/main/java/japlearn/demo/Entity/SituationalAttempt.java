package japlearn.demo.Entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "situational_attempts")
public class SituationalAttempt {
    @Id
    private String id;
    private String email;
    private String name;
    private String gameType;
    private String difficulty;
    private int score;
    private int maxScore;
    private int totalQuestions;
    private int correctAnswers;
    private java.util.List<ExpressionQuestionResult> questionResults;
    private java.util.List<PolitenessQuestionResult> politenessResponses;
    public java.util.List<PolitenessQuestionResult> getPolitenessResponses() { return politenessResponses; }
    public void setPolitenessResponses(java.util.List<PolitenessQuestionResult> value) { politenessResponses = value; }
    public java.util.List<ExpressionQuestionResult> getQuestionResults() { return questionResults; }
    public void setQuestionResults(java.util.List<ExpressionQuestionResult> value) { questionResults = value; }
    /** Hints opened during the run (sent by Ahiru Rescue from this version on; null on older runs). */
    private Integer hintsUsed;
    /** Questions answered when a run was left unfinished (Politeness partial saves, from this version on). */
    private Integer answeredCount;
    /** Per-question first answers (questionId, order, difficulty, skill, selected, correct, hintUsed); null on older runs. */
    private java.util.List<java.util.Map<String, Object>> answers;
    /** Untimed transfer practice after a mistake; recorded separately and never scored. */
    private java.util.List<java.util.Map<String, Object>> skillReviews;
    /** CLEARED or GAME_OVER for Ahiru Rescue; null on older runs. */
    private String endReason;
    public java.util.List<java.util.Map<String, Object>> getAnswers() { return answers; }
    public void setAnswers(java.util.List<java.util.Map<String, Object>> answers) { this.answers = answers; }
    public java.util.List<java.util.Map<String, Object>> getSkillReviews() { return skillReviews; }
    public void setSkillReviews(java.util.List<java.util.Map<String, Object>> skillReviews) { this.skillReviews = skillReviews; }
    public String getEndReason() { return endReason; }
    public void setEndReason(String endReason) { this.endReason = endReason; }
    public Integer getAnsweredCount() { return answeredCount; }
    public void setAnsweredCount(Integer answeredCount) { this.answeredCount = answeredCount; }
    public Integer getHintsUsed() { return hintsUsed; }
    public void setHintsUsed(Integer hintsUsed) { this.hintsUsed = hintsUsed; }
    private int wrongAnswers;
    private double accuracy;
    private int stars;
    private boolean completed;
    private int level;
    private int setNumber;
    private String topic;
    private String clientAttemptId;
    private Instant completedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public int getWrongAnswers() { return wrongAnswers; }
    public void setWrongAnswers(int wrongAnswers) { this.wrongAnswers = wrongAnswers; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getSetNumber() { return setNumber; }
    public void setSetNumber(int setNumber) { this.setNumber = setNumber; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getClientAttemptId() { return clientAttemptId; }
    public void setClientAttemptId(String clientAttemptId) { this.clientAttemptId = clientAttemptId; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
