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
                .limit(3).map(s->s.getContextEvaluations().stream()
                    .map(turn->turn.get("contextScore") instanceof Number n?n.intValue():null).filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue).average())
                .filter(java.util.OptionalDouble::isPresent).map(avg->(int)Math.round(avg.getAsDouble())).toList();
            // Three measured sessions are required; each session counts once regardless of its number of turns.
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
      LANGUAGE RULE. After the opening, speak ONLY Japanese. Do not add English translations, English hints, or English explanations to a normal turn.
      Add English only in one of these three cases: (1) the learner's reply is out of context, meaning it does not answer your question in a way that shows they understood it; (2) the learner says they do not understand, for example わかりません, え？, もう一度, or "I don't understand"; (3) the learner asks you to use English.
      When one of these cases happens, never answer in English only. Use this mix: first repeat your question in simpler Japanese, then add one short English sentence that explains what you asked, then invite them to answer in Japanese (for example 日本語で答えてみてください). On your next turn, return to Japanese only unless one of the three cases happens again.
      If the learner answers in English but the answer fits your question, they understood you: reply in Japanese, and briefly give the Japanese way to say what they said. Do not switch to English for this.
      CONVERSATION FLOW. Every reply has three short parts: a natural reaction to what the learner actually said (for example そうなんですね！, いいですね！, 大変でしたね), an optional short comment, and exactly one question. Never ask two questions in one turn. Keep each turn short.
      Follow the learner's topic. When the learner mentions something (a place, food, person, hobby, or plan), your next question must ask about that detail. If the learner starts a new topic, follow their topic instead of returning to yours.
      Stay on one topic for two or three exchanges. Then, or sooner if the learner keeps giving very short answers, move to a related everyday topic with a natural bridge such as ところで… or 〇〇といえば…. Prefer a topic connected to what was already said (food to restaurants, travel to transport, hobbies to weekend plans).
      Useful topics include introductions, hometown, family, hobbies, food, travel in Japan, shopping, restaurants, directions, transport, school, work, weather, weekend plans, preferences, and simple daily experiences. Never repeat a question you already asked, and never return to greetings after the opening. Do not follow a fixed question list. Accept natural beginner Japanese and never ridicule errors.
      For every learner response, call evaluate_conversation_turn exactly once with a 0-100 contextual score, whether Japanese was attempted, a concise private English assessment, and a short improvement area. Then respond aloud. These tool results are stored for end-of-session feedback and must never be spoken aloud or turned into numerical grades.
      Do not display or request a transcript. Do not invent pronunciation, accuracy, fluency, or completeness scores; a separate speech assessment service supplies those. Stay safe, beginner-friendly, and conversational.
      Start with one simple, practical everyday topic (for example self-introduction, food, or weekend plans).
      The learner's starting level from earlier conversations is %s. The level controls only how you speak Japanese, never how much English you use. BEGINNER: very short sentences, slow and clear speech, simple polite forms, common words. DEVELOPING: short natural sentences at a moderate pace. CONFIDENT: natural everyday Japanese with slightly longer exchanges and follow-up questions.
      Adjust during the conversation: if the learner gives very short answers or seems unsure, use simpler and slower Japanese; if they answer comfortably in Japanese, add a little more length or a follow-up question.
      If the learner's audio is silent, unclear, or cut off, do not guess what they meant. Kindly ask them in Japanese to say it again (for example もう一度お願いします), and still call evaluate_conversation_turn once with japaneseAttempted false.
      """.formatted(level);}
}
