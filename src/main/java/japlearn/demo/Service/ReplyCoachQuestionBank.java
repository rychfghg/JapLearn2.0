package japlearn.demo.Service;

import java.util.*;
import org.springframework.stereotype.Component;

/** Instructor-reviewable pilot bank. Readiness items are distinct from the teaching examples. */
@Component
public class ReplyCoachQuestionBank {
    public record Option(String id, String japanese, String romaji, String meaning) {}
    public record Question(String id, String skill, String prompt, List<Option> choices, String correctId, String explanation, int practiceNumber) {}
    private static Question question(String id, String skill, String prompt, String jp, String romaji, String meaning,
            String otherJp, String otherRomaji, String otherMeaning, String explanation, int practiceNumber) {
        boolean flip = id.hashCode() % 2 == 0;
        String correct = flip ? "y" : "x";
        return new Question(id, skill, prompt, List.of(new Option(correct, jp, romaji, meaning),
            new Option(flip ? "x" : "y", otherJp, otherRomaji, otherMeaning)), correct, explanation, practiceNumber);
    }
    public static final List<Question> PRACTICE = List.of(
        question("practice-1", "practice-1", "Friends meet at school. Greet them warmly.", "おはよう！", "Ohayou!", "Good morning!", "いただきます。", "Itadakimasu.", "Before eating.", "A greeting fits meeting friends; itadakimasu belongs before a meal.", 1),
        question("practice-2", "practice-2", "Ask a station employee where the exit is.", "すみません、出口はどこですか。", "Sumimasen, deguchi wa doko desu ka.", "Excuse me, where is the exit?", "早く教えて。", "Hayaku oshiete.", "Tell me quickly.", "Sumimasen gets attention politely; the question explains what you need.", 2),
        question("practice-3", "practice-3", "People are leaving a bus. What should you do before boarding?", "降りる人を待ちます。", "Oriru hito o machimasu.", "Wait for people getting off.", "押して入ります。", "Oshite hairimasu.", "Push inside.", "Let people leave before you board, just as on a train.", 3),
        question("practice-4", "practice-4", "Your phone rings in a quiet library. What should you do?", "マナーモードにします。", "Manaa moodo ni shimasu.", "Switch to silent mode.", "大声で話します。", "Oogoe de hanashimasu.", "Speak loudly.", "Silent mode avoids disturbing people in a shared quiet space.", 4),
        question("practice-5", "practice-5", "You enter a temple courtyard. Choose a respectful action.", "静かに歩きます。", "Shizuka ni arukimasu.", "Walk quietly.", "大声で叫びます。", "Oogoe de sakebimasu.", "Shout loudly.", "Quiet behavior shows consideration for the place and other visitors.", 5),
        question("practice-6", "practice-6", "A bakery clerk says irasshaimase. Choose a suitable action.", "軽く会釈します。", "Karuku eshaku shimasu.", "Give a small nod.", "いらっしゃいませ。", "Irasshaimase.", "Welcome to the store.", "Irasshaimase welcomes customers; you do not need to welcome the clerk back.", 6),
        question("practice-7", "practice-7", "Hand a ticket to a staff member for checking.", "お願いします。", "Onegaishimasu.", "Please.", "早くして。", "Hayaku shite.", "Hurry up.", "Onegaishimasu politely accompanies a request; hayaku shite sounds demanding here.", 7),
        question("practice-8", "practice-8", "A restaurant employee asks your party size. There are two of you.", "二人です。", "Futari desu.", "Two people.", "三人です。", "Sannin desu.", "Three people.", "Futari desu gives the requested number accurately and politely.", 8),
        question("practice-9", "practice-9", "Politely order water from a server.", "水をお願いします。", "Mizu o onegaishimasu.", "Water, please.", "早く持ってきて。", "Hayaku motte kite.", "Bring it quickly.", "Naming the item and adding onegaishimasu makes the request clear and polite.", 9),
        question("practice-10", "practice-10", "Breakfast is served, and you have not started eating.", "いただきます。", "Itadakimasu.", "Said before eating.", "ごちそうさまでした。", "Gochisousama deshita.", "Thank you for the meal.", "Itadakimasu is used before eating; gochisousama deshita comes after.", 10),
        question("practice-11", "practice-11", "You pause during dinner. Where should you put your chopsticks?", "箸置きに置きます。", "Hashioki ni okimasu.", "On the chopstick rest.", "ご飯に立てます。", "Gohan ni tatemasu.", "Upright in the rice.", "Use the rest; standing chopsticks upright in rice has funeral associations.", 11),
        question("practice-12", "practice-12", "You have finished dinner at a host family’s home.", "ごちそうさまでした。", "Gochisousama deshita.", "Thank you for the meal.", "いただきます。", "Itadakimasu.", "Said before eating.", "Thank the people who provided the meal after you finish.", 12),
        question("practice-13", "practice-13", "Greet your teacher when you arrive at school in the morning.", "おはようございます。", "Ohayou gozaimasu.", "Good morning, politely.", "元気？", "Genki?", "You good?", "Ohayou gozaimasu is a polite morning greeting suitable for your teacher.", 13),
        question("practice-14", "practice-14", "An unfamiliar adult gives you a souvenir. Thank them.", "ありがとうございます。", "Arigatou gozaimasu.", "Thank you very much.", "これだけ？", "Kore dake?", "Is this all?", "Express gratitude rather than questioning the amount of the gift.", 14),
        question("practice-15", "practice-15", "A classmate’s family invites you inside their house.", "おじゃまします。", "Ojama shimasu.", "Excuse me for intruding.", "ただいま。", "Tadaima.", "I’m home.", "Ojama shimasu is for entering as a guest; tadaima announces your return home.", 15),
        question("practice-16", "practice-16", "You have removed your shoes at a friend’s entrance.", "靴をそろえます。", "Kutsu o soroemasu.", "Arrange the shoes neatly.", "靴を散らかします。", "Kutsu o chirakashimasu.", "Leave the shoes scattered.", "Neat shoes keep the entrance usable and show care for the host’s space.", 16),
        question("practice-17", "practice-17", "Your host offers you water. Accept it with thanks.", "ありがとうございます。", "Arigatou gozaimasu.", "Thank you very much.", "水。", "Mizu.", "Water.", "Thanking the host acknowledges their effort; naming the drink alone does not.", 17),
        question("practice-18", "practice-18", "You accidentally knock a host’s book onto the floor.", "すみません。拾います。", "Sumimasen. Hiroimasu.", "I’m sorry. I’ll pick it up.", "本が悪い。", "Hon ga warui.", "The book is at fault.", "Apologize and offer to put things right instead of blaming an object.", 18),
        question("practice-19", "practice-19", "An older passenger asks you to move your bag.", "はい、わかりました。", "Hai, wakarimashita.", "Yes, understood.", "自分でやって。", "Jibun de yatte.", "Do it yourself.", "A respectful acknowledgment fits a reasonable request from someone you do not know.", 19),
        question("practice-20", "practice-20", "Thank a friend after studying together and say goodbye.", "今日はありがとう。またね！", "Kyou wa arigatou. Mata ne!", "Thanks for today. See you!", "やっと帰れる。", "Yatto kaereru.", "Finally, I can leave.", "Thanks and a friendly goodbye acknowledge the time you spent together.", 20));
    public static final List<Question> READINESS = List.of(
        question("ready-1", "greetings", "You meet a friend in the afternoon. Greet them.", "こんにちは！", "Konnichiwa!", "Hello!", "おやすみ。", "Oyasumi.", "Good night.", "Use a daytime greeting when meeting someone in the afternoon.", 1),
        question("ready-2", "greetings", "You meet a friend in the evening. Greet them.", "こんばんは！", "Konbanwa!", "Good evening!", "いただきます。", "Itadakimasu.", "Said before eating.", "Konbanwa greets someone in the evening.", 1),
        question("ready-3", "asking-directions", "Ask a staff member where the restroom is.", "すみません、トイレはどこですか。", "Sumimasen, toire wa doko desu ka.", "Excuse me, where is the restroom?", "早く教えて。", "Hayaku oshiete.", "Tell me quickly.", "Politely get attention, then state what you need to find.", 2),
        question("ready-4", "asking-directions", "Ask a shop employee where the station is.", "すみません、駅はどこですか。", "Sumimasen, eki wa doko desu ka.", "Excuse me, where is the station?", "駅！", "Eki!", "Station!", "A full polite question makes the request clear.", 2),
        question("ready-5", "party-size", "A server asks your party size. There are four people.", "四人です。", "Yonin desu.", "Four people.", "二人です。", "Futari desu.", "Two people.", "Give the number requested and use desu.", 8),
        question("ready-6", "party-size", "A server asks your party size. You are alone.", "一人です。", "Hitori desu.", "One person.", "三人です。", "Sannin desu.", "Three people.", "Hitori means one person; the answer must match the situation.", 8),
        question("ready-7", "ordering", "Politely order tea from a server.", "お茶をお願いします。", "Ocha o onegaishimasu.", "Tea, please.", "早くして。", "Hayaku shite.", "Hurry up.", "Name the item and add a polite request.", 9),
        question("ready-8", "ordering", "Politely order coffee at a cafe.", "コーヒーをお願いします。", "Koohii o onegaishimasu.", "Coffee, please.", "いらない。", "Iranai.", "I do not want it.", "Onegaishimasu makes the order polite; iranai refuses something.", 9),
        question("ready-9", "before-meal", "Your host serves lunch. You are about to take the first bite.", "いただきます。", "Itadakimasu.", "Said before eating.", "ごちそうさまでした。", "Gochisousama deshita.", "Thank you for the meal.", "Itadakimasu belongs before the meal starts.", 10),
        question("ready-10", "before-meal", "You unwrap your packed lunch and are ready to eat.", "いただきます。", "Itadakimasu.", "Said before eating.", "おやすみなさい。", "Oyasuminasai.", "Good night.", "Use the expression for beginning a meal, not going to bed.", 10),
        question("ready-11", "after-meal", "You finish breakfast prepared by your host.", "ごちそうさまでした。", "Gochisousama deshita.", "Thank you for the meal.", "いただきます。", "Itadakimasu.", "Said before eating.", "Gochisousama deshita expresses gratitude after eating.", 12),
        question("ready-12", "after-meal", "You finish a meal at a small restaurant. Thank the staff as you leave.", "ごちそうさまでした。", "Gochisousama deshita.", "Thank you for the meal.", "おはようございます。", "Ohayou gozaimasu.", "Good morning.", "Use the expression thanking people after a meal.", 12),
        question("ready-13", "teacher-greeting", "You meet your teacher in the afternoon. Greet them.", "こんにちは、先生。", "Konnichiwa, sensei.", "Hello, teacher.", "おい！", "Oi!", "Hey!", "A respectful greeting fits your relationship with a teacher.", 13),
        question("ready-14", "teacher-greeting", "You greet your instructor at an evening class.", "こんばんは、先生。", "Konbanwa, sensei.", "Good evening, teacher.", "元気？", "Genki?", "You good?", "Use a respectful greeting suited to the time and relationship.", 13),
        question("ready-15", "gratitude", "A stranger returns your dropped wallet. Thank them.", "ありがとうございます。", "Arigatou gozaimasu.", "Thank you very much.", "これだけ？", "Kore dake?", "Is this all?", "A polite thanks acknowledges the person’s help.", 14),
        question("ready-16", "gratitude", "A staff member helps carry your bag. Thank them politely.", "ありがとうございます。", "Arigatou gozaimasu.", "Thank you very much.", "何？", "Nani?", "What?", "Acknowledge the help with thanks, not an abrupt question.", 14),
        question("ready-17", "guest-entry", "Your friend’s parents invite you into their apartment.", "おじゃまします。", "Ojama shimasu.", "Excuse me for intruding.", "ただいま。", "Tadaima.", "I am home.", "Use ojama shimasu when entering someone else’s home as a guest.", 15),
        question("ready-18", "guest-entry", "You enter a host family’s house for a study visit.", "おじゃまします。", "Ojama shimasu.", "Excuse me for intruding.", "いってきます。", "Ittekimasu.", "I am leaving.", "You are entering as a guest, not announcing that you are leaving.", 15),
        question("ready-19", "apology", "You bump into an unfamiliar adult. Apologize politely.", "すみません。", "Sumimasen.", "I am sorry.", "あなたが悪い。", "Anata ga warui.", "It is your fault.", "A polite apology takes responsibility instead of blaming the other person.", 18),
        question("ready-20", "apology", "You accidentally step on someone’s foot. Apologize.", "すみません。", "Sumimasen.", "I am sorry.", "おめでとう。", "Omedetou.", "Congratulations.", "An apology fits an accident; congratulations does not.", 18));
    public Question get(String id) {
        return java.util.stream.Stream.concat(PRACTICE.stream(), READINESS.stream()).filter(q -> q.id().equals(id))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown question."));
    }
    public List<String> select(List<String> previous) {
        Map<String, List<Question>> skills = new LinkedHashMap<>();
        READINESS.forEach(q -> skills.computeIfAbsent(q.skill(), key -> new ArrayList<>()).add(q));
        List<String> ids = new ArrayList<>();
        for (List<Question> options : skills.values()) {
            List<Question> unseen = options.stream().filter(q -> !previous.contains(q.id())).toList();
            List<Question> available = unseen.isEmpty() ? options : unseen;
            ids.add(available.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(available.size())).id());
        }
        Collections.shuffle(ids);
        return ids;
    }
}

