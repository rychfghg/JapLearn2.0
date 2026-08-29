package japlearn.demo.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import japlearn.demo.Entity.ReplyCoachChapter;
import japlearn.demo.Repository.ReplyCoachChapterRepository;

@Configuration
public class ReplyCoachChapterSeeder {
    private record Encounter(
            String location,
            String background,
            String narration,
            String speaker,
            String dialogue,
            String japanese,
            String romaji,
            String prompt,
            String bestJp,
            String bestRomaji,
            String bestText,
            String acceptableJp,
            String acceptableRomaji,
            String acceptableText,
            String awkwardJp,
            String awkwardRomaji,
            String awkwardText,
            String rudeJp,
            String rudeRomaji,
            String rudeText,
            String note) {}

    @Bean
    CommandLineRunner seedReplyCoachChapter(ReplyCoachChapterRepository repository) {
        return args -> {
            ReplyCoachChapter chapter = repository
                    .findByTitleIgnoreCase("A Day of First Impressions")
                    .orElseGet(ReplyCoachChapter::new);
            boolean currentStoryVersion = chapter.getNodes() != null
                    && chapter.getNodes().stream()
                            .anyMatch(node -> "dialogue-1-companion".equals(node.getId()));
            if (currentStoryVersion) return;

            chapter.setTitle("A Day of First Impressions");
            chapter.setDescription("Join Sumi and Haru for a connected first-day journey through Tokyo while learning natural replies, etiquette, and respectful behavior.");
            chapter.setDifficulty("BEGINNER_TO_INTERMEDIATE");
            chapter.setLearningObjectives(List.of(
                    "Choose natural greetings for friends, strangers, and superiors",
                    "Use polite Japanese in public and service situations",
                    "Practice restaurant, train, home, and gift etiquette",
                    "Recognize awkward, impolite, and offensive responses"));
            chapter.setStatus("PUBLISHED");
            chapter.setCoverKey("station");
            chapter.setOrder(1);
            chapter.setStartNodeId("opening");

            List<ReplyCoachChapter.StoryNode> nodes = new ArrayList<>();
            nodes.add(node(
                    "opening",
                    "NARRATION",
                    "Tokyo, 8:10 AM",
                    "The morning train releases a stream of commuters into central Tokyo. Today is your first full day in Japan, and Sumi and Haru have promised to guide you through the small social moments that guidebooks rarely explain.",
                    "station",
                    "scene-1",
                    false));

            List<Encounter> encounters = encounters();
            for (int index = 0; index < encounters.size(); index++) {
                int number = index + 1;
                Encounter encounter = encounters.get(index);
                String nextScene = number == encounters.size() ? "ending-narration" : "scene-" + (number + 1);
                String dialogueId = "dialogue-" + number;
                String companionDialogueId = "dialogue-" + number + "-companion";
                String choiceId = "decision-" + number;
                String mergeId = "merge-" + number;

                nodes.add(node(
                        "scene-" + number,
                        "NARRATION",
                        encounter.location(),
                        encounter.narration(),
                        encounter.background(),
                        dialogueId,
                        false));

                ReplyCoachChapter.StoryNode dialogue = node(
                        dialogueId,
                        "DIALOGUE",
                        encounter.speaker(),
                        encounter.dialogue(),
                        encounter.background(),
                        companionDialogueId,
                        true);
                dialogue.setSpeaker(encounter.speaker());
                dialogue.setJapanese(encounter.japanese());
                dialogue.setRomaji(encounter.romaji());
                dialogue.setCharacterKey("Sumi".equals(encounter.speaker()) ? "SUMI" : "HARU");
                dialogue.setExpressionKey("SPEAKING");
                dialogue.setSecondaryCharacterKey("Sumi".equals(encounter.speaker()) ? "HARU" : "SUMI");
                dialogue.setSecondaryExpressionKey("NEUTRAL");
                nodes.add(dialogue);

                String companionKey = "Sumi".equals(encounter.speaker()) ? "HARU" : "SUMI";
                String companionName = "SUMI".equals(companionKey) ? "Sumi" : "Haru";
                ReplyCoachChapter.StoryNode companionDialogue = node(
                        companionDialogueId,
                        "DIALOGUE",
                        companionName,
                        companionName + " looks toward you and brings you into the conversation.",
                        encounter.background(),
                        choiceId,
                        true);
                companionDialogue.setSpeaker(companionName);
                companionDialogue.setJapanese(number % 2 == 0
                        ? "そうだね。あなたなら、どう答える？"
                        : "一緒に考えよう。あなたは何と言う？");
                companionDialogue.setRomaji(number % 2 == 0
                        ? "Sou da ne. Anata nara, dou kotaeru?"
                        : "Issho ni kangaeyou. Anata wa nan to iu?");
                companionDialogue.setCharacterKey(companionKey);
                companionDialogue.setExpressionKey("SPEAKING");
                companionDialogue.setSecondaryCharacterKey(
                        "SUMI".equals(companionKey) ? "HARU" : "SUMI");
                companionDialogue.setSecondaryExpressionKey("NEUTRAL");
                nodes.add(companionDialogue);

                ReplyCoachChapter.StoryNode choice = node(
                        choiceId,
                        "CHOICE",
                        "What would you say?",
                        encounter.prompt(),
                        encounter.background(),
                        null,
                        true);
                choice.setCharacterKey("Sumi".equals(encounter.speaker()) ? "SUMI" : "HARU");
                choice.setExpressionKey("NEUTRAL");
                choice.setSecondaryCharacterKey("Sumi".equals(encounter.speaker()) ? "HARU" : "SUMI");
                choice.setShuffleChoices(true);

                List<ReplyCoachChapter.ChoiceOption> options = new ArrayList<>(List.of(
                        option(number + "-best", encounter.bestText(), encounter.bestJp(), encounter.bestRomaji(),
                                "BEST", 5, "This is the most natural and context-appropriate response.",
                                encounter.note(), "Exactly. That response fits both the relationship and the setting.",
                                "SUMI", "CORRECT", "reaction-" + number + "-best"),
                        option(number + "-acceptable", encounter.acceptableText(), encounter.acceptableJp(), encounter.acceptableRomaji(),
                                "ACCEPTABLE", 4, "This works, although another response sounds more natural here.",
                                encounter.note(), "That is understandable. A slightly different expression would sound smoother.",
                                "HARU", "NEUTRAL", "reaction-" + number + "-acceptable"),
                        option(number + "-awkward", encounter.awkwardText(), encounter.awkwardJp(), encounter.awkwardRomaji(),
                                "AWKWARD", 2, "The words are valid Japanese, but they do not match this moment.",
                                encounter.note(), "I understand the words, but they sound unusual in this situation.",
                                "SUMI", "WRONG", "reaction-" + number + "-awkward"),
                        option(number + "-rude", encounter.rudeText(), encounter.rudeJp(), encounter.rudeRomaji(),
                                number % 3 == 0 ? "RUDE" : "IMPOLITE", 0,
                                "This can make the listener uncomfortable because it is dismissive or disrespectful.",
                                encounter.note(), "Careful. That reply may sound disrespectful, so let us choose kinder wording.",
                                "HARU", "WRONG", "reaction-" + number + "-rude")));
                rotate(options, number % options.size());
                choice.setChoices(options);
                nodes.add(choice);

                nodes.add(reaction("reaction-" + number + "-best", "SUMI", "CORRECT", options, number + "-best", encounter.background(), mergeId));
                nodes.add(reaction("reaction-" + number + "-acceptable", "HARU", "NEUTRAL", options, number + "-acceptable", encounter.background(), mergeId));
                nodes.add(reaction("reaction-" + number + "-awkward", "SUMI", "WRONG", options, number + "-awkward", encounter.background(), mergeId));
                nodes.add(reaction("reaction-" + number + "-rude", "HARU", "WRONG", options, number + "-rude", encounter.background(), mergeId));

                ReplyCoachChapter.StoryNode merge = node(
                        mergeId,
                        number % 4 == 0 ? "CULTURAL_NOTE" : "DIALOGUE",
                        number % 4 == 0 ? "Culture note" : "The journey continues",
                        number % 4 == 0
                                ? encounter.note()
                                : "Sumi and Haru acknowledge your response, and the three of you continue together.",
                        encounter.background(),
                        nextScene,
                        number % 4 != 0);
                merge.setSpeaker(number % 4 == 0 ? null : "Sumi");
                merge.setCharacterKey("SUMI");
                merge.setExpressionKey("SMILE");
                merge.setSecondaryCharacterKey("HARU");
                merge.setSecondaryExpressionKey("SMILE");
                if (number % 4 != 0) {
                    merge.setJapanese(number % 2 == 0
                            ? "うん、覚えておこう。次へ行こう！"
                            : "いい経験になったね。続きを見に行こう！");
                    merge.setRomaji(number % 2 == 0
                            ? "Un, oboete okou. Tsugi e ikou!"
                            : "Ii keiken ni natta ne. Tsuzuki o mi ni ikou!");
                }
                nodes.add(merge);
            }

            nodes.add(node(
                    "ending-narration",
                    "NARRATION",
                    "Tokyo, 8:45 PM",
                    "The day ends beneath the station lights. You did more than memorize phrases—you learned how relationships, setting, and culture shape every reply.",
                    "station-night",
                    "ending-dialogue",
                    false));
            ReplyCoachChapter.StoryNode endingDialogue = node(
                    "ending-dialogue",
                    "DIALOGUE",
                    "Sumi",
                    "You handled a lot of new situations today. Let us see how your choices shaped your first day.",
                    "station-night",
                    "ending",
                    true);
            endingDialogue.setSpeaker("Sumi");
            endingDialogue.setJapanese("今日はよく頑張ったね！");
            endingDialogue.setRomaji("Kyou wa yoku ganbatta ne!");
            endingDialogue.setCharacterKey("SUMI");
            endingDialogue.setExpressionKey("CORRECT");
            endingDialogue.setSecondaryCharacterKey("HARU");
            endingDialogue.setSecondaryExpressionKey("SMILE");
            nodes.add(endingDialogue);
            nodes.add(node("ending", "ENDING", "Reply Coach Complete", "Your first-day journey is complete.", "station-night", null, false));

            chapter.setNodes(nodes);
            repository.save(chapter);
        };
    }

