package japlearn.demo.Controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import japlearn.demo.Service.LiveConversationProvider;
import japlearn.demo.Service.GuidedPhrasePronunciationService;
import japlearn.demo.Repository.QuackTalkSessionRepository;
import japlearn.demo.Repository.GuidedPhraseDailyPracticeRepository;
import japlearn.demo.Repository.UserRepository;
import japlearn.demo.Entity.QuackTalkSession;
import japlearn.demo.Entity.GuidedPhraseDailyPractice;

@RestController
@RequestMapping("/api/guided-phrase")
public class GuidedPhraseLiveController {
    private static final int DAILY_PRACTICE_LIMIT=5;
    private final LiveConversationProvider live;
    private final GuidedPhrasePronunciationService pronunciation;
    private final QuackTalkSessionRepository sessions;
    private final UserRepository users;
    private final GuidedPhraseDailyPracticeRepository dailyPractices;
    public GuidedPhraseLiveController(LiveConversationProvider live,GuidedPhrasePronunciationService pronunciation,QuackTalkSessionRepository sessions,UserRepository users,GuidedPhraseDailyPracticeRepository dailyPractices){this.live=live;this.pronunciation=pronunciation;this.sessions=sessions;this.users=users;this.dailyPractices=dailyPractices;}

    @GetMapping("/status") public Map<String,Object> status(@RequestParam(required=false) String email){int used=email==null?0:practicesUsed(normalize(email));return Map.of("conversationReady",live.configured(),"pronunciationReady",pronunciation.configured(),"dailyPracticeLimit",DAILY_PRACTICE_LIMIT,"practicesRemaining",Math.max(0,DAILY_PRACTICE_LIMIT-used));}

    @PostMapping("/live-token") public synchronized ResponseEntity<?> token(@RequestBody Map<String,String> request){
        String email=normalize(request.get("email")); if(email.isBlank())return ResponseEntity.badRequest().body(Map.of("message","A learner account is required."));
        var learner=users.findByEmail(email);if(learner==null)return ResponseEntity.status(403).body(Map.of("message","This learner account could not be verified.","accessDisabled",true));
        if(!learner.isGuidedPhraseEnabled())return ResponseEntity.status(403).body(Map.of("message","Guided Phrase Practice is not available for your account yet. Your teacher or administrator will let you know when access is ready.","accessDisabled",true));
        int used=practicesUsed(email);if(used>=DAILY_PRACTICE_LIMIT)return ResponseEntity.status(429).body(Map.of("message","You have completed today's five Guided Phrase sessions. Come back tomorrow for more practice.","practicesRemaining",0));
        String scenario=request.getOrDefault("scenario","Everyday life in Japan");
        int dailyRotation=Math.floorMod(LocalDate.now(ZoneId.of("Asia/Manila")).getDayOfYear()+used,10);
        try {var access=live.createGuidedPhraseAccess(instruction(scenario,dailyRotation));return ResponseEntity.ok(Map.of("token",access.token(),"model",access.model(),"websocketUrl",access.websocketUrl(),"voice",access.voice(),"systemInstruction",access.systemInstruction(),"practicesRemaining",Math.max(0,DAILY_PRACTICE_LIMIT-used)));}catch(IllegalStateException e){return ResponseEntity.status(503).body(Map.of("message",e.getMessage()));}catch(Exception e){return ResponseEntity.status(502).body(Map.of("message","Sumi could not start the conversation. Please try again."));}
    }

