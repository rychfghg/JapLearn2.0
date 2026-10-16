package japlearn.demo.Config;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import japlearn.demo.Entity.SituationalQuestion;
import japlearn.demo.Repository.SituationalQuestionRepository;

@Component
public class ExpressionMatchSeeder implements CommandLineRunner {
    private static final String FIRST_IMAGE_URL = "/expression-match/gesture-wave.png";
    private static final String SECOND_IMAGE_URL = "/expression-match/gesture-bow.png";

    private final SituationalQuestionRepository repository;

    public ExpressionMatchSeeder(SituationalQuestionRepository repository) {
        this.repository = repository;
    }

    private record Moment(String japanese, String romaji, String scene, String opposite, String hint) {}

    private static final List<Moment> EASY = rows("""
        おはようございます|Ohayou gozaimasu|A student greets a teacher in the morning.|A friend waves goodbye after school.|A morning greeting to someone you are meeting, not a goodbye after school.
        こんにちは|Konnichiwa|Two neighbors meet during the afternoon.|A family begins eating dinner together.|A daytime greeting when neighbors meet, not a phrase for starting a meal.
        こんばんは|Konbanwa|You meet a neighbor in the evening.|You arrive at school early in the morning.|An evening greeting. Compare the evening visit with the early-morning arrival.
        ありがとうございます|Arigatou gozaimasu|A classmate returns your lost notebook.|A customer asks where the station is.|Thanks for something helpful: returning your notebook. This is not a request for directions.
        すみません|Sumimasen|You accidentally bump into someone at the station.|Your teammate wins a competition.|A brief apology after bumping into someone, not congratulations for winning.
        さようなら|Sayounara|A student says goodbye to a teacher after class.|A guest enters a home for the first time.|A goodbye when parting after class, not a greeting when entering a home.
        いただきます|Itadakimasu|Everyone is ready to begin a meal.|Everyone has just finished eating.|Said just before eating. Check whether the meal is beginning or already finished.
        ごちそうさまでした|Gochisousama deshita|You thank the host after finishing a meal.|You introduce yourself to a new class.|Thanks after a meal is finished, not an introduction to new classmates.
        いってきます|Ittekimasu|You tell your family you are leaving home.|You welcome someone who returned home.|The speaker is leaving home and expects to return; they are not welcoming someone back.
        いってらっしゃい|Itterasshai|You see a family member off at the door.|You return home and announce your arrival.|Spoken by the person staying home to someone leaving, not by the person arriving.
        ただいま|Tadaima|You return home and greet your family.|You meet someone for the first time.|Spoken by the person returning home, not someone meeting a stranger.
        おかえりなさい|Okaerinasai|You welcome a family member home.|You ask a shopkeeper for help.|Spoken to the family member who has returned, not to a shopkeeper for help.
        はじめまして|Hajimemashite|You meet a new classmate for the first time.|You congratulate a friend on an award.|A first introduction. Look for people meeting for the first time, not celebrating an award.
        よろしくおねがいします|Yoroshiku onegaishimasu|You finish introducing yourself to a new group.|You leave the office before coworkers.|A friendly request for a good relationship after introducing yourself, not leaving work.
        おめでとうございます|Omedetou gozaimasu|Your friend has won a school competition.|Your friend is feeling sick.|Congratulations on an achievement, not words to someone who is ill.
        おつかれさまです|Otsukaresama desu|You greet a teammate after practice.|You enter someone's home as a guest.|Acknowledge a teammate’s effort after practice, not your arrival as a houseguest.
        おねがいします|Onegaishimasu|You politely ask a clerk for assistance.|You refuse an offer because you are fine.|A polite request for assistance, not a refusal of help.
        だいじょうぶです|Daijoubu desu|Someone offers help but you are okay.|You interrupt a teacher to ask a question.|Reassure the person offering help that you are okay, not interrupt a teacher.
        しつれいします|Shitsurei shimasu|You enter the teacher's office politely.|You welcome a friend home.|Politely acknowledge entering someone’s office, not welcome a person returning home.
        またあした|Mata ashita|You will see your classmate again tomorrow.|You meet a manager for the first time.|A farewell that mentions seeing the person tomorrow, not a first meeting.
        """);

