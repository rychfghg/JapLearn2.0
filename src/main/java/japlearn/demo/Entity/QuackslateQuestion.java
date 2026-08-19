package japlearn.demo.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quackslateQuestionBank")
public class QuackslateQuestion {
    @Id
    private String id;
    private String prompt;
    private String translation;
    private String category;
    private String difficulty;
    private String[] options;
    private String correctAnswer;
    private String explanation;
    private boolean approved = true;
    private boolean systemAvailable = true;
    private String createdBy = "SYSTEM";

    public QuackslateQuestion() {}

    public QuackslateQuestion(String prompt, String translation, String category, String difficulty,
            String[] options, String correctAnswer, String explanation) {
        this.prompt = prompt;
        this.translation = translation;
        this.category = category;
        this.difficulty = difficulty;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getTranslation() { return translation; }
    public void setTranslation(String translation) { this.translation = translation; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String[] getOptions() { return options; }
    public void setOptions(String[] options) { this.options = options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public boolean isSystemAvailable() { return systemAvailable; }
    public void setSystemAvailable(boolean systemAvailable) { this.systemAvailable = systemAvailable; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
