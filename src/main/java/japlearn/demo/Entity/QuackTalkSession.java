package japlearn.demo.Entity;

import java.time.Instant;
import java.util.ArrayList;
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
    private int durationSeconds;
    private boolean completed;
    private boolean evaluated;
    private Integer score;
    private Instant practicedAt = Instant.now();
    private Instant startedAt;
    private Instant endedAt;
    private Instant usageRecordedAt;
    private String scenarioId;
    private String scenarioTitle;
    private String status;
    private String currentNodeId;
    private String currentSumiJapanese;
    private double scenarioObjectiveProgress;
    private int conversationTurns;
    private Integer averagePronunciationScore;
    private Integer averageAccuracyScore;
    private Integer averageFluencyScore;
    private Integer contextualAccuracy;
    private String registerPerformance;
    private int hintsUsed;
    private List<String> expressionsPracticed = new ArrayList<>();
    private List<String> incorrectPatterns = new ArrayList<>();
    private List<String> areasForImprovement = new ArrayList<>();
    private String feedbackSummary;
    private List<Turn> turns = new ArrayList<>();

    public static class Turn {
        private String nodeId, transcript, sumiJapanese, feedback, register;
        private Integer pronunciation, accuracy, fluency;
        private boolean contextuallyAppropriate;
        private int hintsUsed;
        private Instant createdAt = Instant.now();
        public String getNodeId(){return nodeId;} public void setNodeId(String v){nodeId=v;} public String getTranscript(){return transcript;} public void setTranscript(String v){transcript=v;}
        public String getSumiJapanese(){return sumiJapanese;} public void setSumiJapanese(String v){sumiJapanese=v;} public String getFeedback(){return feedback;} public void setFeedback(String v){feedback=v;}
        public String getRegister(){return register;} public void setRegister(String v){register=v;} public Integer getPronunciation(){return pronunciation;} public void setPronunciation(Integer v){pronunciation=v;}
        public Integer getAccuracy(){return accuracy;} public void setAccuracy(Integer v){accuracy=v;} public Integer getFluency(){return fluency;} public void setFluency(Integer v){fluency=v;}
        public boolean isContextuallyAppropriate(){return contextuallyAppropriate;} public void setContextuallyAppropriate(boolean v){contextuallyAppropriate=v;}
        public int getHintsUsed(){return hintsUsed;} public void setHintsUsed(int v){hintsUsed=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    }

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

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

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

    public Instant getPracticedAt() {
        return practicedAt;
    }

    public void setPracticedAt(Instant practicedAt) {
        this.practicedAt = practicedAt;
    }
    public Instant getStartedAt(){return startedAt;} public void setStartedAt(Instant v){startedAt=v;} public Instant getEndedAt(){return endedAt;} public void setEndedAt(Instant v){endedAt=v;}
    public Instant getUsageRecordedAt(){return usageRecordedAt;} public void setUsageRecordedAt(Instant v){usageRecordedAt=v;}
    public String getScenarioId(){return scenarioId;} public void setScenarioId(String v){scenarioId=v;} public String getScenarioTitle(){return scenarioTitle;} public void setScenarioTitle(String v){scenarioTitle=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getCurrentNodeId(){return currentNodeId;} public void setCurrentNodeId(String v){currentNodeId=v;}
    public String getCurrentSumiJapanese(){return currentSumiJapanese;} public void setCurrentSumiJapanese(String v){currentSumiJapanese=v;} public double getScenarioObjectiveProgress(){return scenarioObjectiveProgress;} public void setScenarioObjectiveProgress(double v){scenarioObjectiveProgress=v;}
    public int getConversationTurns(){return conversationTurns;} public void setConversationTurns(int v){conversationTurns=v;} public Integer getAveragePronunciationScore(){return averagePronunciationScore;} public void setAveragePronunciationScore(Integer v){averagePronunciationScore=v;}
    public Integer getAverageAccuracyScore(){return averageAccuracyScore;} public void setAverageAccuracyScore(Integer v){averageAccuracyScore=v;} public Integer getAverageFluencyScore(){return averageFluencyScore;} public void setAverageFluencyScore(Integer v){averageFluencyScore=v;}
    public Integer getContextualAccuracy(){return contextualAccuracy;} public void setContextualAccuracy(Integer v){contextualAccuracy=v;} public String getRegisterPerformance(){return registerPerformance;} public void setRegisterPerformance(String v){registerPerformance=v;}
    public int getHintsUsed(){return hintsUsed;} public void setHintsUsed(int v){hintsUsed=v;} public List<String> getExpressionsPracticed(){return expressionsPracticed;} public void setExpressionsPracticed(List<String> v){expressionsPracticed=v;}
    public List<String> getIncorrectPatterns(){return incorrectPatterns;} public void setIncorrectPatterns(List<String> v){incorrectPatterns=v;} public List<String> getAreasForImprovement(){return areasForImprovement;} public void setAreasForImprovement(List<String> v){areasForImprovement=v;}
    public String getFeedbackSummary(){return feedbackSummary;} public void setFeedbackSummary(String v){feedbackSummary=v;} public List<Turn> getTurns(){return turns;} public void setTurns(List<Turn> v){turns=v;}
}