    private static final List<Moment> MEDIUM = rows("""
        お先に失礼します|Osaki ni shitsurei shimasu|You leave work before your coworkers.|You arrive late and keep someone waiting.|Acknowledge that you are leaving work while others remain, not arriving late.
        お待たせしました|Omatase shimashita|You apologize after making someone wait.|You ask a guest to wait for a moment.|The other person has already waited and you are now ready; you are not asking them to wait.
        かしこまりました|Kashikomarimashita|A staff member formally accepts a customer's request.|A friend casually agrees to play a game.|Respectfully accept a customer’s request as staff, rather than casually agree with a friend.
        少々お待ちください|Shoushou omachi kudasai|A receptionist asks a visitor to wait briefly.|A doctor tells a patient to take care.|Ask a visitor to wait a little longer, not wish a sick person well.
        お気をつけて|Oki o tsukete|You tell a friend to travel safely.|You enter a host's home.|Wish someone safety as they travel, not announce that you are entering a home.
        お大事に|Odaiji ni|You speak to someone who is ill.|You greet a coworker after a shift.|Wish an ill person a good recovery, not thank a coworker for their work.
        お邪魔します|Ojama shimasu|You enter a friend's home as a guest.|You finish eating at a restaurant.|A guest acknowledges entering someone else’s home, not finishing a meal.
        失礼しました|Shitsurei shimashita|You apologize after making a formal mistake.|You welcome a returning family member.|Apologize for an action that already happened, not welcome someone home.
        どうぞよろしくお願いいたします|Douzo yoroshiku onegai itashimasu|You close a formal self-introduction.|You ask for the restaurant bill.|A respectful closing to a self-introduction, not a request to pay for food.
        申し訳ありません|Moushiwake arimasen|You sincerely apologize to a customer.|You congratulate a close friend.|A sincere apology for a customer-facing mistake, not congratulations.
        手伝いましょうか|Tetsudaimashou ka|You offer to help someone carrying boxes.|You ask permission to take a photo.|Offer your own help with a task such as carrying boxes; you are not asking permission.
        助かりました|Tasukarimashita|You thank someone whose help solved your problem.|You ask someone to repeat a sentence.|Recognize that someone’s help solved your problem, not ask them to repeat their words.
        こちらへどうぞ|Kochira e douzo|A staff member guides a guest to a seat.|A traveler asks for directions.|The speaker invites a guest to come this way, rather than asks where to go.
        もう一度お願いします|Mou ichido onegaishimasu|You politely ask someone to repeat what they said.|You tell a coworker to travel safely.|Ask to hear the same words again, not wish someone a safe trip.
        写真を撮ってもいいですか|Shashin o totte mo ii desu ka|You ask permission before taking a photo.|You announce that you are leaving home.|Ask permission before taking a picture, not announce your departure.
        予約しています|Yoyaku shiteimasu|You tell restaurant staff you have a reservation.|You apologize for bumping into someone.|Tell staff about a booking already made, not apologize for an accident.
        お会計をお願いします|Okaikei o onegaishimasu|You politely ask for the bill after a meal.|You introduce yourself to a new group.|Ask to pay after eating, not introduce yourself to a group.
        ご案内します|Goannai shimasu|A staff member offers to guide a visitor.|A guest thanks the cook after eating.|The speaker offers to lead a visitor, not thank the person who cooked.
        お久しぶりです|Ohisashiburi desu|You meet someone after a long absence.|You meet someone for the first time.|You already know this person but have not met for a long time; it is not a first meeting.
        では、また|Dewa mata|You end a polite conversation and expect to meet again.|You begin a meal with your host.|A light, polite goodbye with an expectation of meeting again, not a mealtime phrase.
        """);

