package japlearn.demo.Controller;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.DialogueRelayProgress;
import japlearn.demo.Repository.DialogueRelayProgressRepository;
import japlearn.demo.Service.EmailInput;

/** Saves the learner's unfinished Dialogue Relay run. Ownership is checked by StudentOwnershipFilter. */
@RestController
@RequestMapping("/api/dialogue-relay/progress")
public class DialogueRelayProgressController {

    private final DialogueRelayProgressRepository progress;

    public DialogueRelayProgressController(DialogueRelayProgressRepository progress) {
        this.progress = progress;
    }

    @PutMapping
    public ResponseEntity<?> save(@RequestParam String email, @RequestBody Map<String, Object> body) {
        String normalized = EmailInput.normalize(email);
        if (normalized == null) return ResponseEntity.badRequest().body(Map.of("error", "Invalid email."));
        int answered = clamp(body.get("answered"), 25), correct = Math.min(answered, clamp(body.get("correct"), 25));
        int bonusAnswered = clamp(body.get("bonusAnswered"), 5), bonusCorrect = Math.min(bonusAnswered, clamp(body.get("bonusCorrect"), 5));
        DialogueRelayProgress p = progress.findByEmailIgnoreCase(normalized).orElseGet(() -> {
            DialogueRelayProgress fresh = new DialogueRelayProgress();
            fresh.setEmail(normalized.toLowerCase(Locale.ROOT));
            return fresh;
        });
        // A new run (fewer answers than saved) starts a new record time.
        if (answered < p.getAnswered()) p.setStartedAt(Instant.now());
        p.setAnswered(answered);
        p.setCorrect(correct);
        p.setBonusAnswered(bonusAnswered);
        p.setBonusCorrect(bonusCorrect);
        p.setUpdatedAt(Instant.now());
        progress.save(p);
        return ResponseEntity.ok(Map.of("saved", true));
    }

    @DeleteMapping
    public ResponseEntity<?> clear(@RequestParam String email) {
        String normalized = EmailInput.normalize(email);
        if (normalized != null) progress.deleteByEmailIgnoreCase(normalized);
        return ResponseEntity.ok(Map.of("cleared", true));
    }

    private static int clamp(Object value, int max) {
        try { return Math.max(0, Math.min(max, (int) Math.round(Double.parseDouble(String.valueOf(value))))); }
        catch (Exception e) { return 0; }
    }
}
