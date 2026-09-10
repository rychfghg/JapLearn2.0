package japlearn.demo.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import japlearn.demo.Entity.Score;
import japlearn.demo.Repository.ScoreRepository;

@Service
public class ScoreService {

    @Autowired
    private ScoreRepository scoreRepository;

    // Save a score
    public Score saveScore(Score score) {
        return scoreRepository.save(score);
    }

    public Score saveHighScore(Score attempt) {
        // Keep every completed run for averages, completion history, weak-area
        // analysis, and teacher reports. getHighScore still derives the personal
        // best using the repository's score-descending query.
        attempt.setId(null);
        attempt.setEmail(attempt.getEmail().trim().toLowerCase());
        attempt.setGame(attempt.getGame().trim().toUpperCase());
        attempt.setScore(Math.max(0, attempt.getScore()));
        attempt.setMaxScore(Math.max(0, attempt.getMaxScore()));
        attempt.setCorrectAnswers(Math.max(0, attempt.getCorrectAnswers()));
        attempt.setTotalQuestions(Math.max(0, attempt.getTotalQuestions()));
        return scoreRepository.save(attempt);
    }

    public Optional<Score> getHighScore(String email, String game) {
        return scoreRepository.findTopByEmailIgnoreCaseAndGameIgnoreCaseOrderByScoreDesc(
                email.trim().toLowerCase(), game.trim().toUpperCase());
    }
    public List<Score> getScoresByEmail(String email) { return scoreRepository.findByEmailIgnoreCaseOrderByDateDesc(email); }

    // Delete a score by ID
    public void deleteScore(String id) {
        if (scoreRepository.existsById(id)) {
            scoreRepository.deleteById(id);
        } else {
            throw new RuntimeException("Score not found");
        }
    }

    // Retrieve all scores
    public List<Score> getAllScores() {
        return scoreRepository.findAll();
    }

    public List<Score> getScoresByDate(String date) {
        return scoreRepository.findByDate(date);
    }
    
    public List<String> getAllAvailableDates() {
    return scoreRepository.findAll()
                          .stream()
                          .map(Score::getDate) // Extract dates
                          .distinct()          // Ensure uniqueness
                          .collect(Collectors.toList());
}

public void deleteScoresByDate(String date) {
    List<Score> scoresToDelete = scoreRepository.findByDate(date);
    scoreRepository.deleteAll(scoresToDelete);
}

}
