package japlearn.demo.Entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "situational_question_bank")
public class SituationalQuestion {
    @Id
    private String id;
    private String gameType;
    private String difficulty;
    private int order;
    private String location;
    private String sceneKey;
    private String imageUrl;
    private String imageAlt;
    private String scenario;
    private String hint;
    private List<ResponseChoice> choices = new ArrayList<>();
    private String correctAnswer;
    private String explanation;
    private int level;
    private int setNumber;
    private String topic;
    private boolean active = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSceneKey() { return sceneKey; }
    public void setSceneKey(String sceneKey) { this.sceneKey = sceneKey; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getImageAlt() { return imageAlt; }
    public void setImageAlt(String imageAlt) { this.imageAlt = imageAlt; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
    public List<ResponseChoice> getChoices() { return choices; }
    public void setChoices(List<ResponseChoice> choices) { this.choices = choices; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getSetNumber() { return setNumber; }
    public void setSetNumber(int setNumber) { this.setNumber = setNumber; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public static class ResponseChoice {
        private String japanese;
        private String romaji;

        public ResponseChoice() {}
        public ResponseChoice(String japanese, String romaji) {
            this.japanese = japanese;
            this.romaji = romaji;
        }
        public String getJapanese() { return japanese; }
        public void setJapanese(String japanese) { this.japanese = japanese; }
        public String getRomaji() { return romaji; }
        public void setRomaji(String romaji) { this.romaji = romaji; }
    }
}
