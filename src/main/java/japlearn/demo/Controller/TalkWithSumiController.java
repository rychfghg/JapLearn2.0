package japlearn.demo.Controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import japlearn.demo.Entity.QuackTalkSession;
import japlearn.demo.Repository.QuackTalkSessionRepository;
import japlearn.demo.Repository.UserRepository;
import japlearn.demo.Service.GuidedPhrasePronunciationService;
import japlearn.demo.Service.LiveConversationProvider;

@RestController
@RequestMapping("/api/talk-with-sumi")
public class TalkWithSumiController {
    private final LiveConversationProvider live;
    private final GuidedPhrasePronunciationService pronunciation;
    private final QuackTalkSessionRepository sessions;
    private final UserRepository users;
    private final org.springframework.data.mongodb.core.MongoTemplate mongo;
    public TalkWithSumiController(LiveConversationProvider live,GuidedPhrasePronunciationService pronunciation,QuackTalkSessionRepository sessions,UserRepository users,org.springframework.data.mongodb.core.MongoTemplate mongo){this.live=live;this.pronunciation=pronunciation;this.sessions=sessions;this.users=users;this.mongo=mongo;}

    @PostMapping("/live-token") public ResponseEntity<?> token(@RequestBody Map<String,String> request){
        String email=normalize(request.get("email"));if(email.isBlank()||users.findByEmail(email)==null)return ResponseEntity.status(403).body(Map.of("message","A verified learner account is required."));
        if(used(email)>=10)return ResponseEntity.status(429).body(Map.of("message","Today's ten responses are complete. Come back tomorrow.","resetsAt",resetAt(),"remaining",0));
        String level=learnerLevel(email);
        try{var access=live.createTalkWithSumiAccess(instruction(level));Map<String,Object> body=new LinkedHashMap<>();body.put("token",access.token());body.put("model",access.model());body.put("websocketUrl",access.websocketUrl());body.put("voice",access.voice());body.put("systemInstruction",access.systemInstruction());body.put("remaining",Math.max(0,10-used(email)));body.put("resetsAt",resetAt());body.put("level",level);return ResponseEntity.ok(body);}catch(IllegalStateException e){return ResponseEntity.status(503).body(Map.of("message",e.getMessage()));}catch(Exception e){return ResponseEntity.status(502).body(Map.of("message","Sumi could not open the conversation. Please try again."));}
    }

