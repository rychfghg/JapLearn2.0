package japlearn.demo.Config;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import japlearn.demo.Entity.QuackslateQuestion;
import japlearn.demo.Repository.QuackslateQuestionRepository;

@Component
public class QuackslateQuestionSeeder implements ApplicationRunner {
    private final QuackslateQuestionRepository repository;

    public QuackslateQuestionSeeder(QuackslateQuestionRepository repository) { this.repository = repository; }

    private QuackslateQuestion q(String prompt, String translation, String category, String difficulty,
            String answer, String explanation, String... options) {
        return new QuackslateQuestion(prompt, translation, category, difficulty, options, answer, explanation);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) return;
        repository.saveAll(List.of(
            q("I am a student.", "わたし は がくせい です。", "Particles", "Beginner", "わたし は がくせい です", "は marks the topic.", "わたし", "は", "がくせい", "です"),
            q("This is a book.", "これ は ほん です。", "Copula", "Beginner", "これ は ほん です", "です makes the statement polite.", "これ", "は", "ほん", "です"),
            q("That is a school.", "それ は がっこう です。", "Copula", "Beginner", "それ は がっこう です", "それ refers to something near the listener.", "それ", "は", "がっこう", "です"),
            q("I eat sushi.", "わたし は すし を たべます。", "Particles", "Beginner", "わたし は すし を たべます", "を marks the direct object.", "わたし", "は", "すし", "を", "たべます"),
            q("I drink water.", "みず を のみます。", "Particles", "Beginner", "みず を のみます", "The object comes before を and the verb.", "みず", "を", "のみます"),
            q("I go to school.", "がっこう へ いきます。", "Movement", "Beginner", "がっこう へ いきます", "へ marks a direction.", "がっこう", "へ", "いきます"),
            q("I study at the library.", "としょかん で べんきょうします。", "Location", "Beginner", "としょかん で べんきょうします", "で marks where an action happens.", "としょかん", "で", "べんきょうします"),
            q("There is a cat.", "ねこ が います。", "Existence", "Beginner", "ねこ が います", "います is used for living things.", "ねこ", "が", "います"),
            q("There is a desk.", "つくえ が あります。", "Existence", "Beginner", "つくえ が あります", "あります is used for non-living things.", "つくえ", "が", "あります"),
            q("Whose umbrella is this?", "これ は だれ の かさ ですか。", "Questions", "Beginner", "これ は だれ の かさ ですか", "の connects the owner and the object.", "これ", "は", "だれ", "の", "かさ", "ですか"),
            q("I wake up at seven.", "しちじ に おきます。", "Time", "Beginner", "しちじ に おきます", "に marks a specific time.", "しちじ", "に", "おきます"),
            q("I study Japanese every day.", "まいにち にほんご を べんきょうします。", "Frequency", "Beginner", "まいにち にほんご を べんきょうします", "Frequency usually comes before the object.", "まいにち", "にほんご", "を", "べんきょうします"),
            q("This bag is expensive.", "この かばん は たかい です。", "Adjectives", "Beginner", "この かばん は たかい です", "An い-adjective comes before です.", "この", "かばん", "は", "たかい", "です"),
            q("The room is quiet.", "へや は しずか です。", "Adjectives", "Beginner", "へや は しずか です", "しずか is a な-adjective used here as a predicate.", "へや", "は", "しずか", "です"),
            q("I do not eat meat.", "にく を たべません。", "Negatives", "Beginner", "にく を たべません", "ません is the polite negative verb ending.", "にく", "を", "たべません"),
            q("Did you watch the movie?", "えいが を みましたか。", "Past tense", "Beginner", "えいが を みましたか", "ましたか forms a polite past question.", "えいが", "を", "みましたか"),
            q("Let us go together.", "いっしょに いきましょう。", "Invitation", "Intermediate", "いっしょに いきましょう", "ましょう makes a polite suggestion.", "いっしょに", "いきましょう"),
            q("May I take a picture?", "しゃしん を とっても いいですか。", "Permission", "Intermediate", "しゃしん を とっても いいですか", "てもいいですか asks permission.", "しゃしん", "を", "とっても", "いいですか"),
            q("Please speak slowly.", "ゆっくり はなして ください。", "Requests", "Intermediate", "ゆっくり はなして ください", "てください makes a polite request.", "ゆっくり", "はなして", "ください"),
            q("I like Japanese food.", "にほんりょうり が すき です。", "Preferences", "Beginner", "にほんりょうり が すき です", "すき commonly takes が.", "にほんりょうり", "が", "すき", "です"),
            q("Because it is raining, I will not go.", "あめ ですから いきません。", "Reason", "Intermediate", "あめ ですから いきません", "から connects a reason to its result.", "あめ", "ですから", "いきません"),
            q("After eating, I study.", "たべてから べんきょうします。", "Sequence", "Intermediate", "たべてから べんきょうします", "てから means after doing something.", "たべてから", "べんきょうします"),
            q("I can speak Japanese.", "にほんご を はなす ことが できます。", "Ability", "Intermediate", "にほんご を はなす ことが できます", "ことができます expresses ability.", "にほんご", "を", "はなす", "ことが", "できます"),
            q("I think tomorrow will be sunny.", "あした は はれる と おもいます。", "Opinion", "Intermediate", "あした は はれる と おもいます", "とおもいます expresses what someone thinks.", "あした", "は", "はれる", "と", "おもいます")
        ));
    }
}