    private static final List<Moment> HARD = rows("""
        恐れ入りますが、もう一度お願いします|Osoreirimasu ga, mou ichido onegaishimasu|You ask a senior client to repeat an important point.|You casually ask a close friend to repeat a joke.|Ask for repetition with extra respect toward a senior client, rather than casually ask a friend.
        申し訳ございません|Moushiwake gozaimasen|You make a formal apology to a customer.|You thank a friend for returning a pencil.|A very formal apology to a customer, not thanks for returning an item.
        承知いたしました|Shouchi itashimashita|You formally confirm a supervisor's instructions.|You casually agree with a sibling.|Respectfully confirm that you understand a supervisor’s instructions, not casual agreement at home.
        お手数をおかけします|Otesuu o okake shimasu|You acknowledge that your request creates extra work.|You celebrate a classmate's achievement.|Acknowledge the extra trouble caused by your request, not celebrate someone’s achievement.
        ご確認いただけますでしょうか|Gokakunin itadakemasu deshou ka|You politely ask a manager to review a document.|You tell a friend to view a funny photo.|Respectfully ask a manager to check something, not casually show a friend a funny picture.
        伺ってもよろしいでしょうか|Ukagatte mo yoroshii deshou ka|You respectfully ask whether you may visit.|You ask a close friend where lunch is.|Humbly ask permission to visit someone, not ask a friend where lunch is.
        お目にかかれて光栄です|Ome ni kakarete kouei desu|You meet a distinguished guest for the first time.|You greet a classmate seen every day.|Express that meeting a distinguished guest is an honor, not an everyday classmate greeting.
        ご配慮いただきありがとうございます|Gohairyo itadaki arigatou gozaimasu|You thank a supervisor for thoughtful consideration.|You say goodbye to a shop clerk.|Thank someone for thoughtful consideration of your needs, not say goodbye.
        先ほどは失礼いたしました|Sakihodo wa shitsurei itashimashita|You formally apologize for your earlier behavior.|You begin a casual call with a friend.|Apologize respectfully for something you did earlier, not start a friendly call.
        お忙しいところ申し訳ありません|Oisogashii tokoro moushiwake arimasen|You interrupt a busy professor with an important request.|You welcome a family member home.|Acknowledge interrupting someone who is busy before making your request, not welcome someone home.
        何卒よろしくお願いいたします|Nanitozo yoroshiku onegai itashimasu|You close a very formal request.|You ask a friend to pass the salt.|A very respectful closing asking for cooperation with your request, not a casual request at the table.
        ご無沙汰しております|Gobusata shiteorimasu|You greet a senior person after a long absence.|You greet someone met yesterday.|A humble greeting to someone senior after a long time apart, not someone you saw yesterday.
        お招きいただきありがとうございます|Omaneki itadaki arigatou gozaimasu|You thank a host for a formal invitation.|You ask a waiter for the bill.|Thank your host specifically for inviting you, not request the restaurant bill.
        どうぞお気遣いなく|Douzo okizukai naku|You politely tell a host not to go to extra trouble.|You warn a traveler about weather.|Gently reassure your host that extra effort is unnecessary, not warn someone about weather.
        お先に失礼いたします|Osaki ni shitsurei itashimasu|You formally leave before senior colleagues.|You leave a casual picnic with friends.|A formal workplace farewell when you leave before senior coworkers, not a casual picnic goodbye.
        ご足労いただきありがとうございます|Gosokurou itadaki arigatou gozaimasu|You thank a guest for traveling to meet you.|You thank a friend for an eraser.|Thank someone specifically for making the journey to see you, not lending a small item.
        差し支えなければ、お名前を伺えますか|Sashitsukae nakereba, onamae o ukagaemasu ka|You politely request a visitor's name.|You call a best friend by a nickname.|Ask a visitor’s name politely and indirectly, not call a friend by a name you already know.
        もう少し詳しくお聞かせください|Mou sukoshi kuwashiku okikase kudasai|You respectfully invite a client to explain further.|You command a sibling to speak faster.|Respectfully invite more detail about what was said, not demand faster speech.
        引き続きよろしくお願いいたします|Hikitsuzuki yoroshiku onegai itashimasu|You close a meeting and request continued cooperation.|You say good night to family.|Request continued cooperation after a professional meeting, not say good night at home.
        貴重なお時間をいただき、ありがとうございます|Kichou na ojikan o itadaki, arigatou gozaimasu|You thank an executive for meeting with you.|You thank a classmate for a quick greeting.|Thank someone for giving their valuable time to meet you, not for a passing greeting.
        """);

    private static List<Moment> rows(String source) {
        return source.strip().lines().map(line -> {
            String[] value = line.strip().split("\\|", -1);
            return new Moment(value[0], value[1], value[2], value[3], value[4]);
        }).toList();
    }

    @Override
    public void run(String... args) {
        seedLevel(1, "EASY", "Everyday situations", EASY);
        seedLevel(2, "MEDIUM", "Social situations", MEDIUM);
        seedLevel(3, "HARD", "Formal communication", HARD);
    }

    private static boolean hasMeaningfulText(String text) {
        return text != null && text.codePoints().filter(Character::isLetter).limit(2).count() >= 2;
    }

