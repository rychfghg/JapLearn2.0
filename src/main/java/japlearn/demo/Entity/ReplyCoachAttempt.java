package japlearn.demo.Entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reply_coach_attempts")
public class ReplyCoachAttempt {
    @Id
    private String id;
    private String email;
    private String name;
    private String chapterId;
    private String chapterTitle;
    private int attemptNumber;
    private String currentNodeId;
    private String status = "IN_PROGRESS";
    private int score;
    private int maximumScore;
    private int finalPercentage;
    private int bestCount;
    private int acceptableCount;
    private int awkwardCount;
    private int impoliteCount;
    private int rudeCount;
    private Instant startedAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant completedAt;
    private List<AnswerRecord> answers = new ArrayList<>();

    public static class AnswerRecord {
        private String nodeId;
        private String prompt;
        private String choiceId;
        private String selectedText;
        private String selectedJapanese;
        private String bestResponse;
        private String evaluation;
        private int points;
        private String explanation;
        private String culturalNote;
        private Instant answeredAt = Instant.now();

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getChoiceId() { return choiceId; }
        public void setChoiceId(String choiceId) { this.choiceId = choiceId; }
        public String getSelectedText() { return selectedText; }
        public void setSelectedText(String selectedText) { this.selectedText = selectedText; }
        public String getSelectedJapanese() { return selectedJapanese; }
        public void setSelectedJapanese(String selectedJapanese) { this.selectedJapanese = selectedJapanese; }
        public String getBestResponse() { return bestResponse; }
        public void setBestResponse(String bestResponse) { this.bestResponse = bestResponse; }
        public String getEvaluation() { return evaluation; }
        public void setEvaluation(String evaluation) { this.evaluation = evaluation; }
        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        public String getCulturalNote() { return culturalNote; }
        public void setCulturalNote(String culturalNote) { this.culturalNote = culturalNote; }
        public Instant getAnsweredAt() { return answeredAt; }
        public void setAnsweredAt(Instant answeredAt) { this.answeredAt = answeredAt; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getChapterId() { return chapterId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }
    public String getChapterTitle() { return chapterTitle; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getMaximumScore() { return maximumScore; }
    public void setMaximumScore(int maximumScore) { this.maximumScore = maximumScore; }
    public int getFinalPercentage() { return finalPercentage; }
    public void setFinalPercentage(int finalPercentage) { this.finalPercentage = finalPercentage; }
    public int getBestCount() { return bestCount; }
    public void setBestCount(int bestCount) { this.bestCount = bestCount; }
    public int getAcceptableCount() { return acceptableCount; }
    public void setAcceptableCount(int acceptableCount) { this.acceptableCount = acceptableCount; }
    public int getAwkwardCount() { return awkwardCount; }
    public void setAwkwardCount(int awkwardCount) { this.awkwardCount = awkwardCount; }
    public int getImpoliteCount() { return impoliteCount; }
    public void setImpoliteCount(int impoliteCount) { this.impoliteCount = impoliteCount; }
    public int getRudeCount() { return rudeCount; }
    public void setRudeCount(int rudeCount) { this.rudeCount = rudeCount; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public List<AnswerRecord> getAnswers() { return answers; }
    public void setAnswers(List<AnswerRecord> answers) { this.answers = answers; }
}
