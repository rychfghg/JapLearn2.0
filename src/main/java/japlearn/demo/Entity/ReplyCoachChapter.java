package japlearn.demo.Entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reply_coach_chapters")
public class ReplyCoachChapter {
    @Id
    private String id;
    private String title;
    private String description;
    private String difficulty = "BEGINNER";
    private List<String> learningObjectives = new ArrayList<>();
    private String status = "DRAFT";
    private String startNodeId;
    private String coverKey;
    private int order;
    private List<StoryNode> nodes = new ArrayList<>();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public static class StoryNode {
        private String id;
        private String type;
        private String title;
        private String text;
        private String japanese;
        private String romaji;
        private String speaker;
        private String characterKey;
        private String expressionKey;
        private String secondaryCharacterKey;
        private String secondaryExpressionKey;
        private String backgroundKey;
        private String audioUrl;
        private boolean spritesVisible = true;
        private boolean tapToContinue = true;
        private boolean shuffleChoices;
        private String nextNodeId;
        private List<ChoiceOption> choices = new ArrayList<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getJapanese() { return japanese; }
        public void setJapanese(String japanese) { this.japanese = japanese; }
        public String getRomaji() { return romaji; }
        public void setRomaji(String romaji) { this.romaji = romaji; }
        public String getSpeaker() { return speaker; }
        public void setSpeaker(String speaker) { this.speaker = speaker; }
        public String getCharacterKey() { return characterKey; }
        public void setCharacterKey(String characterKey) { this.characterKey = characterKey; }
        public String getExpressionKey() { return expressionKey; }
        public void setExpressionKey(String expressionKey) { this.expressionKey = expressionKey; }
        public String getSecondaryCharacterKey() { return secondaryCharacterKey; }
        public void setSecondaryCharacterKey(String secondaryCharacterKey) { this.secondaryCharacterKey = secondaryCharacterKey; }
        public String getSecondaryExpressionKey() { return secondaryExpressionKey; }
        public void setSecondaryExpressionKey(String secondaryExpressionKey) { this.secondaryExpressionKey = secondaryExpressionKey; }
        public String getBackgroundKey() { return backgroundKey; }
        public void setBackgroundKey(String backgroundKey) { this.backgroundKey = backgroundKey; }
        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
        public boolean isSpritesVisible() { return spritesVisible; }
        public void setSpritesVisible(boolean spritesVisible) { this.spritesVisible = spritesVisible; }
        public boolean isTapToContinue() { return tapToContinue; }
        public void setTapToContinue(boolean tapToContinue) { this.tapToContinue = tapToContinue; }
        public boolean isShuffleChoices() { return shuffleChoices; }
        public void setShuffleChoices(boolean shuffleChoices) { this.shuffleChoices = shuffleChoices; }
        public String getNextNodeId() { return nextNodeId; }
        public void setNextNodeId(String nextNodeId) { this.nextNodeId = nextNodeId; }
        public List<ChoiceOption> getChoices() { return choices; }
        public void setChoices(List<ChoiceOption> choices) { this.choices = choices; }
    }

    public static class ChoiceOption {
        private String id;
        private String text;
        private String japanese;
        private String romaji;
        private String evaluation;
        private int points;
        private String explanation;
        private String culturalNote;
        private String reactionText;
        private String reactionCharacterKey;
        private String reactionExpressionKey;
        private String nextNodeId;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getJapanese() { return japanese; }
        public void setJapanese(String japanese) { this.japanese = japanese; }
        public String getRomaji() { return romaji; }
        public void setRomaji(String romaji) { this.romaji = romaji; }
        public String getEvaluation() { return evaluation; }
        public void setEvaluation(String evaluation) { this.evaluation = evaluation; }
        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        public String getCulturalNote() { return culturalNote; }
        public void setCulturalNote(String culturalNote) { this.culturalNote = culturalNote; }
        public String getReactionText() { return reactionText; }
        public void setReactionText(String reactionText) { this.reactionText = reactionText; }
        public String getReactionCharacterKey() { return reactionCharacterKey; }
        public void setReactionCharacterKey(String reactionCharacterKey) { this.reactionCharacterKey = reactionCharacterKey; }
        public String getReactionExpressionKey() { return reactionExpressionKey; }
        public void setReactionExpressionKey(String reactionExpressionKey) { this.reactionExpressionKey = reactionExpressionKey; }
        public String getNextNodeId() { return nextNodeId; }
        public void setNextNodeId(String nextNodeId) { this.nextNodeId = nextNodeId; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public List<String> getLearningObjectives() { return learningObjectives; }
    public void setLearningObjectives(List<String> learningObjectives) { this.learningObjectives = learningObjectives; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStartNodeId() { return startNodeId; }
    public void setStartNodeId(String startNodeId) { this.startNodeId = startNodeId; }
    public String getCoverKey() { return coverKey; }
    public void setCoverKey(String coverKey) { this.coverKey = coverKey; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public List<StoryNode> getNodes() { return nodes; }
    public void setNodes(List<StoryNode> nodes) { this.nodes = nodes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
