package japlearn.demo.Entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "guided_practice_scenarios")
public class GuidedPracticeScenario {
    @Id private String id;
    private String title;
    private String category;
    private String roleName;
    private String introduction;
    private String objective;
    private String difficulty = "BEGINNER";
    private boolean published = true;
    private List<String> allowedTopics = new ArrayList<>();
    private List<String> allowedVocabulary = new ArrayList<>();
    private List<String> allowedGrammar = new ArrayList<>();
    private List<String> targetExpressions = new ArrayList<>();
    private List<String> progressiveHints = new ArrayList<>();
    private List<Node> nodes = new ArrayList<>();

    public static class Node {
        private String id;
        private String sumiJapanese;
        private String sumiRomaji;
        private String englishMeaning;
        private List<String> acceptedResponses = new ArrayList<>();
        private List<String> keywords = new ArrayList<>();
        private String register = "POLITE";
        private List<String> hints = new ArrayList<>();
        private String successReply;
        private String retryReply;
        private String nextNodeId;
        private boolean ending;
        public String getId(){return id;} public void setId(String v){id=v;}
        public String getSumiJapanese(){return sumiJapanese;} public void setSumiJapanese(String v){sumiJapanese=v;}
        public String getSumiRomaji(){return sumiRomaji;} public void setSumiRomaji(String v){sumiRomaji=v;}
        public String getEnglishMeaning(){return englishMeaning;} public void setEnglishMeaning(String v){englishMeaning=v;}
        public List<String> getAcceptedResponses(){return acceptedResponses;} public void setAcceptedResponses(List<String> v){acceptedResponses=v;}
        public List<String> getKeywords(){return keywords;} public void setKeywords(List<String> v){keywords=v;}
        public String getRegister(){return register;} public void setRegister(String v){register=v;}
        public List<String> getHints(){return hints;} public void setHints(List<String> v){hints=v;}
        public String getSuccessReply(){return successReply;} public void setSuccessReply(String v){successReply=v;}
        public String getRetryReply(){return retryReply;} public void setRetryReply(String v){retryReply=v;}
        public String getNextNodeId(){return nextNodeId;} public void setNextNodeId(String v){nextNodeId=v;}
        public boolean isEnding(){return ending;} public void setEnding(boolean v){ending=v;}
    }
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getRoleName(){return roleName;} public void setRoleName(String v){roleName=v;}
    public String getIntroduction(){return introduction;} public void setIntroduction(String v){introduction=v;}
    public String getObjective(){return objective;} public void setObjective(String v){objective=v;}
    public String getDifficulty(){return difficulty;} public void setDifficulty(String v){difficulty=v;}
    public boolean isPublished(){return published;} public void setPublished(boolean v){published=v;}
    public List<String> getAllowedTopics(){return allowedTopics;} public void setAllowedTopics(List<String> v){allowedTopics=v;}
    public List<String> getAllowedVocabulary(){return allowedVocabulary;} public void setAllowedVocabulary(List<String> v){allowedVocabulary=v;}
    public List<String> getAllowedGrammar(){return allowedGrammar;} public void setAllowedGrammar(List<String> v){allowedGrammar=v;}
    public List<String> getTargetExpressions(){return targetExpressions;} public void setTargetExpressions(List<String> v){targetExpressions=v;}
    public List<String> getProgressiveHints(){return progressiveHints;} public void setProgressiveHints(List<String> v){progressiveHints=v;}
    public List<Node> getNodes(){return nodes;} public void setNodes(List<Node> v){nodes=v;}
}
