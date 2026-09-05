package japlearn.demo.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

@Service
public class DialogueRelayPronunciationService {
    public record Prompt(String title, String reference, List<List<String>> requiredGroups,
                         String task, String english, String pronunciationGuide) {}
    public record Result(String recognizedText, boolean appropriate, double pronunciation, double accuracy,
                         double fluency, double completeness, double contextScore, String feedback, String english,
                         String referenceText, List<String> wordIssues, String contextVerdict,
                         String contextExplanation, String pronunciationFeedback,
                         String pronunciationGuide, List<String> missingIdeas) {}

    private static final Map<String, Prompt> PROMPTS = Map.of(
        "bonus-1", new Prompt("Workplace clarification", "すみません、もう一度ゆっくりお願いします。", List.of(List.of("もう一度"), List.of("ゆっくり"), List.of("お願い")), "politely ask the speaker to repeat the instruction more slowly.", "Excuse me, please say it once more slowly.", "SU-MI-MA-SEN · MOU I-CHI-DO · YUK-KU-RI · O-NE-GAI-SHI-MA-SU"),
        "bonus-2", new Prompt("Asking for meaning", "すみません、それはどういう意味ですか？", List.of(List.of("どういう意味", "どんな意味")), "ask what the unfamiliar word or expression means.", "Excuse me, what does that mean?", "SU-MI-MA-SEN · SO-RE WA · DOU IU I-MI DE-SU KA"),
        "bonus-3", new Prompt("Explaining an allergy", "アレルギーがあります。これが入っていますか？", List.of(List.of("アレルギー"), List.of("入って", "入っています")), "tell the staff member about the allergy and ask whether the ingredient is included.", "I have an allergy. Does this contain it?", "A-RE-RU-GII GA A-RI-MA-SU · KO-RE GA HAIT-TE I-MA-SU KA"),
        "bonus-4", new Prompt("Reporting a lost wallet", "すみません、財布をなくしました。", List.of(List.of("財布"), List.of("なくしました", "失くしました", "落としました")), "tell the officer that your wallet is missing.", "Excuse me, I lost my wallet.", "SU-MI-MA-SEN · SAI-FU O · NA-KU-SHI-MA-SHI-TA"),
        "bonus-5", new Prompt("Asking where to go", "すみません、どこへ行けばいいですか？", List.of(List.of("どこ"), List.of("行けば", "行ったら", "行く")), "ask the staff member which place or direction you should go to.", "Excuse me, where should I go?", "SU-MI-MA-SEN · DO-KO E · I-KE-BA II DE-SU KA")
    );

    private final ObjectMapper json;
    @Value("${azure.speech.key:}") private String speechKey;
    @Value("${azure.speech.region:southeastasia}") private String speechRegion;

    public DialogueRelayPronunciationService(ObjectMapper json) { this.json = json; }
    public Prompt prompt(String id) { return PROMPTS.get(id); }
    public boolean configured() { return speechKey != null && !speechKey.isBlank(); }
    public String region() { return speechRegion; }

    public Result assess(String promptId, MultipartFile audio) throws Exception {
        Prompt prompt = PROMPTS.get(promptId);
        if (prompt == null) throw new IllegalArgumentException("Unknown bonus prompt.");
        if (!configured()) throw new IllegalStateException("Azure Speech is not configured.");
        if (audio == null || audio.isEmpty()) throw new IllegalArgumentException("A spoken response is required.");
        if (audio.getSize() > 12_000_000) throw new IllegalArgumentException("The recording is too large.");
        Path input = Files.createTempFile("relay-input-", extension(audio.getOriginalFilename()));
        Path wav = Files.createTempFile("relay-azure-", ".wav");
        try {
            audio.transferTo(input);
            convertToAzureWav(input, wav);
            return assessWithAzureSdk(prompt, wav);
        } finally { Files.deleteIfExists(input); Files.deleteIfExists(wav); }
    }

