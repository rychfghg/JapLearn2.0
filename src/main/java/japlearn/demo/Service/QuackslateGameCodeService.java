package japlearn.demo.Service;

import japlearn.demo.Entity.QuackslateGameCode;
import japlearn.demo.Repository.QuackslateGameCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuackslateGameCodeService {

    private final QuackslateGameCodeRepository quackslateGameCodeRepository;
    private Map<String, Boolean> quizStatusMap = new ConcurrentHashMap<>();  // Store quiz status per gameCode

    @Autowired
    public QuackslateGameCodeService(QuackslateGameCodeRepository quackslateGameCodeRepository) {
        this.quackslateGameCodeRepository = quackslateGameCodeRepository;
    }

    // Generate a new random alphanumeric game code
    public QuackslateGameCode generateNewGameCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";  // Alphanumeric characters
        StringBuilder gameCode = new StringBuilder();
        Random random = new Random();

        // Generate a 6-character random string
        for (int i = 0; i < 6; i++) {
            gameCode.append(characters.charAt(random.nextInt(characters.length())));
        }

        QuackslateGameCode newGame = new QuackslateGameCode();
        newGame.setGameCode(gameCode.toString());  // Set the generated game code

        return quackslateGameCodeRepository.save(newGame);  // Save the new game to the repository
    }

    // Fetch game code by levelID
    public String getGameCodeByLevelID(int levelID) {
        return quackslateGameCodeRepository.findById(levelID)
                .map(QuackslateGameCode::getGameCode)  // Extract gameCode if present
                .orElse(null);  // Return null if not present
    }

    // Method to find a game by its game code
    public Optional<QuackslateGameCode> findByGameCode(String gameCode) {
        return quackslateGameCodeRepository.findByGameCode(gameCode);  // Call the repository method
    }

    // Notify clients that the game has started
    public boolean notifyGameStart(String gameCode) {
        Optional<QuackslateGameCode> game = quackslateGameCodeRepository.findByGameCode(gameCode);
        if (game.isPresent()) {
            quizStatusMap.put(gameCode, true);  // Mark the game as started
            return true;
        } else {
            System.out.println("Game code not found: " + gameCode);  // Log failure
            return false;
        }
    }

    // Check if the quiz has started
    public boolean isQuizStarted(String gameCode) {
        return quizStatusMap.getOrDefault(gameCode, false);  // Return true if quiz has started, false otherwise
    }

    public boolean updateCurrentQuestionIndex(String gameCode, int newIndex) {
        Optional<QuackslateGameCode> gameOpt = quackslateGameCodeRepository.findByGameCode(gameCode);
        if (gameOpt.isPresent()) {
            QuackslateGameCode game = gameOpt.get();
            game.setCurrentQuestionIndex(newIndex);
            quackslateGameCodeRepository.save(game);  // Save updated game state
            return true;
        }
        return false;  // If the game code doesn't exist, return false
    }
    

    public int getCurrentQuestionIndex(String gameCode) {
        Optional<QuackslateGameCode> game = findByGameCode(gameCode);
        return game.map(QuackslateGameCode::getCurrentQuestionIndex).orElse(-1); // Returns -1 if game not found
    }
    public QuackslateGameCode findFirstActiveGame() {
        QuackslateGameCode activeGame = quackslateGameCodeRepository.findFirstByIsActiveTrue();
        if (activeGame != null) {
            System.out.println("Found first active game: " + activeGame.getGameCode());
        } else {
            System.out.println("No active games found in the database.");
        }
        return activeGame;
    }
    
    
    
}