    private List<Encounter> encounters() {
        return Arrays.asList(
                e("Tokyo Station", "station", "Outside the station, Sumi and Haru wave from beside the ticket gate.", "Sumi", "You made it! How was the trip?", "やっと来たね！", "Yatto kita ne!", "How do you greet your friends?", "会えてうれしい！", "Aete ureshii!", "I'm happy to see you!", "こんにちは！", "Konnichiwa!", "Hello!", "いただきます", "Itadakimasu", "Let's eat.", "遅いよ。", "Osoi yo.", "You're late.", "Close friends can use casual greetings, but the first reply should still feel warm."),
                e("Station Gate", "station", "Haru notices a station employee helping travelers near the map.", "Haru", "We should ask which platform reaches Asakusa.", "浅草は何番線かな。", "Asakusa wa nanbansen kana.", "How do you ask the employee?", "すみません、浅草は何番線ですか。", "Sumimasen, Asakusa wa nanbansen desu ka.", "Excuse me, which platform is for Asakusa?", "浅草はどこですか。", "Asakusa wa doko desu ka.", "Where is Asakusa?", "浅草！", "Asakusa!", "Asakusa!", "早く教えて。", "Hayaku oshiete.", "Tell me quickly.", "Sumimasen politely gets a stranger's attention before a request."),
                e("Train Platform", "train", "A crowded train arrives, and passengers form orderly lines beside the doors.", "Sumi", "Everyone is waiting for people to exit first.", "先に降りてもらおう。", "Saki ni orite moraou.", "What should you do?", "降りる人を先に通します。", "Oriru hito o saki ni tooshimasu.", "Let passengers exit first.", "列の後ろで待ちます。", "Retsu no ushiro de machimasu.", "Wait at the back of the line.", "すぐ乗ります。", "Sugu norimasu.", "Board immediately.", "押して入ります。", "Oshite hairimasu.", "Push inside.", "Train etiquette prioritizes orderly lines and lets exiting passengers move first."),
                e("Inside the Train", "train", "A phone begins ringing in the quiet carriage.", "Haru", "People usually keep calls quiet on the train.", "電車では静かにしよう。", "Densha de wa shizuka ni shiyou.", "How do you handle your phone?", "マナーモードにします。", "Manaa moodo ni shimasu.", "Switch it to silent mode.", "小さい声で話します。", "Chiisai koe de hanashimasu.", "Speak very quietly.", "普通に電話します。", "Futsuu ni denwa shimasu.", "Take the call normally.", "大声で話します。", "Oogoe de hanashimasu.", "Speak loudly.", "Calls are generally avoided on Japanese trains; silent mode is considerate."),
                e("Temple Entrance", "temple", "The route passes a small temple where visitors pause at the gate.", "Sumi", "A small bow shows respect before entering.", "入る前に軽くお辞儀をするよ。", "Hairu mae ni karuku ojigi o suru yo.", "What do you do at the entrance?", "軽くお辞儀します。", "Karuku ojigi shimasu.", "Bow lightly.", "静かに入ります。", "Shizuka ni hairimasu.", "Enter quietly.", "手を振ります。", "Te o furimasu.", "Wave your hand.", "門に座ります。", "Mon ni suwarimasu.", "Sit on the gate.", "A small bow at a sacred entrance acknowledges the place respectfully."),
                e("Local Shop", "shop", "You stop at a small shop. The clerk welcomes you with a cheerful greeting.", "Haru", "The clerk said irasshaimase. You do not need a long reply.", "長く答えなくても大丈夫。", "Nagaku kotaenakute mo daijoubu.", "How do you respond?", "軽く会釈します。", "Karuku eshaku shimasu.", "Give a small nod.", "こんにちは。", "Konnichiwa.", "Say hello.", "いらっしゃいませ。", "Irasshaimase.", "Welcome to the store.", "何？", "Nani?", "What?", "Customers are not expected to repeat irasshaimase; a nod is enough."),
                e("Checkout Counter", "shop", "The clerk places your purchase in a tray and states the total.", "Sumi", "Place the money or card on the tray rather than tossing it across the counter.", "トレーに置こう。", "Toree ni okou.", "What do you say while paying?", "お願いします。", "Onegaishimasu.", "Please.", "これでお願いします。", "Kore de onegaishimasu.", "This, please.", "ちょうだい。", "Choudai.", "Give it to me.", "早くして。", "Hayaku shite.", "Hurry up.", "Using the payment tray and a brief onegaishimasu is polite and common."),
                e("Restaurant Entrance", "restaurant", "At lunch, the three of you enter a small family restaurant.", "Haru", "The staff asks how many people are in your group.", "何名様ですか、と聞かれたよ。", "Nanmei-sama desu ka, to kikareta yo.", "How do you answer?", "三人です。", "Sannin desu.", "Three people.", "三名です。", "Sanmei desu.", "A party of three.", "三人！", "Sannin!", "Three!", "見ればわかるでしょ。", "Mireba wakaru desho.", "You can see that, can't you?", "A short polite count with desu is natural when restaurant staff ask party size."),
                e("Ordering Lunch", "restaurant", "The server waits beside the table while everyone looks at the menu.", "Sumi", "Pointing is fine, but add a polite phrase.", "丁寧な言い方をつけよう。", "Teinei na iikata o tsukeyou.", "How do you order?", "これをお願いします。", "Kore o onegaishimasu.", "This, please.", "これください。", "Kore kudasai.", "This, please.", "これ。", "Kore.", "This.", "早く持ってきて。", "Hayaku motte kite.", "Bring it quickly.", "Onegaishimasu is a courteous, flexible phrase for ordering."),
                e("Before Eating", "restaurant", "The meals arrive together, filling the table with steam and color.", "Haru", "There is a phrase people often say before eating.", "食べる前の言葉だよ。", "Taberu mae no kotoba da yo.", "What do you say?", "いただきます。", "Itadakimasu.", "I gratefully receive this meal.", "食べましょう。", "Tabemashou.", "Let's eat.", "ごちそうさま。", "Gochisousama.", "Thank you for the meal.", "まずいかも。", "Mazui kamo.", "It may taste bad.", "Itadakimasu expresses gratitude before eating; gochisousama is used after."),
                e("Using Chopsticks", "restaurant", "You pause with your chopsticks while deciding where to place them.", "Sumi", "Do not stick chopsticks upright in rice.", "箸をご飯に立てないでね。", "Hashi o gohan ni tatenaide ne.", "Where should the chopsticks rest?", "箸置きに置きます。", "Hashioki ni okimasu.", "Place them on the chopstick rest.", "皿の端にそっと置きます。", "Sara no hashi ni sotto okimasu.", "Rest them gently at the plate edge.", "ご飯に立てます。", "Gohan ni tatemasu.", "Stand them in the rice.", "人に向けます。", "Hito ni mukemasu.", "Point them at someone.", "Upright chopsticks resemble funeral incense and should be avoided."),
                e("After Lunch", "restaurant", "Everyone finishes eating, and the server begins clearing nearby tables.", "Haru", "This is the time for the phrase we did not use before eating.", "今度は食べた後の言葉だね。", "Kondo wa tabeta ato no kotoba da ne.", "What do you say?", "ごちそうさまでした。", "Gochisousama deshita.", "Thank you for the meal.", "おいしかったです。", "Oishikatta desu.", "It was delicious.", "いただきます。", "Itadakimasu.", "I gratefully receive.", "もういい。", "Mou ii.", "I've had enough.", "Gochisousama deshita politely closes the meal and thanks those involved."),
                e("Teacher Encounter", "hallway", "Outside a community center, Sumi unexpectedly meets her former teacher.", "Sumi", "This is Yamamoto-sensei. We should speak more formally.", "山本先生です。丁寧に話そう。", "Yamamoto-sensei desu. Teinei ni hanasou.", "How do you greet the teacher?", "お久しぶりです。お元気ですか。", "Ohisashiburi desu. Ogenki desu ka.", "It has been a while. How are you?", "こんにちは、先生。", "Konnichiwa, sensei.", "Hello, teacher.", "久しぶり！", "Hisashiburi!", "Long time no see!", "元気？", "Genki?", "You good?", "Teachers and seniors generally receive polite desu/masu speech."),
                e("Receiving a Gift", "home", "At a host family's doorway, the host offers you a small wrapped welcome gift.", "Haru", "Receive it with both hands when practical.", "できれば両手で受け取ろう。", "Dekireba ryoute de uketorou.", "What do you say?", "ありがとうございます。大切にします。", "Arigatou gozaimasu. Taisetsu ni shimasu.", "Thank you very much. I will treasure it.", "ありがとうございます。", "Arigatou gozaimasu.", "Thank you very much.", "もらいます。", "Moraimasu.", "I'll take it.", "これだけ？", "Kore dake?", "Is this all?", "Both hands and a grateful phrase communicate care when receiving an object."),
                e("Entering a Home", "home", "The host opens the door and invites everyone inside.", "Sumi", "The entryway is where outdoor shoes come off.", "玄関で靴を脱ぐよ。", "Genkan de kutsu o nugu yo.", "What do you say as a guest entering?", "おじゃまします。", "Ojama shimasu.", "Excuse me for intruding.", "失礼します。", "Shitsurei shimasu.", "Excuse me.", "ただいま。", "Tadaima.", "I'm home.", "入るよ。", "Hairu yo.", "I'm coming in.", "Ojama shimasu acknowledges that you are entering another person's space."),
                e("Shoes at the Genkan", "home", "You remove your shoes and notice everyone turning theirs toward the door.", "Haru", "That makes them easy to put on when leaving.", "帰る時に履きやすいね。", "Kaeru toki ni hakiyasui ne.", "What should you do?", "靴をそろえてドア側に向けます。", "Kutsu o soroete doa-gawa ni mukemasu.", "Arrange them neatly facing the door.", "靴を端にそろえます。", "Kutsu o hashi ni soroemasu.", "Line them neatly at the side.", "そのまま散らかします。", "Sono mama chirakashimasu.", "Leave them scattered.", "靴のまま入ります。", "Kutsu no mama hairimasu.", "Enter wearing shoes.", "Neatly turning shoes toward the exit is a thoughtful guest habit."),
                e("Tea with the Host", "home", "The host pours tea and offers the cup toward you.", "Sumi", "A short expression of thanks is enough.", "短いお礼で大丈夫。", "Mijikai orei de daijoubu.", "How do you accept the tea?", "ありがとうございます。いただきます。", "Arigatou gozaimasu. Itadakimasu.", "Thank you. I gratefully receive it.", "ありがとうございます。", "Arigatou gozaimasu.", "Thank you very much.", "お茶。", "Ocha.", "Tea.", "いらない。", "Iranai.", "I don't want it.", "A polite thanks respects the host's effort even for a simple drink."),
                e("Accidental Spill", "home", "Your sleeve catches the cup, and a few drops spill onto the table.", "Haru", "Apologize immediately and offer to help.", "すぐ謝って手伝おう。", "Sugu ayamatte tetsudaou.", "What do you say?", "申し訳ありません。すぐ拭きます。", "Moushiwake arimasen. Sugu fukimasu.", "I'm very sorry. I will wipe it immediately.", "すみません。大丈夫ですか。", "Sumimasen. Daijoubu desu ka.", "I'm sorry. Is everything okay?", "ごめん。", "Gomen.", "Sorry.", "テーブルが悪い。", "Teeburu ga warui.", "The table is at fault.", "A sincere apology plus corrective action is stronger than an excuse."),
                e("Neighbor's Request", "neighborhood", "On the way out, an older neighbor asks whether you can move aside for a bicycle.", "Sumi", "Use a respectful reply to acknowledge the request.", "丁寧に返事をしよう。", "Teinei ni henji o shiyou.", "How do you respond?", "はい、どうぞ。すみません。", "Hai, douzo. Sumimasen.", "Yes, please go ahead. Sorry.", "はい、わかりました。", "Hai, wakarimashita.", "Yes, understood.", "ちょっと待って。", "Chotto matte.", "Wait a moment.", "自分で通って。", "Jibun de toutte.", "Get through by yourself.", "Hai and a polite acknowledgment work well with an older stranger."),
                e("Saying Goodbye", "station-night", "Back at the station, the day is ending and your companions must head home.", "Haru", "We will meet again next weekend.", "また来週会おう。", "Mata raishuu aou.", "How do you close the day?", "今日はありがとう。また来週！", "Kyou wa arigatou. Mata raishuu!", "Thank you for today. See you next week!", "またね。気をつけて。", "Mata ne. Ki o tsukete.", "See you. Take care.", "さようなら。", "Sayounara.", "Goodbye.", "やっと帰れる。", "Yatto kaereru.", "Finally, I can leave.", "Thanking companions before parting acknowledges the time and care they shared."));
    }

