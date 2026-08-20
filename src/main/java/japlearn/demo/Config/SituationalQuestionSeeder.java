package japlearn.demo.Config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import japlearn.demo.Entity.SituationalQuestion;
import japlearn.demo.Entity.SituationalQuestion.ResponseChoice;
import japlearn.demo.Repository.SituationalQuestionRepository;

@Component
public class SituationalQuestionSeeder implements ApplicationRunner {
    private final SituationalQuestionRepository repository;

    public SituationalQuestionSeeder(SituationalQuestionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!repository.findByGameTypeIgnoreCaseOrderByOrderAsc("RECOGNITION").isEmpty()) return;

        repository.saveAll(List.of(
            q(1, "STARTER", "School hallway", "school", "You meet your professor in the hallway early in the morning. What should you say?", "Think about the time of day and use polite language with a professor.", "おはようございます", "A polite morning greeting is appropriate for a professor.", c("おはようございます", "Ohayou gozaimasu"), c("こんばんは", "Konbanwa"), c("ありがとう", "Arigatou"), c("じゃあ、また", "Jaa, mata")),
            q(2, "STARTER", "Campus courtyard", "school", "You see a classmate in the afternoon. Which greeting fits?", "This greeting is commonly used during the daytime.", "こんにちは", "Konnichiwa is the standard daytime or afternoon greeting.", c("こんにちは", "Konnichiwa"), c("おやすみなさい", "Oyasumi nasai"), c("ただいま", "Tadaima"), c("いただきます", "Itadakimasu")),
            q(3, "STARTER", "Evening class", "classroom", "You arrive for an evening class and greet your instructor. What do you say?", "Choose the greeting used after the daytime has ended.", "こんばんは", "Konbanwa means good evening and fits this setting.", c("こんばんは", "Konbanwa"), c("おはようございます", "Ohayou gozaimasu"), c("さようなら", "Sayounara"), c("すみません", "Sumimasen")),
            q(4, "STARTER", "Classroom", "classroom", "Your professor gives you helpful feedback. How do you respond politely?", "Use the polite form of thank you.", "ありがとうございます", "Arigatou gozaimasu is a polite way to express thanks.", c("ありがとうございます", "Arigatou gozaimasu"), c("どういたしまして", "Dou itashimashite"), c("ごめんなさい", "Gomen nasai"), c("まだです", "Mada desu")),
            q(5, "STARTER", "Library", "school", "You need to politely get the librarian's attention. What should you say first?", "This phrase can mean excuse me and is used to get attention.", "すみません", "Sumimasen politely gets someone's attention and can also express an apology.", c("すみません", "Sumimasen"), c("乾杯", "Kanpai"), c("おかえりなさい", "Okaeri nasai"), c("もちろんです", "Mochiron desu")),
            q(6, "STARTER", "Cafeteria", "meal", "Your meal has just been served. What do you say before eating?", "Japanese speakers traditionally say this before beginning a meal.", "いただきます", "Itadakimasu is said before eating to show appreciation for the meal.", c("いただきます", "Itadakimasu"), c("ごちそうさまでした", "Gochisousama deshita"), c("おいしいです", "Oishii desu"), c("どうぞ", "Douzo")),
            q(7, "STARTER", "Cafeteria", "meal", "You have finished a wonderful meal. Which expression fits?", "Choose the expression said after finishing a meal.", "ごちそうさまでした", "Gochisousama deshita expresses appreciation after a meal.", c("ごちそうさまでした", "Gochisousama deshita"), c("いただきます", "Itadakimasu"), c("乾杯", "Kanpai"), c("いってきます", "Ittekimasu")),
            q(8, "STARTER", "Orientation", "classroom", "You are introduced to a new classmate for the first time. What do you say first?", "Use the phrase reserved for a first meeting.", "はじめまして", "Hajimemashite is used when meeting someone for the first time.", c("はじめまして", "Hajimemashite"), c("お久しぶり", "Ohisashiburi"), c("おかえりなさい", "Okaeri nasai"), c("お疲れさまでした", "Otsukaresama deshita")),
            q(9, "STARTER", "Classroom", "classroom", "Class is over and you will see your friend again soon. What is a natural casual goodbye?", "This phrase means then, see you.", "じゃあ、また", "Jaa, mata is a natural casual way to say see you again.", c("じゃあ、また", "Jaa, mata"), c("いらっしゃいませ", "Irasshaimase"), c("ただいま", "Tadaima"), c("元気です", "Genki desu")),
            q(10, "STARTER", "Study group", "classroom", "Your classmate asks if you understand the task, and you do. What is the direct reply?", "Choose the standard affirmative response.", "はい", "Hai is the standard polite response meaning yes.", c("はい", "Hai"), c("いいえ", "Iie"), c("まだです", "Mada desu"), c("助けてください", "Tasukete kudasai")),
            q(11, "STARTER", "Dormitory", "home", "Your roommate returns and says Tadaima. What should you reply?", "This response welcomes someone back home.", "おかえりなさい", "Okaeri nasai is the expected response to Tadaima.", c("おかえりなさい", "Okaeri nasai"), c("いってらっしゃい", "Itterasshai"), c("おやすみなさい", "Oyasumi nasai"), c("ようこそ", "Youkoso")),
            q(12, "STARTER", "Campus gate", "school", "A friend is leaving and says Ittekimasu. What do you say back?", "This set response wishes the departing person well.", "いってらっしゃい", "Itterasshai is the expected response to Ittekimasu.", c("いってらっしゃい", "Itterasshai"), c("ただいま", "Tadaima"), c("どうぞ", "Douzo"), c("頑張ってください", "Ganbatte kudasai")),
            q(13, "STARTER", "Reunion", "school", "You meet a friend you have not seen for months. What is the natural opening?", "Choose the phrase meaning long time no see.", "お久しぶり", "Ohisashiburi is used when meeting someone after a long absence.", c("お久しぶり", "Ohisashiburi"), c("はじめまして", "Hajimemashite"), c("おめでとうございます", "Omedetou gozaimasu"), c("どういたしまして", "Dou itashimashite")),
            q(14, "STARTER", "Club room", "classroom", "A teammate is nervous before a presentation. What can you say to encourage them?", "Use the expression that means please do your best or good luck.", "頑張ってください", "Ganbatte kudasai is used to encourage someone to do their best.", c("頑張ってください", "Ganbatte kudasai"), c("気をつけてください", "Ki o tsukete kudasai"), c("ゆっくり話してください", "Yukkuri hanashite kudasai"), c("まだです", "Mada desu")),
            q(15, "STARTER", "Class discussion", "classroom", "Your professor is speaking too quickly. What is the most useful polite request?", "Ask the speaker to slow down.", "ゆっくり話してください", "Yukkuri hanashite kudasai politely asks someone to speak slowly.", c("ゆっくり話してください", "Yukkuri hanashite kudasai"), c("ちょっと待ってください", "Chotto matte kudasai"), c("どうぞ入ってください", "Douzo haitte kudasai"), c("もちろんです", "Mochiron desu")),
            q(16, "HARD", "Faculty office", "office", "You are leaving the faculty office before your professor. Which formal expression is most appropriate?", "Acknowledge that you are leaving ahead of a senior person.", "お先に失礼します", "Osaki ni shitsurei shimasu politely excuses you for leaving ahead of someone senior.", c("お先に失礼します", "Osaki ni shitsurei shimasu"), c("じゃあ、また", "Jaa, mata"), c("さようなら", "Sayounara"), c("いってきます", "Ittekimasu")),
            q(17, "HARD", "Project room", "office", "Your team has completed a long project meeting. What expression recognizes everyone's effort?", "Use the workplace expression for appreciating hard work.", "お疲れさまでした", "Otsukaresama deshita thanks others for their work and effort.", c("お疲れさまでした", "Otsukaresama deshita"), c("よくできました", "Yoku dekimashita"), c("ありがとうございます", "Arigatou gozaimasu"), c("どういたしまして", "Dou itashimashite")),
            q(18, "HARD", "Reception desk", "office", "A visitor arrives for a scheduled meeting. What should you say while inviting them inside?", "Choose the welcoming request meaning please come in.", "どうぞ入ってください", "Douzo haitte kudasai politely invites the visitor to enter.", c("どうぞ入ってください", "Douzo haitte kudasai"), c("いらっしゃいませ", "Irasshaimase"), c("日本へようこそ", "Nihon e youkoso"), c("ちょっと待ってください", "Chotto matte kudasai")),
            q(19, "HARD", "Restaurant", "meal", "You work at a restaurant and a customer enters. Which welcome is appropriate?", "Use the conventional greeting spoken by shop and restaurant staff.", "いらっしゃいませ", "Irasshaimase is the standard welcome used in shops and restaurants.", c("いらっしゃいませ", "Irasshaimase"), c("おかえりなさい", "Okaeri nasai"), c("どうぞ入ってください", "Douzo haitte kudasai"), c("こんにちは", "Konnichiwa")),
            q(20, "HARD", "Award ceremony", "school", "Your classmate wins a major competition. What is the most fitting response?", "Offer a formal congratulation.", "おめでとうございます", "Omedetou gozaimasu is the polite expression for congratulations.", c("おめでとうございます", "Omedetou gozaimasu"), c("よくできました", "Yoku dekimashita"), c("頑張ってください", "Ganbatte kudasai"), c("お疲れさまでした", "Otsukaresama deshita")),
            q(21, "HARD", "Train platform", "station", "A friend is about to travel alone. What phrase best expresses take care?", "Choose the phrase that warns someone to be careful.", "気をつけてください", "Ki o tsukete kudasai means please be careful or take care.", c("気をつけてください", "Ki o tsukete kudasai"), c("頑張ってください", "Ganbatte kudasai"), c("助けてください", "Tasukete kudasai"), c("待ってください", "Matte kudasai")),
            q(22, "HARD", "Busy hallway", "school", "You need your professor to wait briefly while you get a document. What do you say?", "Ask the person to wait a little.", "ちょっと待ってください", "Chotto matte kudasai politely asks someone to wait a moment.", c("ちょっと待ってください", "Chotto matte kudasai"), c("ゆっくり話してください", "Yukkuri hanashite kudasai"), c("どうぞ", "Douzo"), c("まだです", "Mada desu")),
            q(23, "HARD", "Campus clinic", "school", "A classmate looks unwell. What should you ask?", "Check whether the person is all right.", "大丈夫ですか", "Daijoubu desu ka asks whether someone is all right.", c("大丈夫ですか", "Daijoubu desu ka"), c("お元気ですか", "Ogenki desu ka"), c("元気です", "Genki desu"), c("助けてください", "Tasukete kudasai")),
            q(24, "HARD", "Formal introduction", "office", "After saying Hajimemashite to a new professor, which phrase naturally completes the introduction?", "Use the set phrase that asks for a favorable relationship.", "どうぞよろしくお願いします", "Douzo yoroshiku onegaishimasu naturally follows Hajimemashite in a formal introduction.", c("どうぞよろしくお願いします", "Douzo yoroshiku onegaishimasu"), c("どういたしまして", "Dou itashimashite"), c("おかげさまで", "Okagesama de"), c("もちろんです", "Mochiron desu")),
            q(25, "HARD", "Conversation practice", "classroom", "Your professor asks Ogenki desu ka, and you are well. Which reply is most natural?", "Answer the question about your health directly.", "元気です", "Genki desu means I am fine and directly answers Ogenki desu ka.", c("元気です", "Genki desu"), c("おかげさまで", "Okagesama de"), c("はい", "Hai"), c("大丈夫ですか", "Daijoubu desu ka"))
        ));
    }

    private SituationalQuestion q(int order, String difficulty, String location, String sceneKey,
            String scenario, String hint, String correctAnswer, String explanation,
            ResponseChoice... choices) {
        SituationalQuestion question = new SituationalQuestion();
        question.setGameType("RECOGNITION");
        question.setDifficulty(difficulty);
        question.setOrder(order);
        question.setLocation(location);
        question.setSceneKey(sceneKey);
        question.setScenario(scenario);
        question.setHint(hint);
        question.setCorrectAnswer(correctAnswer);
        question.setExplanation(explanation);
        question.setChoices(List.of(choices));
        question.setActive(true);
        return question;
    }

    private ResponseChoice c(String japanese, String romaji) {
        return new ResponseChoice(japanese, romaji);
    }
}