    private void seedLevel(int level, String difficulty, String topic, List<Moment> moments) {
        List<SituationalQuestion> existing = repository
            .findByGameTypeIgnoreCaseOrderByOrderAsc("EXPRESSION_MATCH")
            .stream()
            .filter(question -> question.getLevel() == level && question.getSetNumber() == 1)
            .sorted(Comparator.comparingInt(SituationalQuestion::getOrder))
            .toList();

        existing.forEach(question -> {
            boolean changed = false;
            Moment authored = moments.stream()
                .filter(row -> row.japanese().equals(question.getCorrectAnswer()))
                .findFirst().orElse(null);
            if (authored != null) {
                if (!hasMeaningfulText(question.getScenario()) || !hasMeaningfulText(question.getSecondaryScenario())
                        || question.getScenario().strip().equalsIgnoreCase(question.getSecondaryScenario().strip())
                        || question.getChoices() == null || question.getChoices().stream()
                            .noneMatch(choice -> choice != null && authored.japanese().equals(choice.getJapanese()))) {
                    question.setScenario(authored.scene());
                    question.setSecondaryScenario(authored.opposite());
                    question.setHint(authored.hint());
                    question.setImageAlt("Scene for: " + authored.scene());
                    question.setSecondaryImageAlt("Scene for: " + authored.opposite());
                    question.setExplanation("This expression fits the situation: " + authored.scene() + " " + authored.hint());
                    Moment alternate = moments.get((moments.indexOf(authored) + 1) % moments.size());
                    question.setChoices(Arrays.asList(
                        new SituationalQuestion.ResponseChoice(authored.japanese(), authored.romaji()),
                        new SituationalQuestion.ResponseChoice(alternate.japanese(), alternate.romaji())));
                    changed = true;
                }
                // Preserve teacher-authored contexts; refresh only built-in matching exchanges.
                if (authored.scene().equals(question.getScenario())
                        && authored.opposite().equals(question.getSecondaryScenario())
                        && !authored.hint().equals(question.getHint())) {
                    question.setHint(authored.hint());
                    changed = true;
                }
            }

            if (question.getImageUrl() == null || question.getImageUrl().isBlank()) {
                question.setImageUrl(FIRST_IMAGE_URL);
                changed = true;
            }
            if (question.getSecondaryImageUrl() == null
                    || question.getSecondaryImageUrl().isBlank()) {
                question.setSecondaryImageUrl(SECOND_IMAGE_URL);
                changed = true;
            }

            if ("Everyday gestures".equals(question.getTopic())) {
                question.setTopic("Everyday situations");
                question.setLocation("Everyday situations");
                changed = true;
            }
            if (question.getImageAlt() != null && question.getImageAlt().startsWith("Gesture for:")) {
                question.setImageAlt("Scene for:" + question.getImageAlt().substring("Gesture for:".length()));
                changed = true;
            }
            if (question.getSecondaryImageAlt() != null
                    && question.getSecondaryImageAlt().startsWith("Gesture for:")) {
                question.setSecondaryImageAlt(
                    "Scene for:" + question.getSecondaryImageAlt().substring("Gesture for:".length())
                );
                changed = true;
            }
            if ("The expression fits the first scene more naturally.".equals(question.getExplanation())) {
                question.setExplanation(
                    "This Japanese expression fits the first situation more naturally."
                );
                changed = true;
            }
            if (changed) {
                repository.save(question);
            }
        });

        int order = existing.stream().mapToInt(SituationalQuestion::getOrder).max().orElse(level * 100);

        // Count unique authored expressions, not raw rows: legacy duplicates must
        // not prevent the missing questions from being seeded.
        for (int index = 0; index < moments.size(); index++) {
            Moment moment = moments.get(index);
            if (existing.stream().anyMatch(question -> moment.japanese().equals(question.getCorrectAnswer()))) continue;
            Moment alternate = moments.get((index + 1) % moments.size());
            SituationalQuestion question = new SituationalQuestion();
            question.setGameType("EXPRESSION_MATCH");
            question.setDifficulty(difficulty);
            question.setLevel(level);
            question.setSetNumber(1);
            question.setTopic(topic);
            question.setOrder(++order);
            question.setLocation(topic);
            question.setSceneKey("expression-match-" + difficulty.toLowerCase() + "-" + (index + 1));
            question.setScenario(moment.scene());
            question.setSecondaryScenario(moment.opposite());
            question.setImageUrl(FIRST_IMAGE_URL);
            question.setSecondaryImageUrl(SECOND_IMAGE_URL);
            question.setImageAlt("Scene for: " + moment.scene());
            question.setSecondaryImageAlt("Scene for: " + moment.opposite());
            question.setHint(moment.hint());
            question.setCorrectAnswer(moment.japanese());
            question.setExplanation("This Japanese expression fits the first situation more naturally.");
            question.setChoices(Arrays.asList(
                new SituationalQuestion.ResponseChoice(moment.japanese(), moment.romaji()),
                new SituationalQuestion.ResponseChoice(alternate.japanese(), alternate.romaji())
            ));
            question.setActive(true);
            repository.save(question);
        }
    }
}