    private Encounter e(String... v) {
        return new Encounter(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11], v[12], v[13], v[14], v[15], v[16], v[17], v[18], v[19], v[20]);
    }

    private ReplyCoachChapter.StoryNode node(String id, String type, String title, String text,
            String background, String next, boolean spritesVisible) {
        ReplyCoachChapter.StoryNode node = new ReplyCoachChapter.StoryNode();
        node.setId(id);
        node.setType(type);
        node.setTitle(title);
        node.setText(text);
        node.setBackgroundKey(background);
        node.setNextNodeId(next);
        node.setSpritesVisible(spritesVisible);
        return node;
    }

    private ReplyCoachChapter.StoryNode reaction(String id, String character, String expression,
            List<ReplyCoachChapter.ChoiceOption> options, String optionId, String background, String next) {
        ReplyCoachChapter.ChoiceOption option = options.stream()
                .filter(item -> optionId.equals(item.getId())).findFirst().orElseThrow();
        ReplyCoachChapter.StoryNode node = node(id, "REACTION", option.getEvaluation(),
                option.getReactionText(), background, next, true);
        node.setSpeaker(character.equals("SUMI") ? "Sumi" : "Haru");
        node.setCharacterKey(character);
        node.setExpressionKey(expression);
        node.setSecondaryCharacterKey(character.equals("SUMI") ? "HARU" : "SUMI");
        node.setSecondaryExpressionKey("NEUTRAL");
        switch (option.getEvaluation()) {
            case "BEST" -> {
                node.setJapanese("うん、とても自然でいい答えだね！");
                node.setRomaji("Un, totemo shizen de ii kotae da ne!");
            }
            case "ACCEPTABLE" -> {
                node.setJapanese("通じるけど、もう少し自然に言えるよ。");
                node.setRomaji("Tsujiru kedo, mou sukoshi shizen ni ieru yo.");
            }
            case "AWKWARD" -> {
                node.setJapanese("ちょっと不自然に聞こえるかもしれないね。");
                node.setRomaji("Chotto fushizen ni kikoeru kamo shirenai ne.");
            }
            default -> {
                node.setJapanese("その言い方は失礼に聞こえるよ。気をつけよう。");
                node.setRomaji("Sono iikata wa shitsurei ni kikoeru yo. Ki o tsukeyou.");
            }
        }
        return node;
    }

    private ReplyCoachChapter.ChoiceOption option(String id, String text, String jp, String romaji,
            String evaluation, int points, String explanation, String note, String reaction,
            String character, String expression, String next) {
        ReplyCoachChapter.ChoiceOption option = new ReplyCoachChapter.ChoiceOption();
        option.setId(id);
        option.setText(text);
        option.setJapanese(jp);
        option.setRomaji(romaji);
        option.setEvaluation(evaluation);
        option.setPoints(points);
        option.setExplanation(explanation);
        option.setCulturalNote(note);
        option.setReactionText(reaction);
        option.setReactionCharacterKey(character);
        option.setReactionExpressionKey(expression);
        option.setNextNodeId(next);
        return option;
    }

    private <T> void rotate(List<T> items, int amount) {
        for (int index = 0; index < amount; index++) items.add(items.remove(0));
    }
}
