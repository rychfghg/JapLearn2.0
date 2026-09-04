package japlearn.demo.Controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.ResponseRushProgress;
import japlearn.demo.Repository.ResponseRushProgressRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/response-rush/progress")
public class ResponseRushProgressController {
    private final ResponseRushProgressRepository progress;

    public ResponseRushProgressController(ResponseRushProgressRepository progress) {
        this.progress = progress;
    }

    @GetMapping
    public ResponseEntity<ResponseRushProgress> get(@RequestParam String email) {
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().build();
        return progress.findByEmailIgnoreCase(email.trim())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    public ResponseEntity<?> save(
            @RequestParam String email,
            @RequestBody ResponseRushProgress incoming) {
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().build();
        String normalizedEmail = email.trim().toLowerCase();
        if (incoming.getCurrentNodeId() == null || incoming.getCurrentNodeId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "currentNodeId is required."));
        }
        ResponseRushProgress saved = progress.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(ResponseRushProgress::new);
        saved.setEmail(normalizedEmail);
        saved.setCurrentNodeId(incoming.getCurrentNodeId());
        saved.setAnswers(incoming.getAnswers());
        saved.setTimeLeft(Math.max(0, incoming.getTimeLeft()));
        saved.setBestPercentage(Math.max(saved.getBestPercentage(), incoming.getBestPercentage()));
        saved.setCompleted(incoming.isCompleted());
        saved.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(progress.save(saved));
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@RequestParam String email) {
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().build();
        progress.deleteByEmailIgnoreCase(email.trim());
        return ResponseEntity.noContent().build();
    }
}
