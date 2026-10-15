package japlearn.demo.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import japlearn.demo.Entity.Score;
import java.util.*;

/** V2 practice evidence and response speed are different measurements. */
public final class ReplyRushEvidence {
    private static final JsonNode BANK;
    static {
        try(var input=ReplyRushEvidence.class.getResourceAsStream("/reply-rush-v2.json")) {
            BANK=new ObjectMapper().readTree(input);
        } catch(Exception e) { throw new ExceptionInInitializerError(e); }
    }
    public static boolean isTimed(Score score) { return "RUSH_TIMED_V2".equals(score.getMode()); }
    public static boolean isPractice(Score score) { return "RUSH_PRACTICE_V2".equals(score.getMode()); }
    public static boolean timedMode(String mode) { return "RUSH_TIMED_V2".equals(mode)||"TIMED".equals(mode); }
    public static String summary(Score score) {
        if(score.getAssessmentVersion()!=2||!"QUACKRESPONSE_RUSH".equalsIgnoreCase(score.getGame()))return null;
        var m=score.getRushMetrics();
        return "Answered accuracy (timeouts excluded): "+(m.get("answeredAccuracy")==null?"not assessed":m.get("answeredAccuracy")+"%")
            +"; incorrect: "+m.get("incorrect")+"; timeouts: "+m.get("timeouts")+"; technical interruptions: "+m.get("technicalInterruptions")
            +"; mean eligible response: "+(m.get("meanResponseMs")==null?"no speed sample":m.get("meanResponseMs")+" ms")+". Practice and speed are separate measurements.";
    }
    public static void normalize(Score score) {
        if(!"QUACKRESPONSE_RUSH".equalsIgnoreCase(score.getGame())||score.getAssessmentVersion()!=2)return;
        if(!isTimed(score)&&!isPractice(score))throw new IllegalArgumentException("Choose practice or timed challenge.");
        var incoming=score.getRushOutcomes();
        if(incoming==null||incoming.size()>250)throw new IllegalArgumentException("Invalid round evidence.");
        Set<String> terminals=new HashSet<>();List<Map<String,Object>> canonical=new ArrayList<>();
        Set<String> interrupted=new HashSet<>();
        for(var item:incoming)if("TECHNICAL_INTERRUPTION".equals(item.get("outcome")))interrupted.add(String.valueOf(item.get("nodeId")));
        int correct=0,incorrect=0,timeouts=0,interruptions=0,speedSamples=0;long speed=0;
        for(var item:incoming) {
            String node=String.valueOf(item.get("nodeId")),kind=String.valueOf(item.get("outcome"));
            JsonNode question=BANK.get(node);if(question==null)throw new IllegalArgumentException("Unknown question.");
            Map<String,Object> row=new LinkedHashMap<>();row.put("nodeId",node);
            if("TECHNICAL_INTERRUPTION".equals(kind)) {
                interruptions++;row.put("outcome",kind);row.put("reason",String.valueOf(item.getOrDefault("reason","Interrupted")));
            } else {
                if(!terminals.add(node))throw new IllegalArgumentException("Duplicate answered question.");
                if("TIMEOUT".equals(kind)) {
                    if(!isTimed(score))throw new IllegalArgumentException("Practice cannot time out.");
                    timeouts++;row.put("outcome","TIMEOUT");
                } else {
                    String choice=String.valueOf(item.get("choiceId"));JsonNode evaluation=question.path("choices").get(choice);
                    if(evaluation==null)throw new IllegalArgumentException("Unknown response.");
                    boolean good=Set.of("BEST","ACCEPTABLE").contains(evaluation.asText());
                    if(good)correct++;else incorrect++;
                    row.put("choiceId",choice);row.put("evaluation",evaluation.asText());row.put("outcome",good?"CORRECT":"INCORRECT");
                    boolean eligible=isTimed(score)&&!interrupted.contains(node)&&Boolean.TRUE.equals(item.get("timingEligible"));
                    row.put("timingEligible",eligible);
                    if(eligible&&item.get("responseMs") instanceof Number ms&&ms.longValue()>=0&&ms.longValue()<=20000){row.put("responseMs",ms.longValue());speed+=ms.longValue();speedSamples++;}
                }
            }
            canonical.add(row);
        }
        if(score.isCompleted()&&terminals.size()!=BANK.size())throw new IllegalArgumentException("Complete every question before saving the round.");
        Map<String,Object> stats=new LinkedHashMap<>();stats.put("correct",correct);stats.put("incorrect",incorrect);stats.put("timeouts",timeouts);stats.put("technicalInterruptions",interruptions);
        stats.put("answered",correct+incorrect);stats.put("answeredAccuracy",correct+incorrect==0?null:Math.round(correct*100.0/(correct+incorrect)));
        stats.put("meanResponseMs",speedSamples==0?null:Math.round((double)speed/speedSamples));stats.put("speedSamples",speedSamples);
        score.setRushMetrics(stats);score.setRushOutcomes(canonical);score.setScore(correct);score.setCorrectAnswers(correct);score.setTotalQuestions(correct+incorrect);score.setMaxScore(correct+incorrect);
        // Follow-up practice is attached only by the authenticated review endpoint.
        score.setRushFollowups(new ArrayList<>());
    }
    public static void review(Score score,List<Map<String,Object>> answers) {
        if(score.getAssessmentVersion()!=2||!score.isCompleted()||answers==null||answers.size()>20)throw new IllegalArgumentException("A completed V2 round is required.");
        Set<String> missed=new HashSet<>();for(var e:score.getRushOutcomes())if(!"CORRECT".equals(e.get("outcome")))missed.add(String.valueOf(e.get("nodeId")));
        var stored=score.getRushFollowups()==null?new ArrayList<Map<String,Object>>():new ArrayList<>(score.getRushFollowups());
        for(var answer:answers){String node=String.valueOf(answer.get("nodeId")),selected=String.valueOf(answer.get("selectedJapanese"));JsonNode q=BANK.get(node);
            if(q==null||!missed.contains(node)||(!selected.equals(q.path("followup").asText())&&!selected.equals(q.path("alternate").asText())))throw new IllegalArgumentException("Invalid skill review answer.");
            if(stored.stream().noneMatch(row->node.equals(row.get("nodeId"))))stored.add(Map.of("nodeId",node,"selectedJapanese",selected,"correct",selected.equals(q.path("followup").asText()),"mode","UNTIMED_REVIEW"));
        }score.setRushFollowups(stored);
    }
}
