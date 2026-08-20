package japlearn.demo.Controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import japlearn.demo.Entity.SituationalAttempt;
import japlearn.demo.Repository.SituationalAttemptRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/communicationAnalytics")
public class CommunicationAnalyticsController {
    private final SituationalAttemptRepository attempts;

    public CommunicationAnalyticsController(SituationalAttemptRepository attempts) {
        this.attempts = attempts;
    }

    @GetMapping("/getStudentAnalytics")
    public Map<String, Object> getStudentAnalytics(@RequestParam String email) {
        List<SituationalAttempt> records = attempts
                .findByEmailIgnoreCaseAndCompletedTrueOrderByCompletedAtDesc(email);

        double recognitionAccuracy = averageAccuracy(records, "RECOGNITION");
        double responseAccuracy = averageAccuracy(records, "RESPONSE");
        double situationalAccuracy = averageAccuracy(records, "SITUATIONAL");
        if (situationalAccuracy == 0) situationalAccuracy = recognitionAccuracy;

        List<String> strengths = new ArrayList<>();
        List<String> weakAreas = new ArrayList<>();
        if (recognitionAccuracy >= 80) strengths.add("Recognizing natural expressions for everyday situations");
        else if (!records.isEmpty()) weakAreas.add("Choosing expressions that fit the person, place, and time");
        if (records.isEmpty()) weakAreas.add("No completed communication activity has been recorded yet");

        String recommendation;
        if (records.isEmpty()) {
            recommendation = "Complete Recognition to begin building a communication performance record.";
        } else if (recognitionAccuracy < 70) {
            recommendation = "Replay Recognition and review the explanation shown after each answer.";
        } else if (recognitionAccuracy < 90) {
            recommendation = "Continue with the hard Recognition missions to strengthen formal responses.";
        } else {
            recommendation = "Excellent situational awareness. Continue with Expression Match and Politeness.";
        }

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("quackTalkAccuracy", 0);
        analytics.put("quackSituateAccuracy", round(situationalAccuracy));
        analytics.put("quackResponseAccuracy", round(responseAccuracy));
        analytics.put("recognitionAccuracy", round(recognitionAccuracy));
        analytics.put("completedActivities", records.size());
        analytics.put("weakAreaCount", weakAreas.size());
        analytics.put("strengths", strengths);
        analytics.put("weakAreas", weakAreas);
        analytics.put("recommendation", recommendation);
        return analytics;
    }

    private double averageAccuracy(List<SituationalAttempt> records, String gameType) {
        return records.stream()
                .filter(record -> gameType.equalsIgnoreCase(record.getGameType()))
                .mapToDouble(SituationalAttempt::getAccuracy)
                .average()
                .orElse(0);
    }

    private int round(double value) {
        return (int) Math.round(value);
    }
}