    @PostMapping(value="/assess",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<?> assess(@RequestPart("audio") MultipartFile audio){
        try{return ResponseEntity.ok(pronunciation.assessOpenConversation(audio));}catch(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}catch(IllegalStateException e){return ResponseEntity.status(503).body(Map.of("message","Pronunciation review is temporarily unavailable."));}catch(Exception e){return ResponseEntity.status(502).body(Map.of("message","This response could not be reviewed."));}
    }

    @PostMapping("/complete") public ResponseEntity<?> complete(@RequestBody Map<String,Object> request){
        String email=normalize(String.valueOf(request.getOrDefault("email","")));if(email.isBlank()||users.findByEmail(email)==null)return ResponseEntity.status(403).body(Map.of("message","A verified learner account is required."));
        QuackTalkSession row=new QuackTalkSession();row.setEmail(email);row.setName(String.valueOf(request.getOrDefault("name","")));row.setRoomType("TALK_WITH_SUMI");row.setLanguage("JAPANESE");row.setScenarioTitle("Open Japanese conversation");row.setDurationSeconds(Math.max(1,Math.min(3600,number(request.get("durationSeconds")))));row.setConversationTurns(Math.max(0,number(request.get("conversationTurns"))));row.setCompleted(true);row.setEvaluated(row.getConversationTurns()>0);row.setPronunciationScore(score(request.get("pronunciationScore")));row.setAccuracyScore(score(request.get("accuracyScore")));row.setFluencyScore(score(request.get("fluencyScore")));row.setCompletenessScore(score(request.get("completenessScore")));row.setContextualAccuracy(score(request.get("contextualAccuracy")));row.setScore(score(request.get("score")));row.setFeedbackSummary(String.valueOf(request.getOrDefault("feedbackSummary","Keep building confidence through short, natural Japanese exchanges.")));row.setExpressionsPracticed(strings(request.get("expressionsPracticed")));row.setAreasForImprovement(strings(request.get("areasForImprovement")));row.setTurnAssessments(turnAssessments(request.get("turnAssessments")));row.setContextEvaluations(contextEvaluations(request.get("contextEvaluations")));row.setId(String.valueOf(request.getOrDefault("sessionId",java.util.UUID.randomUUID().toString())));
        var existing=sessions.findById(row.getId());
        if(existing.isPresent()&&!email.equalsIgnoreCase(existing.get().getEmail()))return ResponseEntity.status(403).build();
        row.setEvaluated(!row.getTurnAssessments().isEmpty());
        if(!row.isEvaluated()){row.setScore(null);row.setPronunciationScore(null);row.setAccuracyScore(null);row.setFluencyScore(null);row.setCompletenessScore(null);}
        row.setPracticedAt(Instant.now());return ResponseEntity.ok(sessions.save(row));
    }

    private static final String USAGE="talk_with_sumi_daily_usage";
    private String day(){return java.time.LocalDate.now(java.time.ZoneId.of("Asia/Manila")).toString();}
    private String resetAt(){return java.time.LocalDate.now(java.time.ZoneId.of("Asia/Manila")).plusDays(1).atStartOfDay(java.time.ZoneId.of("Asia/Manila")).toInstant().toString();}
    private int used(String email){var doc=mongo.findById(email+":"+day(),org.bson.Document.class,USAGE);return doc==null?0:doc.getInteger("used",0);}
    @PostMapping("/reserve-response") public ResponseEntity<?> reserve(@RequestBody Map<String,String> request){
        String email=normalize(request.get("email"));
        if(email.isBlank()||users.findByEmail(email)==null)return ResponseEntity.status(403).body(Map.of("message","Sign in to continue."));
        String id=email+":"+day();
        var idQuery=new org.springframework.data.mongodb.core.query.Query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id));
        try{mongo.upsert(idQuery,new org.springframework.data.mongodb.core.query.Update().setOnInsert("used",0),USAGE);}catch(org.springframework.dao.DuplicateKeyException ignored){}
        var query=new org.springframework.data.mongodb.core.query.Query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id).and("used").lt(10));
        var updated=mongo.findAndModify(query,new org.springframework.data.mongodb.core.query.Update().inc("used",1),org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true),org.bson.Document.class,USAGE);
        if(updated==null)return ResponseEntity.status(429).body(Map.of("message","Today's ten responses are complete. Come back tomorrow.","remaining",0,"resetsAt",resetAt()));
        return ResponseEntity.ok(Map.of("remaining",10-updated.getInteger("used"),"resetsAt",resetAt()));
    }

    /** Returns one of today's responses when a turn could not be used (no speech or a technical failure). Max 5 returns per day. */
    @PostMapping("/release-response") public ResponseEntity<?> release(@RequestBody Map<String,String> request){
        String email=normalize(request.get("email"));
        if(email.isBlank()||users.findByEmail(email)==null)return ResponseEntity.status(403).body(Map.of("message","Sign in to continue."));
        var query=new org.springframework.data.mongodb.core.query.Query(new org.springframework.data.mongodb.core.query.Criteria().andOperator(
                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(email+":"+day()),
                org.springframework.data.mongodb.core.query.Criteria.where("used").gt(0),
                new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                        org.springframework.data.mongodb.core.query.Criteria.where("released").exists(false),
                        org.springframework.data.mongodb.core.query.Criteria.where("released").lt(MAX_RELEASES))));
        var updated=mongo.findAndModify(query,new org.springframework.data.mongodb.core.query.Update().inc("used",-1).inc("released",1),org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true),org.bson.Document.class,USAGE);
        int usedNow=updated==null?used(email):updated.getInteger("used",0);
        return ResponseEntity.ok(Map.of("remaining",Math.max(0,10-usedNow),"resetsAt",resetAt(),"released",updated!=null));
    }
    private static final int MAX_RELEASES=5;

    // Starting level thresholds (average context score of recent Talk with Sumi turns).
    // Provisional pilot values pending instructor approval; configurable without code changes.
    @org.springframework.beans.factory.annotation.Value("${app.sumi.level.developing-from:60}") private int developingFrom;
    @org.springframework.beans.factory.annotation.Value("${app.sumi.level.confident-from:80}") private int confidentFrom;

    /** BEGINNER, DEVELOPING or CONFIDENT from the learner's last three measured sessions (BEGINNER without enough history). */
    String learnerLevel(String email){
        try{
            List<Integer> recent=sessions.findByEmailIgnoreCaseOrderByPracticedAtDesc(email).stream()
                .filter(s->"TALK_WITH_SUMI".equalsIgnoreCase(s.getRoomType())&&s.getContextEvaluations()!=null&&!s.getContextEvaluations().isEmpty())
                .limit(3).flatMap(s->s.getContextEvaluations().stream())
                .map(turn->turn.get("contextScore") instanceof Number n?n.intValue():null).filter(java.util.Objects::nonNull).toList();
            if(recent.size()<3)return "BEGINNER";
            double average=recent.stream().mapToInt(Integer::intValue).average().orElse(0);
            return average>=confidentFrom?"CONFIDENT":average>=developingFrom?"DEVELOPING":"BEGINNER";
        }catch(Exception e){return "BEGINNER";}
    }

    private String normalize(String value){return value==null?"":value.trim().toLowerCase();}
    private int number(Object value){try{double parsed=Double.parseDouble(String.valueOf(value));return Double.isFinite(parsed)?(int)Math.round(parsed):0;}catch(Exception e){return 0;}}
    private int score(Object value){return Math.max(0,Math.min(100,number(value)));}
    private List<String> strings(Object value){if(!(value instanceof List<?> values))return List.of();LinkedHashSet<String> unique=new LinkedHashSet<>();values.stream().map(String::valueOf).filter(item->!item.isBlank()).forEach(unique::add);return unique.stream().limit(30).toList();}
    private List<Map<String,Object>> contextEvaluations(Object value){
        if(!(value instanceof List<?> values))return List.of();
        return values.stream().filter(Map.class::isInstance).limit(60).map(item->{
            Map<?,?> raw=(Map<?,?>)item;
            Map<String,Object> safe=new LinkedHashMap<>();
            safe.put("contextScore",score(raw.get("contextScore")));
            safe.put("japaneseAttempted",Boolean.TRUE.equals(raw.get("japaneseAttempted")));
            return safe;
        }).toList();
    }
    private List<Map<String,Object>> turnAssessments(Object value){
        if(!(value instanceof List<?> values))return List.of();
        return values.stream().filter(Map.class::isInstance).limit(30).map(item->{
            Map<?,?> raw=(Map<?,?>)item;
            Map<String,Object> safe=new LinkedHashMap<>();
            safe.put("recognizedText",String.valueOf(raw.get("recognizedText")));
            safe.put("pronunciation",score(raw.get("pronunciation")));
            safe.put("accuracy",score(raw.get("accuracy")));
            safe.put("fluency",score(raw.get("fluency")));
            safe.put("completeness",score(raw.get("completeness")));
            return safe;
        }).toList();
    }
    private String instruction(String level){return """
      You are Sumi, JapLearn's friendly Japanese conversation partner. This is Talk with Sumi, an open-ended voice conversation for a beginner learner, not Guided Phrase Practice and not a repeat-after-me quiz.
      Open by greeting the learner naturally in Japanese, then give one very short English introduction explaining that you will have a relaxed Japanese conversation. Invite the learner to begin, then ask one short beginner-friendly question in Japanese.
      After the opening, converse primarily in Japanese. Listen to what the learner actually communicates and decide every next response dynamically. Follow relevant details from their answer, sometimes changing naturally to another practical everyday topic. Do not follow a fixed question sequence. Ask only one main question at a time and keep each turn short.
      Accept natural beginner Japanese. If the learner answers in English, briefly acknowledge the meaning, encourage a simple Japanese version, provide a short Japanese example only when helpful, and continue. If the learner is silent, hesitant, or confused, reassure them and briefly explain your last question in English, then invite a simple Japanese response. Never ridicule errors.
      Useful topics include introductions, hometown, family, hobbies, food, travel in Japan, shopping, restaurants, directions, transport, work, weather, weekend plans, preferences, and simple daily experiences. Avoid repeatedly asking greetings or the same question.
      For every learner response, call evaluate_conversation_turn exactly once with a 0-100 contextual score, whether Japanese was attempted, a concise private English assessment, and a short improvement area. Then respond aloud naturally. These tool results are stored for end-of-session feedback and must not be spoken as numerical grades.
      Do not display or request a transcript. Do not invent pronunciation, accuracy, fluency, or completeness scores; a separate speech assessment service supplies those. Stay safe, beginner-friendly, and conversational.
      Start with one simple, practical everyday topic (for example self-introduction, food, or weekend plans) and stay on it for two or three short exchanges before moving to a related topic.
      The learner's starting level from earlier conversations is %s. BEGINNER: very short sentences, slow and clear speech, simple polite forms, and a brief English hint after each question. DEVELOPING: short natural sentences at a moderate pace, English only when the learner is stuck. CONFIDENT: natural everyday Japanese with slightly longer exchanges and follow-up questions, English only if asked.
      Adjust during the conversation from what the learner demonstrates: if they answer in English, give very short answers, or seem confused, simplify and slow down; if they answer comfortably in Japanese, add a little more length or a follow-up question.
      If the learner's audio is silent, unclear, or cut off, do not guess what they meant. Kindly ask them to say it again, and still call evaluate_conversation_turn once with japaneseAttempted false.
      """.formatted(level);}
}
