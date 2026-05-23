package japlearn.demo.Controller;

import japlearn.demo.Entity.QuackslateGameCode;
import japlearn.demo.Service.QuackslateGameCodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/quackslateLevels")
public class QuackslateGameCodeController {

    private final QuackslateGameCodeService quackslateGameService;

    public QuackslateGameCodeController(QuackslateGameCodeService quackslateGameService) {
        this.quackslateGameService = quackslateGameService;
    }

    // API to generate a new game code
    @PostMapping("/generateGameCode")
    public ResponseEntity<Map<String, String>> generateNewGameCode() {
        QuackslateGameCode newGame = quackslateGameService.generateNewGameCode();
        Map<String, String> response = new HashMap<>();
        response.put("gameCode", newGame.getGameCode());
        return ResponseEntity.ok(response);
    }
   

    // API to start a quiz based on game code
    @PostMapping("/startQuiz/{gameCode}")
    public ResponseEntity<Map<String, String>> startQuiz(@PathVariable String gameCode) {
        System.out.println("Received start quiz request for gameCode: " + gameCode);
        boolean success = quackslateGameService.notifyGameStart(gameCode);  // Notify clients that the game has started

        if (success) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "Quiz started");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(404).body(null);  // Return 404 if the game code is invalid
        }
    }

    // API to check if the quiz has started
    @GetMapping("/isQuizStarted/{gameCode}")
    public ResponseEntity<Map<String, Boolean>> isQuizStarted(@PathVariable String gameCode) {
        boolean quizStarted = quackslateGameService.isQuizStarted(gameCode); // Check if the quiz has started
        Map<String, Boolean> response = new HashMap<>();
        response.put("quizStarted", quizStarted);
        return ResponseEntity.ok(response);
    }

    // *** New API to validate game code ***
    @GetMapping("/getGameCode/{gameCode}")
    public ResponseEntity<?> validateGameCode(@PathVariable String gameCode) {
        Optional<QuackslateGameCode> game = quackslateGameService.findByGameCode(gameCode);
        if (game.isPresent()) {
            // If game code is valid, return the game data
            return ResponseEntity.ok(game.get());
        } else {
            // Return 404 if the game code is invalid
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid Game Code");
        }
    }
    @PostMapping("/setCurrentQuestionIndex/{gameCode}")
    public ResponseEntity<Map<String, String>> setCurrentQuestionIndex(
        @PathVariable String gameCode, @RequestBody Map<String, Integer> request) {
        System.out.println("Updating current question index for gameCode: " + gameCode);
        boolean success = quackslateGameService.updateCurrentQuestionIndex(gameCode, request.get("currentQuestionIndex"));
        if (success) {
            return ResponseEntity.ok(Map.of("status", "Updated successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game code not found"));
        }
    }

@GetMapping("/getCurrentQuestionIndex/{gameCode}")
public ResponseEntity<Map<String, Integer>> getCurrentQuestionIndex(@PathVariable String gameCode) {
    Optional<QuackslateGameCode> game = quackslateGameService.findByGameCode(gameCode);
    if (game.isPresent()) {
        Map<String, Integer> response = new HashMap<>();
        response.put("currentQuestionIndex", game.get().getCurrentQuestionIndex());
        return ResponseEntity.ok(response);
    } else {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}




}


