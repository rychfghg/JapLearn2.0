package japlearn.demo.Controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import japlearn.demo.Entity.GuidedPhraseDailyUsage;
import japlearn.demo.Repository.GuidedPhraseDailyUsageRepository;
import japlearn.demo.Service.LiveConversationProvider;
import japlearn.demo.Service.GuidedPhrasePronunciationService;
import japlearn.demo.Repository.QuackTalkSessionRepository;
import japlearn.demo.Repository.UserRepository;
import japlearn.demo.Entity.QuackTalkSession;

@RestController
@RequestMapping("/api/guided-phrase")
public class GuidedPhraseLiveController {
    private static final int DAILY_LIMIT=1200;
    private final LiveConversationProvider live;
    private final GuidedPhraseDailyUsageRepository usage;
    private final GuidedPhrasePronunciationService pronunciation;
    private final QuackTalkSessionRepository sessions;
    private final UserRepository users;
    public GuidedPhraseLiveController(LiveConversationProvider live,GuidedPhraseDailyUsageRepository usage,GuidedPhrasePronunciationService pronunciation,QuackTalkSessionRepository sessions,UserRepository users){this.live=live;this.usage=usage;this.pronunciation=pronunciation;this.sessions=sessions;this.users=users;}

    @GetMapping("/status") public Map<String,Object> status(@RequestParam String email){int remaining=remaining(email);return Map.of("provider",live.providerName(),"configured",live.configured(),"dailyLimitSeconds",DAILY_LIMIT,"remainingSeconds",remaining,"azurePronunciationEnabled",true);}

    @PostMapping("/live-token") public ResponseEntity<?> token(@RequestBody Map<String,String> request){
        String email=normalize(request.get("email")); if(email.isBlank())return ResponseEntity.badRequest().body(Map.of("message","A learner account is required."));
        if(users.findByEmail(email)==null)return ResponseEntity.status(403).body(Map.of("message","This learner account could not be verified."));
        int remaining=remaining(email); if(remaining<=0)return ResponseEntity.status(429).body(Map.of("message","Your 20-minute Guided Phrase allowance is complete for today.","remainingSeconds",0));
        String scenario=request.getOrDefault("scenario","First conversation in Japan");
        try {var access=live.createGuidedPhraseAccess(instruction(scenario),remaining);return ResponseEntity.ok(access);}catch(IllegalStateException e){return ResponseEntity.status(503).body(Map.of("message",e.getMessage()));}catch(Exception e){return ResponseEntity.status(502).body(Map.of("message","Gemini Live could not start. Please try again."));}
    }

    @PostMapping("/usage") public synchronized ResponseEntity<?> addUsage(@RequestBody Map<String,Object> request){
        String email=normalize(String.valueOf(request.getOrDefault("email",""))); if(email.isBlank())return ResponseEntity.badRequest().body(Map.of("message","A learner account is required."));
        int requested=Math.max(0,Math.min(30,number(request.get("seconds")))); String date=today();
        GuidedPhraseDailyUsage row=usage.findByEmailIgnoreCaseAndUsageDate(email,date).orElseGet(GuidedPhraseDailyUsage::new);
        row.setEmail(email);row.setUsageDate(date);row.setSecondsUsed(Math.min(DAILY_LIMIT,row.getSecondsUsed()+requested));row.setUpdatedAt(Instant.now());usage.save(row);
        return ResponseEntity.ok(Map.of("secondsUsed",row.getSecondsUsed(),"remainingSeconds",DAILY_LIMIT-row.getSecondsUsed()));
    }

    @PostMapping(value="/assess",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> assess(@RequestPart("audio") MultipartFile audio,@RequestParam String referenceText){
        try{return ResponseEntity.ok(pronunciation.assess(audio,referenceText));}catch(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}catch(IllegalStateException e){return ResponseEntity.status(503).body(Map.of("message",e.getMessage()));}catch(Exception e){return ResponseEntity.status(502).body(Map.of("message","Azure could not assess this response. Please try again."));}
    }

    @PostMapping("/complete") public ResponseEntity<?> complete(@RequestBody Map<String,Object> request){
        String email=normalize(String.valueOf(request.getOrDefault("email","")));if(email.isBlank())return ResponseEntity.badRequest().body(Map.of("message","A learner account is required."));
        QuackTalkSession row=new QuackTalkSession();row.setEmail(email);row.setName(String.valueOf(request.getOrDefault("name","")));row.setRoomType("GUIDED_PHRASE");row.setLanguage("JAPANESE");row.setDurationSeconds(Math.max(1,number(request.get("durationSeconds"))));row.setCompleted(true);row.setEvaluated(true);row.setScore(Math.max(0,Math.min(100,number(request.get("score")))));row.setPracticedAt(Instant.now());
        return ResponseEntity.ok(sessions.save(row));
    }

    private int remaining(String email){if(email==null||email.isBlank())return DAILY_LIMIT;return DAILY_LIMIT-usage.findByEmailIgnoreCaseAndUsageDate(normalize(email),today()).map(GuidedPhraseDailyUsage::getSecondsUsed).orElse(0);}
    private String today(){return LocalDate.now(ZoneId.of("Asia/Manila")).toString();}
    private String normalize(String value){return value==null?"":value.trim().toLowerCase();}
    private int number(Object value){try{return Integer.parseInt(String.valueOf(value));}catch(Exception e){return 0;}}
    private String instruction(String scenario){return """
      You are Sumi, JapLearn's friendly female Japanese conversation coach. This is Guided Phrase Practice only.
      Selected scenario: %s. The learner is a beginner. Start immediately by greeting the learner briefly in Japanese, then in English explain that this is Guided Phrase Practice and that you will complete five short spoken exchanges together.
      Conduct a realistic audio-first Japanese simulation. Ask or model one short beginner-appropriate phrase at a time, then wait for the learner's microphone response. React to what the learner actually says and dynamically choose the next turn; do not use a fixed question list and do not require one exact sentence when the meaning is valid.
      Before speaking each learner prompt, call prepare_practice_turn with the exact Japanese target, its English meaning, and a short instruction that includes the Japanese phrase. After every learner answer, call evaluate_learner_meaning before your spoken reaction. These tool calls record learning data; they must happen for all five turns.
      Stay strictly inside the selected scenario. Use Japanese primarily. If the learner is confused, briefly explain in English and return to Japanese. Keep every response short. Encourage gently and never ridicule pronunciation.
      Complete exactly five learner-response turns. At the fifth response, naturally conclude and congratulate the learner. Do not continue asking questions after five learner answers. Azure—not you—produces pronunciation, accuracy, fluency, completeness, and word-level scores; never invent numerical pronunciation scores.
      """.formatted(scenario);}
}
