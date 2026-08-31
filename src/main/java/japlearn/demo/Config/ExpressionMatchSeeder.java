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
        おはようございます|Ohayou gozaimasu|A student greets a teacher in the morning.|A friend waves goodbye after school.|Think about a respectful morning greeting.
        こんにちは|Konnichiwa|Two neighbors meet during the afternoon.|A family begins eating dinner together.|Use the common daytime greeting.
        こんばんは|Konbanwa|You meet a neighbor in the evening.|You arrive at school early in the morning.|Notice the time of day.
        ありがとうございます|Arigatou gozaimasu|A classmate returns your lost notebook.|A customer asks where the station is.|Show polite gratitude.
        すみません|Sumimasen|You accidentally bump into someone at the station.|Your teammate wins a competition.|Use a brief apology.
        さようなら|Sayounara|A student says goodbye to a teacher after class.|A guest enters a home for the first time.|Use a standard farewell.
        いただきます|Itadakimasu|Everyone is ready to begin a meal.|Everyone has just finished eating.|This is said before eating.
        ごちそうさまでした|Gochisousama deshita|You thank the host after finishing a meal.|You introduce yourself to a new class.|This is said after eating.
        いってきます|Ittekimasu|You tell your family you are leaving home.|You welcome someone who returned home.|The speaker is leaving home.
        いってらっしゃい|Itterasshai|You see a family member off at the door.|You return home and announce your arrival.|Say this to the person leaving.
        ただいま|Tadaima|You return home and greet your family.|You meet someone for the first time.|The speaker has come home.
        おかえりなさい|Okaerinasai|You welcome a family member home.|You ask a shopkeeper for help.|Say this to the person who returned.
        はじめまして|Hajimemashite|You meet a new classmate for the first time.|You congratulate a friend on an award.|Use this at a first meeting.
        よろしくおねがいします|Yoroshiku onegaishimasu|You finish introducing yourself to a new group.|You leave the office before coworkers.|Politely close an introduction.
        おめでとうございます|Omedetou gozaimasu|Your friend has won a school competition.|Your friend is feeling sick.|Celebrate another person's success.
        おつかれさまです|Otsukaresama desu|You greet a teammate after practice.|You enter someone's home as a guest.|Acknowledge someone's effort.
        おねがいします|Onegaishimasu|You politely ask a clerk for assistance.|You refuse an offer because you are fine.|Use a polite request.
        だいじょうぶです|Daijoubu desu|Someone offers help but you are okay.|You interrupt a teacher to ask a question.|Politely say that you are fine.
        しつれいします|Shitsurei shimasu|You enter the teacher's office politely.|You welcome a friend home.|Use this when entering formally.
        またあした|Mata ashita|You will see your classmate again tomorrow.|You meet a manager for the first time.|The next meeting is tomorrow.
        """);

    private static final List<Moment> MEDIUM = rows("""
        お先に失礼します|Osaki ni shitsurei shimasu|You leave work before your coworkers.|You arrive late and keep someone waiting.|Acknowledge coworkers when leaving first.
        お待たせしました|Omatase shimashita|You apologize after making someone wait.|You ask a guest to wait for a moment.|The waiting has just ended.
        かしこまりました|Kashikomarimashita|A staff member formally accepts a customer's request.|A friend casually agrees to play a game.|Choose formal service language.
        少々お待ちください|Shoushou omachi kudasai|A receptionist asks a visitor to wait briefly.|A doctor tells a patient to take care.|Politely request a short wait.
        お気をつけて|Oki o tsukete|You tell a friend to travel safely.|You enter a host's home.|Express care as someone leaves.
        お大事に|Odaiji ni|You speak to someone who is ill.|You greet a coworker after a shift.|Wish an ill person well.
        お邪魔します|Ojama shimasu|You enter a friend's home as a guest.|You finish eating at a restaurant.|Use the customary phrase when entering a home.
        失礼しました|Shitsurei shimashita|You apologize after making a formal mistake.|You welcome a returning family member.|Use a formal past-tense apology.
        どうぞよろしくお願いいたします|Douzo yoroshiku onegai itashimasu|You close a formal self-introduction.|You ask for the restaurant bill.|A formal relationship is beginning.
        申し訳ありません|Moushiwake arimasen|You sincerely apologize to a customer.|You congratulate a close friend.|A stronger formal apology is needed.
        手伝いましょうか|Tetsudaimashou ka|You offer to help someone carrying boxes.|You ask permission to take a photo.|Offer assistance as a question.
        助かりました|Tasukarimashita|You thank someone whose help solved your problem.|You ask someone to repeat a sentence.|Their help made a real difference.
        こちらへどうぞ|Kochira e douzo|A staff member guides a guest to a seat.|A traveler asks for directions.|Invite the person to follow this way.
        もう一度お願いします|Mou ichido onegaishimasu|You politely ask someone to repeat what they said.|You tell a coworker to travel safely.|You need to hear it one more time.
        写真を撮ってもいいですか|Shashin o totte mo ii desu ka|You ask permission before taking a photo.|You announce that you are leaving home.|Ask before using the camera.
        予約しています|Yoyaku shiteimasu|You tell restaurant staff you have a reservation.|You apologize for bumping into someone.|You already arranged a booking.
        お会計をお願いします|Okaikei o onegaishimasu|You politely ask for the bill after a meal.|You introduce yourself to a new group.|The meal is finished and you want to pay.
        ご案内します|Goannai shimasu|A staff member offers to guide a visitor.|A guest thanks the cook after eating.|The speaker will show the way.
        お久しぶりです|Ohisashiburi desu|You meet someone after a long absence.|You meet someone for the first time.|You know them but much time passed.
        では、また|Dewa mata|You end a polite conversation and expect to meet again.|You begin a meal with your host.|Use a polite light farewell.
        """);

    private static final List<Moment> HARD = rows("""
        恐れ入りますが、もう一度お願いします|Osoreirimasu ga, mou ichido onegaishimasu|You ask a senior client to repeat an important point.|You casually ask a close friend to repeat a joke.|Use a deferential request.
        申し訳ございません|Moushiwake gozaimasen|You make a formal apology to a customer.|You thank a friend for returning a pencil.|Use the strongest formal apology.
        承知いたしました|Shouchi itashimashita|You formally confirm a supervisor's instructions.|You casually agree with a sibling.|Confirm understanding formally.
        お手数をおかけします|Otesuu o okake shimasu|You acknowledge that your request creates extra work.|You celebrate a classmate's achievement.|Recognize the burden of your request.
        ご確認いただけますでしょうか|Gokakunin itadakemasu deshou ka|You politely ask a manager to review a document.|You tell a friend to view a funny photo.|Use an indirect respectful request.
        伺ってもよろしいでしょうか|Ukagatte mo yoroshii deshou ka|You respectfully ask whether you may visit.|You ask a close friend where lunch is.|Ask permission very politely.
        お目にかかれて光栄です|Ome ni kakarete kouei desu|You meet a distinguished guest for the first time.|You greet a classmate seen every day.|Express honor at a formal meeting.
        ご配慮いただきありがとうございます|Gohairyo itadaki arigatou gozaimasu|You thank a supervisor for thoughtful consideration.|You say goodbye to a shop clerk.|Thank someone for special consideration.
        先ほどは失礼いたしました|Sakihodo wa shitsurei itashimashita|You formally apologize for your earlier behavior.|You begin a casual call with a friend.|Refer respectfully to an earlier mistake.
        お忙しいところ申し訳ありません|Oisogashii tokoro moushiwake arimasen|You interrupt a busy professor with an important request.|You welcome a family member home.|Acknowledge that they are busy.
        何卒よろしくお願いいたします|Nanitozo yoroshiku onegai itashimasu|You close a very formal request.|You ask a friend to pass the salt.|Use a formal closing.
        ご無沙汰しております|Gobusata shiteorimasu|You greet a senior person after a long absence.|You greet someone met yesterday.|Use the humble phrase for a long absence.
        お招きいただきありがとうございます|Omaneki itadaki arigatou gozaimasu|You thank a host for a formal invitation.|You ask a waiter for the bill.|Express gratitude for being invited.
        どうぞお気遣いなく|Douzo okizukai naku|You politely tell a host not to go to extra trouble.|You warn a traveler about weather.|Politely decline excessive consideration.
        お先に失礼いたします|Osaki ni shitsurei itashimasu|You formally leave before senior colleagues.|You leave a casual picnic with friends.|Use the formal workplace farewell.
        ご足労いただきありがとうございます|Gosokurou itadaki arigatou gozaimasu|You thank a guest for traveling to meet you.|You thank a friend for an eraser.|Acknowledge the effort of coming.
        差し支えなければ、お名前を伺えますか|Sashitsukae nakereba, onamae o ukagaemasu ka|You politely request a visitor's name.|You call a best friend by a nickname.|Soften a sensitive question.
        もう少し詳しくお聞かせください|Mou sukoshi kuwashiku okikase kudasai|You respectfully invite a client to explain further.|You command a sibling to speak faster.|Invite more detail respectfully.
        引き続きよろしくお願いいたします|Hikitsuzuki yoroshiku onegai itashimasu|You close a meeting and request continued cooperation.|You say good night to family.|The professional relationship continues.
        貴重なお時間をいただき、ありがとうございます|Kichou na ojikan o itadaki, arigatou gozaimasu|You thank an executive for meeting with you.|You thank a classmate for a quick greeting.|Recognize the value of their time.
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

    private void seedLevel(int level, String difficulty, String topic, List<Moment> moments) {
        List<SituationalQuestion> existing = repository
            .findByGameTypeIgnoreCaseOrderByOrderAsc("EXPRESSION_MATCH")
            .stream()
            .filter(question -> question.getLevel() == level && question.getSetNumber() == 1)
            .sorted(Comparator.comparingInt(SituationalQuestion::getOrder))
            .toList();

        existing.forEach(question -> {
            boolean changed = false;

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

        for (int index = existing.size(); index < 20; index++) {
            Moment moment = moments.get(index);
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
