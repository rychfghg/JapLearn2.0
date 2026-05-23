package japlearn.demo.Controller;


import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import japlearn.demo.Entity.Score;
import japlearn.demo.Service.ScoreService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final ScoreService scoreService;

    @Autowired
    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @PostMapping("/save")
    public ResponseEntity<Score> saveScore(@RequestBody Score score) {
        try {
            return ResponseEntity.ok(scoreService.saveScore(score));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteScore(@RequestParam String id) {
        try {
            scoreService.deleteScore(id);
            return ResponseEntity.ok("{\"message\": \"Score deleted successfully\"}");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/export")
    public void exportScoresAsCsv(@RequestParam String date, HttpServletResponse response) {
    try {
        List<Score> scores = scoreService.getScoresByDate(date);

        // Sort scores by last name alphabetically
        scores.sort((score1, score2) -> {
            String lastName1 = getLastName(score1.getName());
            String lastName2 = getLastName(score2.getName());
            return lastName1.compareToIgnoreCase(lastName2);
        });

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"scores_" + date + ".csv\"");

        PrintWriter writer = response.getWriter();

        writer.println("Name,Score"); // CSV Header

        for (Score score : scores) {
            String formattedName = formatName(score.getName()); // Format names properly
            writer.println("\"" + formattedName + "\"," + score.getScore()); // Add quotes around Name
        }

        writer.flush();
    } catch (Exception e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        e.printStackTrace();
    }
}

// Helper method to format names as "Lastname, Firstname"
private String formatName(String fullName) {
    String[] parts = fullName.trim().split("\\s+"); // Split the name into parts based on spaces
    if (parts.length >= 2) {
        String lastname = parts[parts.length - 1]; // The last word is the lastname
        String firstname = String.join(" ", Arrays.copyOfRange(parts, 0, parts.length - 1)); // Join everything else as firstname
        return lastname + ", " + firstname; // Return in "Lastname, Firstname" format
    }
    return fullName; // If only one word, return as is
}

// Helper method to extract the last name from a full name
private String getLastName(String fullName) {
    String[] parts = fullName.trim().split("\\s+");
    return (parts.length >= 2) ? parts[parts.length - 1] : fullName; // Return last word as last name
}


    
@GetMapping("/getAvailableDates")
public ResponseEntity<List<String>> getAvailableDates() {
    List<String> availableDates = scoreService.getAllAvailableDates(); // Fetch dates from DB
    return ResponseEntity.ok(availableDates);
}

@DeleteMapping("/deleteByDate")
public ResponseEntity<String> deleteScoresByDate(@RequestParam String date) {
    try {
        scoreService.deleteScoresByDate(date);
        return ResponseEntity.ok("{\"message\": \"Scores deleted successfully for the date: " + date + "\"}");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
    }
}

}
