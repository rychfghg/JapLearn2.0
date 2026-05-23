package japlearn.demo.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quackslateContent")
public class QuackslateContent {

    @Id
    private int id;
    private String englishWord;            // The word or phrase in Japanese
    private String translatedWord;  // The translated word or phrase
    private String level;           // The level or title
    private String gameCode;        // Game code to optionally link content to a specific game
    private String[] options;       // Options for multiple-choice questions
    private String correctAnswer;   // The correct answer for the content
    private String wrongAnswer; // Add the wrongAnswer field
    // Default constructor
    public QuackslateContent() {}

    // Constructor with parameters
    public QuackslateContent(int id, String englishWord, String translatedWord, String level, String gameCode, String[] options, String correctAnswer, String wrongAnswer) {
        this.id = id;
        this.englishWord = englishWord;
        this.translatedWord = translatedWord;
        this.level = level;
        this.gameCode = gameCode;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.wrongAnswer = wrongAnswer;
    }

    public String getWrongAnswer() {
        return wrongAnswer;
    }

    public void setWrongAnswer(String wrongAnswer) {
        this.wrongAnswer = wrongAnswer;
    }
    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEnglishWord() {
        return englishWord;
    }

    public void setEnglishWord(String englishWord) {
        this.englishWord = englishWord;
    }

    public String getTranslatedWord() {
        return translatedWord;
    }

    public void setTranslatedWord(String translatedWord) {
        this.translatedWord = translatedWord;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getGameCode() {
        return gameCode;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
}
