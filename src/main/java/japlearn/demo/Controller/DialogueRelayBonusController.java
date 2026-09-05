package japlearn.demo.Controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import japlearn.demo.Entity.DialogueRelayBonusAssessment;
import japlearn.demo.Repository.DialogueRelayBonusAssessmentRepository;
import japlearn.demo.Service.DialogueRelayPronunciationService;

@RestController
@RequestMapping("/api/dialogue-relay/bonus")
public class DialogueRelayBonusController {
    private final DialogueRelayPronunciationService speech;
    private final DialogueRelayBonusAssessmentRepository assessments;

    public DialogueRelayBonusController(DialogueRelayPronunciationService speech, DialogueRelayBonusAssessmentRepository assessments) {
        this.speech = speech;
        this.assessments = assessments;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("provider", "AZURE_SPEECH", "configured", speech.configured(), "region", speech.region(), "rawAudioStored", false);
    }

    @PostMapping(value = "/assess", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> assess(@RequestPart("audio") MultipartFile audio,
                                    @RequestParam String email,
                                    @RequestParam String promptId) {
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().body(Map.of("message", "A learner account is required."));
        try {
            var result = speech.assess(promptId, audio);
            var prompt = speech.prompt(promptId);
            DialogueRelayBonusAssessment saved = new DialogueRelayBonusAssessment();
            saved.setEmail(email.trim().toLowerCase());
            saved.setPromptId(promptId);
            saved.setPromptTitle(prompt.title());
            saved.setReferenceText(result.referenceText());
            saved.setRecognizedText(result.recognizedText());
            saved.setResponseAppropriate(result.appropriate());
            saved.setContextVerdict(result.contextVerdict());
            saved.setContextExplanation(result.contextExplanation());
            saved.setPronunciationFeedback(result.pronunciationFeedback());
            saved.setPronunciationGuide(result.pronunciationGuide());
            saved.setPronunciationScore(result.pronunciation());
            saved.setAccuracyScore(result.accuracy());
            saved.setFluencyScore(result.fluency());
            saved.setCompletenessScore(result.completeness());
            saved.setContextScore(result.contextScore());
            saved.setFeedback(result.feedback());
            saved.setWordIssues(result.wordIssues());
            saved.setAssessedAt(Instant.now());
            saved = assessments.save(saved);
            return ResponseEntity.ok(Map.ofEntries(
                Map.entry("id", saved.getId()), Map.entry("recognizedText", result.recognizedText()),
                Map.entry("appropriate", result.appropriate()), Map.entry("pronunciationScore", result.pronunciation()),
                Map.entry("accuracyScore", result.accuracy()), Map.entry("fluencyScore", result.fluency()),
                Map.entry("completenessScore", result.completeness()), Map.entry("feedback", result.feedback()),
                Map.entry("contextScore", result.contextScore()),
                Map.entry("english", result.english()), Map.entry("referenceText", result.referenceText()),
                Map.entry("wordIssues", result.wordIssues()), Map.entry("contextVerdict", result.contextVerdict()),
                Map.entry("contextExplanation", result.contextExplanation()),
                Map.entry("pronunciationFeedback", result.pronunciationFeedback()),
                Map.entry("pronunciationGuide", result.pronunciationGuide()),
                Map.entry("missingIdeas", result.missingIdeas())));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        } catch (IllegalStateException error) {
            return ResponseEntity.status(503).body(Map.of("message", error.getMessage()));
        } catch (Exception error) {
            return ResponseEntity.status(502).body(Map.of("message", "Azure could not assess this recording. Please record again."));
        }
    }

    @GetMapping("/assessments")
    public List<DialogueRelayBonusAssessment> assessments(@RequestParam String email) {
        return email == null || email.isBlank() ? List.of() : assessments.findByEmailIgnoreCaseOrderByAssessedAtDesc(email.trim());
    }
}
