package japlearn.demo.Controller;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import japlearn.demo.Service.ScoreService;
import japlearn.demo.Service.StudentAuthorizationService;

@RestController
@RequestMapping("/api/response-rush/review")
public class ReplyRushReviewController {
    private final ScoreService scores;private final StudentAuthorizationService auth;
    public ReplyRushReviewController(ScoreService scores,StudentAuthorizationService auth){this.scores=scores;this.auth=auth;}
    public record ReviewRequest(String email,String clientAttemptId,List<Map<String,Object>> answers){}
    @PostMapping
    public ResponseEntity<?> save(@RequestBody ReviewRequest body,@RequestHeader(value="X-Student-Token",required=false)String token){
        auth.requireOwnStudent(body.email(),token,true);
        try{return ResponseEntity.ok(scores.saveRushReview(body.email(),body.clientAttemptId(),body.answers()));}
        catch(IllegalArgumentException invalid){return ResponseEntity.badRequest().body(Map.of("message",invalid.getMessage()));}
    }
}
