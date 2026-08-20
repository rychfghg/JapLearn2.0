package japlearn.demo.Config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import japlearn.demo.Entity.SituationalQuestion;
import japlearn.demo.Repository.SituationalQuestionRepository;

@Component
public class ExpressionMatchSeeder implements CommandLineRunner {
    private final SituationalQuestionRepository repository;

    public ExpressionMatchSeeder(SituationalQuestionRepository repository) {
        this.repository = repository;
    }

    private record Pair(String jp, String romaji, String meaning, String scene, String key) {}

    private static final List<Pair> PAIRS = List.of(
        new Pair("おはようございます", "Ohayou gozaimasu", "Good morning", "Greet your instructor early in the morning.", "school"),
        new Pair("ありがとうございます", "Arigatou gozaimasu", "Thank you", "A classmate returns the notebook you lost.", "classroom"),
        new Pair("すみません", "Sumimasen", "Excuse me / Sorry", "You accidentally bump into someone at the station.", "station"),
        new Pair("おめでとうございます", "Omedetou gozaimasu", "Congratulations", "Your friend has just won a competition.", "school"),
        new Pair("さようなら", "Sayounara", "Goodbye", "You leave your instructor at the end of class.", "classroom"),
        new Pair("いただきます", "Itadakimasu", "Thank you for the meal", "Everyone is ready to begin eating.", "meal"),
        new Pair("ごちそうさまでした", "Gochisousama deshita", "Thank you for the meal", "You have finished a meal prepared for you.", "meal"),
        new Pair("おつかれさまです", "Otsukaresama desu", "Thank you for your work", "You meet a colleague after a long work shift.", "office"),
        new Pair("いってきます", "Ittekimasu", "I am leaving", "You tell your family you are leaving home.", "home"),
        new Pair("おかえりなさい", "Okaerinasai", "Welcome home", "A family member returns home in the evening.", "home"),
        new Pair("はじめまして", "Hajimemashite", "Nice to meet you", "You are introduced to someone for the first time.", "office"),
        new Pair("よろしくおねがいします", "Yoroshiku onegaishimasu", "Please treat me well", "You finish introducing yourself to a new group.", "classroom")
    );

    @Override
    public void run(String... args) {
        if (!repository.findByGameTypeIgnoreCaseOrderByOrderAsc("EXPRESSION_MATCH").isEmpty()) return;
        int order = 1;
        for (int level = 1; level <= 5; level++) {
            int setCount = level <= 3 ? 3 : level == 4 ? 5 : 10;
            int pairCount = level <= 2 ? 3 : level <= 4 ? 4 : 5;
            for (int set = 1; set <= setCount; set++) {
                String topic = topic(level, set);
                for (int index = 0; index < pairCount; index++) {
                    Pair pair = PAIRS.get((level * 2 + set + index - 3) % PAIRS.size());
                    SituationalQuestion question = new SituationalQuestion();
                    question.setGameType("EXPRESSION_MATCH");
                    question.setDifficulty(level == 5 ? "HARD" : level >= 3 ? "INTERMEDIATE" : "STARTER");
                    question.setLevel(level);
                    question.setSetNumber(set);
                    question.setTopic(topic);
                    question.setOrder(order++);
                    question.setLocation(topic);
                    question.setSceneKey(pair.key());
                    question.setScenario(pair.scene());
                    question.setHint(pair.meaning());
                    question.setCorrectAnswer(pair.jp());
                    question.setExplanation(pair.meaning());
                    question.setChoices(List.of(new SituationalQuestion.ResponseChoice(pair.jp(), pair.romaji())));
                    question.setActive(true);
                    repository.save(question);
                }
            }
        }
    }

    private String topic(int level, int set) {
        String[][] topics = {
            {"Morning greetings", "Thanks and apologies", "Friendly farewells"},
            {"School moments", "Meals and hospitality", "Home routines"},
            {"Introductions", "Workplace courtesy", "Travel encounters"},
            {"Formal school life", "Service encounters", "Group activities", "Celebrations", "Daily transitions"},
            {"Respectful speech", "Unexpected encounters", "Formal hospitality", "Academic life", "Workplace nuance", "Public spaces", "Family occasions", "Travel problems", "Community events", "Mixed mastery"}
        };
        return topics[level - 1][set - 1];
    }
}