    @PostMapping(value="/assess",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> assess(@RequestPart("audio") MultipartFile audio,@RequestParam String referenceText){
        try{return ResponseEntity.ok(pronunciation.assess(audio,referenceText));}catch(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}catch(IllegalStateException e){return ResponseEntity.status(503).body(Map.of("message","Pronunciation feedback is temporarily unavailable."));}catch(Exception e){return ResponseEntity.status(502).body(Map.of("message","Your response could not be reviewed. Please try again."));}
    }

    @PostMapping("/complete") public synchronized ResponseEntity<?> complete(@RequestBody Map<String,Object> request){
        String email=normalize(String.valueOf(request.getOrDefault("email","")));if(email.isBlank())return ResponseEntity.badRequest().body(Map.of("message","A learner account is required."));
        QuackTalkSession row=new QuackTalkSession();row.setEmail(email);row.setName(String.valueOf(request.getOrDefault("name","")));row.setRoomType("GUIDED_PHRASE");row.setLanguage("JAPANESE");row.setScenarioTitle(String.valueOf(request.getOrDefault("scenarioTitle","First conversations in Japan")));row.setDurationSeconds(Math.max(1,number(request.get("durationSeconds"))));row.setConversationTurns(Math.max(0,number(request.get("conversationTurns"))));row.setCompleted(true);row.setEvaluated(true);row.setScore(score(request.get("score")));row.setPronunciationScore(score(request.get("pronunciationScore")));row.setAccuracyScore(score(request.get("accuracyScore")));row.setFluencyScore(score(request.get("fluencyScore")));row.setCompletenessScore(score(request.get("completenessScore")));row.setContextualAccuracy(score(request.get("contextualAccuracy")));row.setFeedbackSummary(String.valueOf(request.getOrDefault("feedbackSummary","Keep practicing these phrases with a steady pace and clear sounds.")));row.setExpressionsPracticed(strings(request.get("expressionsPracticed")));row.setAreasForImprovement(strings(request.get("areasForImprovement")));row.setPracticedAt(Instant.now());
        QuackTalkSession saved=sessions.save(row);if(row.getConversationTurns()>=5){GuidedPhraseDailyPractice daily=dailyPractices.findByEmailIgnoreCaseAndPracticeDate(email,today()).orElseGet(GuidedPhraseDailyPractice::new);daily.setEmail(email);daily.setPracticeDate(today());daily.setSessionsCompleted(Math.min(DAILY_PRACTICE_LIMIT,daily.getSessionsCompleted()+1));daily.setUpdatedAt(Instant.now());dailyPractices.save(daily);}return ResponseEntity.ok(saved);
    }

    @GetMapping("/history") public ResponseEntity<?> history(@RequestParam String email){String normalized=normalize(email);if(normalized.isBlank())return ResponseEntity.badRequest().body(Map.of("message","A learner account is required."));return ResponseEntity.ok(sessions.findByEmailIgnoreCaseOrderByPracticedAtDesc(normalized).stream().filter(item->"GUIDED_PHRASE".equalsIgnoreCase(item.getRoomType())).toList());}

    private String normalize(String value){return value==null?"":value.trim().toLowerCase();}
    private String today(){return LocalDate.now(ZoneId.of("Asia/Manila")).toString();}
    private int practicesUsed(String email){return dailyPractices.findByEmailIgnoreCaseAndPracticeDate(email,today()).map(GuidedPhraseDailyPractice::getSessionsCompleted).orElse(0);}
    private int number(Object value){try{return Integer.parseInt(String.valueOf(value));}catch(Exception e){return 0;}}
    private int score(Object value){return Math.max(0,Math.min(100,number(value)));}
    private List<String> strings(Object value){if(!(value instanceof List<?> values))return List.of();return values.stream().map(String::valueOf).filter(item->!item.isBlank()).limit(20).toList();}
    private String instruction(String scenario,int dailyRotation){return """
      You are Sumi, JapLearn's friendly female Japanese conversation coach. This is Guided Phrase Practice only.
      Selected scenario: %s. Daily rotation: %d. The learner is a beginner. Start immediately by greeting the learner briefly in Japanese, then in English explain that this is Guided Phrase Practice and that you will complete five short spoken exchanges together.
      Conduct a realistic audio-first Japanese simulation. Ask or model one short beginner-appropriate phrase at a time, then wait for the learner's microphone response. React to what the learner actually says and dynamically choose the next turn; do not use a fixed question list and do not require one exact sentence when the meaning is valid.
      Before speaking each learner prompt, you MUST call prepare_practice_turn with the exact Japanese target, accurate beginner-readable romaji, its English meaning, and a short learner instruction. Never ask the learner to respond until this call succeeds. After every learner answer, call evaluate_learner_meaning before your spoken reaction. These tool calls record learning data and must happen exactly once for each of all five turns.
      Make the five exchanges meaningfully different and useful in everyday life in Japan. Select five different purposes from self-introduction, asking directions, trains, shopping, restaurants, convenience stores, hotels, requesting help, asking someone to repeat, apologizing, thanking, and polite social interaction. Do not make the session mostly greetings. Use the daily rotation to vary the five purposes and wording from day to day.
      Never repeat a target phrase inside a session. Never output placeholder tokens such as LEARNER_NAME. For self-introduction, use a natural replaceable pattern such as わたしは___です. Do not ask for or guess personal information.
      Stay strictly inside the selected scenario. Use Japanese primarily. After each learner response, acknowledge what you understood, react naturally in short Japanese, and add one brief English explanation when useful. If the learner is confused, briefly explain in English and return to Japanese. Keep every response short. Encourage with reactions such as いいですね, よくできました, or 惜しいです and never ridicule pronunciation.
      Complete exactly five learner-response turns. At the fifth response, naturally conclude and congratulate the learner. Do not continue asking questions after five learner answers. A separate assessment service produces pronunciation, accuracy, fluency, completeness, and word-level scores; never invent numerical pronunciation scores.
      """.formatted(scenario,dailyRotation);}
}
