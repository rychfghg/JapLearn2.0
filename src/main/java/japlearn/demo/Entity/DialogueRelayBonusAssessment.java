package japlearn.demo.Entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "dialogue_relay_bonus_assessments")
public class DialogueRelayBonusAssessment {
    @Id private String id;
    private String email;
    private String promptId;
    private String promptTitle;
    private String referenceText;
    private String recognizedText;
    private boolean responseAppropriate;
    private double pronunciationScore;
    private double accuracyScore;
    private double fluencyScore;
    private double completenessScore;
    private String feedback;
    private List<String> wordIssues = new ArrayList<>();
    private Instant assessedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPromptId() { return promptId; }
    public void setPromptId(String promptId) { this.promptId = promptId; }
    public String getPromptTitle() { return promptTitle; }
    public void setPromptTitle(String promptTitle) { this.promptTitle = promptTitle; }
    public String getReferenceText() { return referenceText; }
    public void setReferenceText(String referenceText) { this.referenceText = referenceText; }
    public String getRecognizedText() { return recognizedText; }
    public void setRecognizedText(String recognizedText) { this.recognizedText = recognizedText; }
    public boolean isResponseAppropriate() { return responseAppropriate; }
    public void setResponseAppropriate(boolean responseAppropriate) { this.responseAppropriate = responseAppropriate; }
    public double getPronunciationScore() { return pronunciationScore; }
    public void setPronunciationScore(double pronunciationScore) { this.pronunciationScore = pronunciationScore; }
    public double getAccuracyScore() { return accuracyScore; }
    public void setAccuracyScore(double accuracyScore) { this.accuracyScore = accuracyScore; }
    public double getFluencyScore() { return fluencyScore; }
    public void setFluencyScore(double fluencyScore) { this.fluencyScore = fluencyScore; }
    public double getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(double completenessScore) { this.completenessScore = completenessScore; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public List<String> getWordIssues() { return wordIssues; }
    public void setWordIssues(List<String> wordIssues) { this.wordIssues = wordIssues; }
    public Instant getAssessedAt() { return assessedAt; }
    public void setAssessedAt(Instant assessedAt) { this.assessedAt = assessedAt; }
}
