package japlearn.demo.Config;

import java.time.Instant;
import java.util.*;
import japlearn.demo.Entity.ReplyCoachChapter;

/** Additive stock-content migration: stable IDs, choice text, audio and scores are retained. */
public final class ReplyCoachContentRevision {
    private static final String[] JAPANESE = {
        "やっと来たね！",
        "浅草は何番線かな。",
        "先に降りてもらおう。",
        "電車では静かにしよう。",
        "入る前に軽くお辞儀をするよ。",
        "長く答えなくても大丈夫。",
        "トレーに置こう。",
        "何名様ですか、と聞かれたよ。",
        "丁寧な言い方をつけよう。",
        "食べる前の言葉だよ。",
        "箸をご飯に立てないでね。",
        "今度は食べた後の言葉だね。",
        "山本先生です。丁寧に話そう。",
        "できれば両手で受け取ろう。",
        "玄関で靴を脱ぐよ。",
        "帰る時に履きやすいね。",
        "短いお礼で大丈夫。",
        "すぐ謝って手伝おう。",
        "丁寧に返事をしよう。",
        "また来週会おう。"
    };
    private static final String[] OLD_SCENES = {
        "Just after eight in the morning, the city is already moving around you. Outside the station, Sumi and Haru wave from beside the ticket gate. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. Haru notices a station employee helping travelers near the map. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "A few minutes later, the three of you reach the platform as another wave of commuters gathers beside the marked lines. A crowded train arrives, and passengers form orderly lines beside the doors. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. A phone begins ringing in the quiet carriage. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. The route passes a small temple where visitors pause at the gate. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. You stop at a small shop. The clerk welcomes you with a cheerful greeting. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. The clerk places your purchase in a tray and states the total. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "By lunchtime, the crowded streets give way to the warm entrance of a neighborhood restaurant. At lunch, the three of you enter a small family restaurant. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. The server waits beside the table while everyone looks at the menu. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. The meals arrive together, filling the table with steam and color. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. You pause with your chopsticks while deciding where to place them. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. Everyone finishes eating, and the server begins clearing nearby tables. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. Outside a community center, Sumi unexpectedly meets her former teacher. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "In the afternoon, your journey becomes more personal when your companions bring you to a host family's home. At a host family's doorway, the host offers you a small wrapped welcome gift. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. The host opens the door and invites everyone inside. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. You remove your shoes and notice everyone turning theirs toward the door. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. The host pours tea and offers the cup toward you. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. Your sleeve catches the cup, and a few drops spill onto the table. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "As the day continues, the next social moment arrives naturally. On the way out, an older neighbor asks whether you can move aside for a bicycle. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next.",
        "Night settles over Tokyo as the three of you return to the station where the day began. Back at the station, the day is ending and your companions must head home. Sumi and Haru pause long enough for you to notice how the setting and relationship shape what happens next."
    };
    private static final String[] SCENES = {
        "Outside the station, Sumi and Haru wave from beside the ticket gate.",
        "Haru notices a station employee helping travelers near the map.",
        "A crowded train arrives, and passengers form orderly lines beside the doors.",
        "A phone begins ringing in the quiet carriage.",
        "The route passes a small temple where visitors pause at the gate.",
        "You stop at a small shop. The clerk welcomes you with a cheerful greeting.",
        "The clerk places your purchase in a tray and states the total.",
        "At lunch, the three of you enter a small family restaurant.",
        "The server waits beside the table while everyone looks at the menu.",
        "The meals arrive together, filling the table with steam and color.",
        "You pause with your chopsticks while deciding where to place them.",
        "Everyone finishes eating, and the server begins clearing nearby tables.",
        "Outside a community center, Sumi unexpectedly meets her former teacher.",
        "At a host family's doorway, the host offers you a small wrapped welcome gift.",
        "The host opens the door and invites everyone inside.",
        "You remove your shoes and notice everyone turning theirs toward the door.",
        "The host pours tea and offers the cup toward you.",
        "Your sleeve catches the cup, and a few drops spill onto the table.",
        "On the way out, an older neighbor asks whether you can move aside for a bicycle.",
        "Back at the station, the day is ending and your companions must head home."
    };
    private ReplyCoachContentRevision() {}
    private static final String[] OLD_MEANINGS = {
        "You made it! How was the trip?",
        "We should ask which platform reaches Asakusa.",
        "Everyone is waiting for people to exit first.",
        "People usually keep calls quiet on the train.",
        "A small bow shows respect before entering.",
        "The clerk said irasshaimase. You do not need a long reply.",
        "Place the money or card on the tray rather than tossing it across the counter.",
        "The staff asks how many people are in your group.",
        "Pointing is fine, but add a polite phrase.",
        "There is a phrase people often say before eating.",
        "Do not stick chopsticks upright in rice.",
        "This is the time for the phrase we did not use before eating.",
        "This is Yamamoto-sensei. We should speak more formally.",
        "Receive it with both hands when practical.",
        "The entryway is where outdoor shoes come off.",
        "That makes them easy to put on when leaving.",
        "A short expression of thanks is enough.",
        "Apologize immediately and offer to help.",
        "Use a respectful reply to acknowledge the request.",
        "We will meet again next weekend."
    };
    private static final String[] MEANINGS = {
        "You finally made it!",
        "I wonder which platform is for Asakusa.",
        "Let's let people get off first.",
        "Let's be quiet on the train.",
        "We give a small bow before entering.",
        "You don't need to give a long reply.",
        "Let's put it on the tray.",
        "They asked, 'How many people?'",
        "Let's add a polite expression.",
        "It's the phrase used before eating.",
        "Don't stand your chopsticks upright in the rice.",
        "This time, it's the phrase used after eating.",
        "This is Yamamoto-sensei. Let's speak politely.",
        "Let's use both hands to receive it if we can.",
        "We take our shoes off at the entrance.",
        "That makes them easy to put on when it's time to leave.",
        "A short thank-you is enough.",
        "Let's apologize right away and help.",
        "Let's reply politely.",
        "Let's meet again next week."
    };
    private static final String[] OLD_PROMPTS = {
        "How do you greet your friends?",
        "How do you ask the employee?",
        "What should you do?",
        "How do you handle your phone?",
        "What do you do at the entrance?",
        "How do you respond?",
        "What do you say while paying?",
        "How do you answer?",
        "How do you order?",
        "What do you say?",
        "Where should the chopsticks rest?",
        "What do you say?",
        "How do you greet the teacher?",
        "What do you say?",
        "What do you say as a guest entering?",
        "What should you do?",
        "How do you accept the tea?",
        "What do you say?",
        "How do you respond?",
        "How do you close the day?"
    };
    private static final String[] PROMPTS = {
        "Sumi and Haru have come to welcome you. Choose a warm greeting for your friends.",
        "Ask the station employee which platform goes to Asakusa.",
        "Passengers are getting off your train. Choose what you will do before boarding.",
        "Your phone rings in the quiet carriage. Choose how you will avoid disturbing other passengers.",
        "At the temple gate, choose a respectful action before entering.",
        "The shop clerk says 'irasshaimase' to welcome you. Choose a suitable response as the customer.",
        "You place your payment on the tray. Choose a polite phrase to say to the clerk.",
        "The server asks how many people are in your group. There are three of you: you, Sumi, and Haru.",
        "Point to the dish you want and choose a polite request to the server.",
        "Lunch has arrived, and nobody has started eating. Choose what to say before your first bite.",
        "You are pausing during the meal. Choose where to put your chopsticks.",
        "You have finished lunch. Choose the customary phrase to thank the people who provided the meal.",
        "You recognize Yamamoto-sensei from an earlier online course and have not seen the teacher for a while. Choose a respectful greeting.",
        "Your host offers you a welcome gift. Choose a reply that expresses gratitude.",
        "The host invites you inside. Choose what to say as you enter someone else's home.",
        "You have removed your shoes at the entrance. Choose how to leave them for your host.",
        "Your host offers you tea. Choose a polite way to accept it and express thanks.",
        "You spill tea on the host's table. Choose an apology that also offers to put things right.",
        "An older neighbor asks you to make room for a bicycle. Choose a respectful acknowledgment.",
        "Your friends say you will meet again next week. Thank them for today and say goodbye."
    };
    private static final String[] HINTS = {
        "Your friends made time to meet you. Look for a greeting that expresses pleasure at seeing them, not a complaint about being late.",
        "You need a platform number, not just the location of Asakusa. Compare which question asks for that number and politely gets the employee's attention.",
        "People need space to leave before you can enter. Compare waiting or making room with boarding immediately or pushing.",
        "The carriage is quiet. Think about preventing noise altogether, rather than deciding how loudly to take a call.",
        "Look for a respectful entrance gesture. Waving is for greeting a person; sitting on the gate is not an entrance custom.",
        "Irasshaimase is the clerk welcoming a customer. You are not the shop employee, so you do not need to welcome the clerk back.",
        "You are handing over payment, not asking for an object or telling the clerk to hurry. Choose wording that politely accompanies the transaction.",
        "Count you, Sumi, and Haru. Look for that number followed by a polite ending; both nin and mei can count people.",
        "You need to request the dish politely. A bare 'this' only identifies it, and telling a server to hurry adds an unnecessary demand.",
        "The meal is about to begin. Distinguish the expression used before eating from the one used to thank people after eating.",
        "Think about a safe resting place for the chopsticks. Upright chopsticks in rice and pointing them at people are not resting positions.",
        "The meal is already over. Choose a closing expression of thanks, not the phrase used before the first bite.",
        "This is a teacher you already know, not a new friend. Look for polite language that fits meeting again after some time.",
        "The host is giving you something. Compare thanking them with merely saying you will take it or questioning whether there is more.",
        "You are a visitor entering the host's space. Distinguish a guest's entrance phrase from the phrase used when returning to your own home.",
        "The shoes are already off. Look for a neat arrangement that leaves the entrance usable and makes departure easy.",
        "The host has served you a drink. Naming the drink alone does not acknowledge that effort; think about thanks when accepting it.",
        "You caused the spill. A strong reply takes responsibility and offers to clean up, rather than blaming the table.",
        "The neighbor needs room to pass. Choose a polite acknowledgment instead of asking them to manage without your help.",
        "You plan to see these friends again. Choose a friendly closing that acknowledges their help today and the next meeting."
    };
    private static final String[] TRANSITIONS = {
        "After greeting your friends, you walk together to the station map to plan the trip to Asakusa.",
        "With the platform information checked, the three of you join the boarding line.",
        "Once the passengers have stepped off, you board together and find space in the carriage.",
        "After the train ride, you walk toward a small temple on the route through Asakusa.",
        "After visiting the temple, you stop at a nearby shop.",
        "You browse the shop together and bring your purchase to the checkout.",
        "With the purchase paid for, you look for somewhere to have lunch.",
        "The server leads your group of three to a table and brings the menus.",
        "After you place your orders, the meals arrive at the table.",
        "During lunch, you pause and look for a place to rest your chopsticks.",
        "You continue eating together. Soon, everyone has finished lunch.",
        "After leaving the restaurant, you recognize Yamamoto-sensei from an earlier online course.",
        "After greeting the teacher, you continue to the host family's home for an afternoon visit.",
        "The host finishes giving you the welcome gift and invites everyone inside.",
        "At the entrance, you remove your shoes before stepping into the living area.",
        "Once the shoes are arranged, your host invites you to sit and offers tea.",
        "As you reach for your tea, your sleeve catches the cup and a little spills onto the table.",
        "After the spill is dealt with, you thank the host and head outside, where a neighbor needs room to pass.",
        "You make room for the bicycle, then walk with Sumi and Haru back to the station.",
        "Your friends prepare to catch their trains. The day together comes to a close."
    };
    private static final String[] ACCEPTABLE = {
        "A general hello is a friendly greeting here, even though it says less about being happy to see your friends.",
        "This politely asks where Asakusa is, but the employee still has to infer that you need a platform number.",
        "Waiting at the back of the line is considerate; letting people exit is the immediate action needed at the doors.",
        "Speaking quietly reduces noise, but avoiding the call or switching to silent mode is more considerate in this carriage.",
        "Entering quietly is respectful, though it does not include the entrance gesture Sumi mentioned.",
        "A polite hello can acknowledge the clerk; repeating the shop's welcome is not necessary.",
        "This is also a polite phrase to use when handing over payment.",
        "Sanmei desu is also a correct, polite way to say your party has three people.",
        "Kore kudasai is also a polite request for the item you point to.",
        "Tabemashou invites everyone to eat, although it is not the customary expression of gratitude before eating.",
        "The plate edge can be an alternative resting place when there is no chopstick rest.",
        "Saying the meal was delicious conveys appreciation, though it is not the customary closing phrase.",
        "A polite hello addresses the teacher appropriately, even without mentioning the long time since you last met.",
        "A polite thank-you is sufficient to express gratitude for the gift.",
        "Shitsurei shimasu is polite, while ojama shimasu is the phrase particularly associated with visiting a home.",
        "Putting shoes neatly to the side keeps the entrance clear, even without turning them toward the exit.",
        "A polite thank-you is a suitable way to accept the tea.",
        "This apologizes and checks whether things are okay, but does not yet offer to wipe up the spill.",
        "This politely acknowledges the neighbor's request.",
        "This is a friendly goodbye and reminder to take care, even without thanking them for today."
    };
    private static final String[] AWKWARD = {
        "Itadakimasu belongs before a meal; you are greeting friends at a station.",
        "Saying only Asakusa does not ask which platform you need.",
        "Boarding immediately does not give passengers space to get off.",
        "Taking the call normally ignores the quiet shared setting.",
        "Waving greets a person; the question asks about respect at the temple entrance.",
        "Irasshaimase is the shop employee's welcome, not the customer's reply.",
        "Choudai asks someone to give you something; it does not politely accompany payment here.",
        "The number is correct, but adding desu makes the response more appropriate for the server.",
        "A bare kore identifies the item but leaves out the polite request.",
        "Gochisousama is used after the meal; you have not started eating yet.",
        "Standing chopsticks upright in rice has funeral associations and is not a resting position.",
        "Itadakimasu starts a meal; the question asks what to say after finishing.",
        "The casual hisashiburi is less appropriate for greeting a teacher than its polite form.",
        "Moraimasu says that you will take it, but does not express thanks for the gift.",
        "Tadaima announces a return to your own home; you are entering as a guest.",
        "Leaving the shoes scattered makes the host's entrance harder to use.",
        "Saying only ocha names the tea without thanking the host.",
        "Gomen is a casual apology; this unfamiliar host and the spill call for a more polite apology.",
        "Asking the neighbor to wait delays a simple request you can acknowledge politely.",
        "Sayounara can sound more final or distant than a casual goodbye to friends you will meet next week."
    };
    private static final String[] IMPOLITE = {
        "Telling the friends 'You're late' is a complaint, not the warm greeting requested.",
        "Tell me quickly puts pressure on an unfamiliar employee instead of making a polite request.",
        "Pushing into the train disregards the passengers who need room to leave.",
        "Speaking loudly disturbs the quiet carriage.",
        "Sitting on the gate treats the entrance as a seat rather than showing respect.",
        "An abrupt 'What?' can sound dismissive toward the clerk's welcome.",
        "Telling the clerk to hurry makes a demand instead of accompanying payment politely.",
        "You can see that, can't you? challenges the server instead of answering the routine question.",
        "Telling the server to bring it quickly is a demand rather than a polite order.",
        "Suggesting the food may taste bad before eating can offend the people who prepared it.",
        "Pointing chopsticks at someone is not a resting action and can make them uncomfortable.",
        "I've had enough can sound dismissive here; it does not thank anyone for the meal.",
        "A casual 'You good?' does not fit the respectful greeting requested for your teacher.",
        "Asking 'Is this all?' questions the amount instead of appreciating the host's gift.",
        "I'm coming in announces your action without the polite acknowledgment expected from a guest.",
        "Wearing outdoor shoes inside ignores the entryway custom the host has shown you.",
        "An abrupt 'I don't want it' rejects the tea instead of politely accepting it as requested.",
        "Blaming the table avoids responsibility for the spill you caused.",
        "Telling the neighbor to get through by themselves dismisses their request for space.",
        "Finally, I can leave suggests you did not enjoy their company instead of thanking them."
    };
    public static boolean apply(ReplyCoachChapter chapter) {
        if (!"A Day of First Impressions".equals(chapter.getTitle()) || chapter.getStoryVersion() >= 3 || chapter.getNodes() == null) return false;
        Map<String, ReplyCoachChapter.StoryNode> nodes = new HashMap<>();
        chapter.getNodes().forEach(n -> nodes.put(n.getId(), n));
        if (!nodes.containsKey("decision-1") || !nodes.containsKey("dialogue-1-companion")) return false;
        for (int i = 0; i < 20; i++) {
            int number = i + 1;
            var scene = nodes.get("scene-" + number);
            if (scene != null && OLD_SCENES[i].equals(scene.getText())) scene.setText(SCENES[i]);
            var companion = nodes.get("dialogue-" + number + "-companion");
            boolean stockCompanion = companion != null && companion.getText() != null
                && (number % 2 == 0 ? "そうだね。あなたなら、どう答える？" : "一緒に考えよう。あなたは何と言う？").equals(companion.getJapanese())
                && (companion.getText().endsWith("looks toward you and brings you into the conversation.")
                    || companion.getText().equals("That's right. How would you answer?")
                    || companion.getText().equals("Let's think together. What would you say?"));
            var line = nodes.get("dialogue-" + number);
            if (line != null) {
                if (OLD_MEANINGS[i].equals(line.getText()) && JAPANESE[i].equals(line.getJapanese())) line.setText(MEANINGS[i]);
                // Remove only the stock repeated coaching interjection from the normal route.
                if (stockCompanion && ("dialogue-" + number + "-companion").equals(line.getNextNodeId())) line.setNextNodeId("decision-" + number);
            }
            if (stockCompanion && companion.getText().endsWith("looks toward you and brings you into the conversation."))
                companion.setText(number % 2 == 0 ? "That's right. How would you answer?" : "Let's think together. What would you say?");
            var decision = nodes.get("decision-" + number);
            if (decision != null) {
                if (OLD_PROMPTS[i].equals(decision.getText())) decision.setText(PROMPTS[i]);
                if (decision.getHint() == null || decision.getHint().equals(legacyHint(number))) decision.setHint(HINTS[i]);
                for (var choice : decision.getChoices() == null ? List.<ReplyCoachChapter.ChoiceOption>of() : decision.getChoices()) {
                    String explanation = choice.getExplanation();
                    if (explanation == null || !GENERIC.contains(explanation)) continue;
                    choice.setExplanation(switch (choice.getEvaluation()) {
                        case "BEST" -> choice.getCulturalNote();
                        case "ACCEPTABLE" -> ACCEPTABLE[i];
                        case "AWKWARD" -> AWKWARD[i];
                        default -> IMPOLITE[i];
                    });
                }
            }
            var merge = nodes.get("merge-" + number);
            boolean stockDialogue = merge != null && "Sumi and Haru acknowledge your response, and the three of you continue together.".equals(merge.getText());
            stockDialogue = stockDialogue && (number % 2 == 0 ? "うん、覚えておこう。次へ行こう！" : "いい経験になったね。続きを見に行こう！").equals(merge.getJapanese());
            boolean stockCulture = merge != null && "CULTURAL_NOTE".equals(merge.getType()) && decision != null
                && merge.getJapanese() == null
                && decision.getChoices() != null
                && decision.getChoices().stream().anyMatch(c -> Objects.equals(c.getCulturalNote(), merge.getText()));
            if (stockDialogue || stockCulture) {
                merge.setText((stockCulture ? merge.getText() + " " : "") + TRANSITIONS[i]);
                merge.setType("NARRATION"); merge.setTitle("Next in the story");
                merge.setJapanese(null); merge.setRomaji(null); merge.setSpeaker(null); merge.setCharacterKey(null);
                merge.setSecondaryCharacterKey(null); merge.setAudioUrl(null); merge.setSpritesVisible(false);
            }
            for (String suffix : List.of("best", "acceptable", "awkward", "rude")) {
                var reaction = nodes.get("reaction-" + number + "-" + suffix);
                if (reaction != null && reaction.getJapanese() != null && reaction.getText() != null && REACTION_MEANINGS.containsKey(reaction.getJapanese())
                        && STOCK_REACTIONS.contains(reaction.getText())) reaction.setText(REACTION_MEANINGS.get(reaction.getJapanese()));
            }
        }
        var ending = nodes.get("ending-dialogue");
        if (ending != null && "You handled a lot of new situations today. Let us see how your choices shaped your first day.".equals(ending.getText()))
            ending.setText("You worked hard today!");
        var teacherScene = nodes.get("scene-13");
        if (teacherScene != null && teacherScene.getText() != null && teacherScene.getText().contains("Sumi unexpectedly meets her former teacher"))
            teacherScene.setText("Outside the community center, you and Sumi recognize Yamamoto-sensei from an earlier online course. It has been a while since you last met.");
        chapter.setStoryVersion(3); chapter.setUpdatedAt(Instant.now());
        return true;
    }
    private static final Set<String> GENERIC = Set.of(
        "This is the most natural and context-appropriate response.", "This works, although another response sounds more natural here.",
        "The words are valid Japanese, but they do not match this moment.",
        "This can make the listener uncomfortable because it is dismissive or disrespectful.");
    private static final Set<String> STOCK_REACTIONS = Set.of(
        "Exactly. That response fits both the relationship and the setting.",
        "That is understandable. A slightly different expression would sound smoother.",
        "I understand the words, but they sound unusual in this situation.",
        "Careful. That reply may sound disrespectful, so let us choose kinder wording.");
    private static final Map<String, String> REACTION_MEANINGS = Map.of(
        "そうそう！今の返事なら、相手も安心すると思うよ。", "That's right! I think that reply will put the other person at ease.",
        "うん、意味は伝わるよ。でも、もう少し自然な言い方もあるかな。", "Yes, your meaning comes across. But there may be a slightly more natural way to say it.",
        "え？……ああ、なるほど。意味はわかるけど、ここでは少し不自然かも。", "Huh? Oh, I see. I understand what you mean, but it may sound a little unnatural here.",
        "……え？ちょっと待って。その言い方は、この相手には失礼に聞こえるかも。", "Huh? Wait a moment. That wording may sound impolite to this person.",
        "それは……ちょっと。今の言い方はかなり強く聞こえるよ。言い直してみよう。", "That's a bit much. That wording sounds quite strong. Let's try saying it another way.");
    private static String legacyHint(int number) {
        return switch(number) {
            case 1, 20 -> "These are close friends. Look for a response that feels warm and natural rather than distant or demanding.";
            case 2, 7, 13, 19 -> "You are speaking to someone you do not know well. Begin respectfully and avoid commands.";
            case 3, 4 -> "Public transportation depends on consideration for the people around you. Choose the least disruptive action.";
            case 5 -> "The location is culturally important. Think about the small gesture visitors use to show respect.";
            case 8, 9, 10, 11, 12 -> "You are in a restaurant. Notice whether the moment happens before, during, or after the meal.";
            case 14, 15, 16, 17 -> "You are a guest in someone else's home. Choose language and behavior that acknowledges the host's space and effort.";
            default -> "Pay attention to age, status, setting, and how directly the response addresses the other person.";
        };
    }
}

