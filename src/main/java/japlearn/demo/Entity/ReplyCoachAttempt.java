package japlearn.demo.Entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

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
    private List<ReadinessRun> readinessRuns = new ArrayList<>();
    private List<PracticeResult> practiceResults = new ArrayList<>();
    public List<ReadinessRun> getReadinessRuns() {
        if (readinessRuns == null) readinessRuns = new ArrayList<>();
        return readinessRuns;
    }
    public void setReadinessRuns(List<ReadinessRun> value) { readinessRuns = value; }
    public List<PracticeResult> getPracticeResults() {
        if (practiceResults == null) practiceResults = new ArrayList<>();
        return practiceResults;
    }
    public void setPracticeResults(List<PracticeResult> value) { practiceResults = value; }
    public static class ReadinessRun {
        public String id;
        public int bankVersion = 1;
        public Instant startedAt = Instant.now();
        public Instant completedAt;
        public List<String> questionIds = new ArrayList<>();
        public Map<String, List<String>> optionOrder = new LinkedHashMap<>();
        public Map<String, List<String>> support = new LinkedHashMap<>();
        public List<ReadinessAnswer> answers = new ArrayList<>();
    }
    public static class ReadinessAnswer {
        public String questionId;
        public String choiceId;
        public String skill;
        public boolean correct;
        public List<String> helpTypes = new ArrayList<>();
        public Instant answeredAt = Instant.now();
    }
    public static class PracticeResult {
        public String key;
        public String questionId;
        public boolean correct;
        public List<String> helpTypes = new ArrayList<>();
        public Instant answeredAt = Instant.now();
    }
    private Map<String, List<String>> supportUsage = new LinkedHashMap<>();
    public Map<String, List<String>> getSupportUsage() {
        if (supportUsage == null) supportUsage = new LinkedHashMap<>();
        return supportUsage;
    }
    public void setSupportUsage(Map<String, List<String>> supportUsage) { this.supportUsage = supportUsage; }

    public static class AnswerRecord {
        // Nullable for legacy answers: unknown is never presented as independent mastery.
        private List<String> helpTypes;
        private Boolean supported;
        public List<String> getHelpTypes() { return helpTypes; }
        public void setHelpTypes(List<String> helpTypes) { this.helpTypes = helpTypes; }
        public Boolean getSupported() { return supported; }
        public void setSupported(Boolean supported) { this.supported = supported; }
        private String nodeId;
        private String prompt;
        private String choiceId;
        private String selectedText;
        private String selectedJapanese;
        private String bestResponse;
        private String evaluation;
        private int points;
        private String explanation;
        /** Whether the learner opened the hint before answering (recorded from this version on; null on older answers). */
        private Boolean hintUsed;
        public Boolean getHintUsed() { return hintUsed; }
        public void setHintUsed(Boolean hintUsed) { this.hintUsed = hintUsed; }
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