    private Result assessWithAzureSdk(Prompt prompt, Path wav) throws Exception {
        try (SpeechConfig speech = SpeechConfig.fromSubscription(speechKey, speechRegion)) {
            speech.setSpeechRecognitionLanguage("ja-JP");
            try (AudioConfig audio = AudioConfig.fromWavFileInput(wav.toString());
                 SpeechRecognizer recognizer = new SpeechRecognizer(speech, audio);
                 PronunciationAssessmentConfig config = new PronunciationAssessmentConfig(prompt.reference(),
                     PronunciationAssessmentGradingSystem.HundredMark,
                     PronunciationAssessmentGranularity.Phoneme, true)) {
              config.applyTo(recognizer);
              try (SpeechRecognitionResult result = recognizer.recognizeOnceAsync().get(35, TimeUnit.SECONDS)) {
                if (result.getReason() != ResultReason.RecognizedSpeech) throw new IOException("Azure could not recognize a spoken response.");
                PronunciationAssessmentResult scores = PronunciationAssessmentResult.fromResult(result);
                double pronunciation = rounded(scores.getPronunciationScore());
                double accuracy = rounded(scores.getAccuracyScore());
                double fluency = rounded(scores.getFluencyScore());
                double completeness = rounded(scores.getCompletenessScore());
                if (pronunciation == 0 && accuracy == 0 && fluency == 0 && completeness == 0)
                    throw new IOException("Azure returned a transcript without pronunciation metrics.");
                String raw = result.getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult);
                return buildResult(prompt, result.getText(), pronunciation, accuracy, fluency, completeness, raw);
              }
            }
        }
    }

    private Result buildResult(Prompt prompt, String recognized, double pronunciation, double accuracy,
                               double fluency, double completeness, String rawJson) {
        String normalized = normalize(recognized);
        List<String> missingIdeas = new ArrayList<>();
        int matched = 0;
        for (List<String> group : prompt.requiredGroups()) {
            boolean found = group.stream().map(this::normalize).anyMatch(normalized::contains);
            if (found) matched++; else missingIdeas.add(group.get(0));
        }
        boolean politeOpener = containsAny(normalized, "すみません", "お願い", "ください");
        boolean appropriate = !normalized.isBlank() && matched == prompt.requiredGroups().size();
        double ideaRatio = prompt.requiredGroups().isEmpty() ? 0 : (double) matched / prompt.requiredGroups().size();
        double contextScore = rounded(Math.min(100, ideaRatio * 85 + (politeOpener ? 15 : 0)));
        String verdict = normalized.isBlank() ? "NOT_HEARD" : appropriate ? "COMPLETE" : (matched > 0 || politeOpener) ? "PARTIAL" : "DOES_NOT_FIT";
        List<String> issues = extractWordIssues(rawJson);
        String context = "Situation score: " + contextScore + "/100. " + explainContext(prompt, verdict, politeOpener, missingIdeas, recognized);
        String pronunciationText = explainPronunciation(recognized, pronunciation, accuracy, fluency, issues);
        return new Result(recognized, appropriate, pronunciation, accuracy, fluency, completeness, contextScore,
            context + " " + pronunciationText, prompt.english(), prompt.reference(), issues, verdict,
            context, pronunciationText, prompt.pronunciationGuide(), missingIdeas);
    }

    private String explainContext(Prompt prompt, String verdict, boolean polite, List<String> missing, String heard) {
        if ("COMPLETE".equals(verdict)) return "Your response completes the task: " + prompt.task() + " It gives Sumi the information needed for this moment.";
        if ("NOT_HEARD".equals(verdict)) return "No usable Japanese response was heard, so the situation could not be rated. Record again in a quieter place and speak toward the microphone.";
        String ideas = missing.isEmpty() ? "the main requested information" : String.join(" and ", missing);
        if (polite) return "“" + heard + "” works as a polite opening, so it earns partial credit. However, it does not complete the request by itself. You still need to " + prompt.task() + " Add the idea “" + ideas + "”.";
        if ("PARTIAL".equals(verdict)) return "Your response communicates part of the intended meaning, but Sumi is still missing “" + ideas + "”. Complete it to " + prompt.task();
        return "Azure recognized Japanese, but its meaning does not answer this situation. Here, you need to " + prompt.task() + " Use the complete model response below as a guide.";
    }

    private String explainPronunciation(String heard, double pronunciation, double accuracy, double fluency, List<String> issues) {
        String notes = issues.isEmpty() ? "" : " Review: " + String.join(", ", issues.subList(0, Math.min(3, issues.size()))) + ".";
        if (heard == null || heard.isBlank()) return "Pronunciation could not be rated because no speech was recognized.";
        if (pronunciation >= 85) return "The words Azure heard were pronounced clearly and naturally." + notes;
        if (pronunciation >= 70) return "The words Azure heard were understandable. Slow down slightly and keep each sound group clear." + notes;
        if (accuracy >= 55 || fluency >= 55) return "Your speech was partly understandable, but some sounds or pacing reduced clarity. Use the sound guide and try once more." + notes;
        return "Speak a little louder and more slowly, separating the sound groups in the guide. This score rates pronunciation only, not situation fit." + notes;
    }

    private List<String> extractWordIssues(String raw) {
        List<String> issues = new ArrayList<>();
        if (raw == null || raw.isBlank()) return issues;
        try {
            JsonNode best = json.readTree(raw).path("NBest").path(0);
            best.path("Words").forEach(word -> {
                String error = word.path("PronunciationAssessment").path("ErrorType").asText("None");
                if (!"None".equalsIgnoreCase(error)) issues.add(word.path("Word").asText() + ": " + error);
            });
        } catch (Exception ignored) { }
        return issues;
    }

    private boolean containsAny(String value, String... options) { for (String option : options) if (value.contains(normalize(option))) return true; return false; }
    private double rounded(double value) { return Math.round(value * 10.0) / 10.0; }
    private String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s、。！？,.!?]", ""); }
    private String extension(String name) { if (name == null || !name.contains(".")) return ".audio"; String ext = name.substring(name.lastIndexOf('.')).replaceAll("[^A-Za-z0-9.]", ""); return ext.length() > 8 ? ".audio" : ext; }
    private void convertToAzureWav(Path input, Path output) throws Exception {
        Process process = new ProcessBuilder("ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y", "-i", input.toString(), "-ac", "1", "-ar", "16000", "-c:a", "pcm_s16le", output.toString()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IOException("Audio conversion timed out."); }
        if (process.exitValue() != 0 || Files.size(output) <= 44) throw new IOException("The recorded audio format could not be processed.");
    }
}
