package japlearn.demo.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DialogueRelayPronunciationService {
    public record Prompt(String title, String reference, List<List<String>> requiredGroups, String explanation, String english) {}
    public record Result(String recognizedText, boolean appropriate, double pronunciation, double accuracy,
                         double fluency, double completeness, String feedback, String english,
                         String referenceText, List<String> wordIssues) {}

    private static final Map<String, Prompt> PROMPTS = Map.of(
        "bonus-1", new Prompt("Workplace clarification", "すみません、もう一度ゆっくりお願いします。", List.of(List.of("もう一度"), List.of("ゆっくり"), List.of("お願い")), "Politely ask the speaker to repeat the instruction more slowly.", "Excuse me, please say it once more slowly."),
        "bonus-2", new Prompt("Asking for meaning", "すみません、それはどういう意味ですか？", List.of(List.of("どういう意味", "どんな意味")), "Ask for the meaning instead of pretending to understand.", "Excuse me, what does that mean?"),
        "bonus-3", new Prompt("Explaining an allergy", "アレルギーがあります。これが入っていますか？", List.of(List.of("アレルギー"), List.of("入って", "入っています")), "State the allergy and check whether the ingredient is included.", "I have an allergy. Does this contain it?"),
        "bonus-4", new Prompt("Reporting a lost wallet", "すみません、財布をなくしました。", List.of(List.of("財布"), List.of("なくしました", "失くしました", "落としました")), "Clearly tell the officer that your wallet is missing.", "Excuse me, I lost my wallet."),
        "bonus-5", new Prompt("Asking where to go", "すみません、どこへ行けばいいですか？", List.of(List.of("どこ"), List.of("行けば", "行ったら", "行く")), "Ask the staff member which place or direction you should go to.", "Excuse me, where should I go?")
    );

    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).build();
    @Value("${azure.speech.key:}") private String speechKey;
    @Value("${azure.speech.region:southeastasia}") private String speechRegion;

    public DialogueRelayPronunciationService(ObjectMapper json) { this.json = json; }
    public Prompt prompt(String id) { return PROMPTS.get(id); }

    public Result assess(String promptId, MultipartFile audio) throws Exception {
        Prompt prompt = PROMPTS.get(promptId);
        if (prompt == null) throw new IllegalArgumentException("Unknown bonus prompt.");
        if (speechKey == null || speechKey.isBlank()) throw new IllegalStateException("Azure Speech is not configured.");
        if (audio == null || audio.isEmpty()) throw new IllegalArgumentException("A spoken response is required.");
        if (audio.getSize() > 12_000_000) throw new IllegalArgumentException("The recording is too large.");

        Path input = Files.createTempFile("relay-input-", extension(audio.getOriginalFilename()));
        Path wav = Files.createTempFile("relay-azure-", ".wav");
        try {
            audio.transferTo(input);
            convertToAzureWav(input, wav);
            String config = json.writeValueAsString(Map.of(
                "ReferenceText", prompt.reference(), "GradingSystem", "HundredMark",
                "Granularity", "Phoneme", "Dimension", "Comprehensive", "EnableMiscue", true));
            String encoded = Base64.getEncoder().encodeToString(config.getBytes(StandardCharsets.UTF_8));
            URI endpoint = URI.create("https://" + speechRegion + ".stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1?language=ja-JP&format=detailed");
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(35))
                .header("Ocp-Apim-Subscription-Key", speechKey)
                .header("Pronunciation-Assessment", encoded)
                .header("Accept", "application/json")
                .header("Content-Type", "audio/wav; codecs=audio/pcm; samplerate=16000")
                .POST(HttpRequest.BodyPublishers.ofFile(wav)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Azure Speech returned HTTP " + response.statusCode());
            }
            return parse(prompt, json.readTree(response.body()));
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(wav);
        }
    }

    private Result parse(Prompt prompt, JsonNode root) throws IOException {
        JsonNode best = root.path("NBest").path(0);
        if (best.isMissingNode()) throw new IOException("Azure did not recognize a spoken response.");
        JsonNode assessment = best.path("PronunciationAssessment");
        String recognized = best.path("Display").asText(best.path("Lexical").asText(""));
        String normalized = normalize(recognized);
        boolean intent = !normalized.isBlank() && prompt.requiredGroups().stream()
            .allMatch(group -> group.stream().map(this::normalize).anyMatch(normalized::contains));
        double pronunciation = score(assessment, "PronScore");
        double accuracy = score(assessment, "AccuracyScore");
        double fluency = score(assessment, "FluencyScore");
        double completeness = score(assessment, "CompletenessScore");
        boolean appropriate = intent && completeness >= 45 && accuracy >= 45;
        List<String> issues = new ArrayList<>();
        best.path("Words").forEach(word -> {
            String error = word.path("PronunciationAssessment").path("ErrorType").asText("None");
            if (!"None".equalsIgnoreCase(error)) issues.add(word.path("Word").asText() + ": " + error);
        });
        String feedback = appropriate
            ? (pronunciation >= 80 ? "Your response fits the situation and was clearly pronounced." : "Your response fits the situation. Practice it once more for clearer pronunciation.")
            : "The response was not clear enough for this situation. Use the suggested phrase and try again next time.";
        return new Result(recognized, appropriate, pronunciation, accuracy, fluency, completeness,
            feedback + " " + prompt.explanation(), prompt.english(), prompt.reference(), issues);
    }

    private double score(JsonNode node, String field) { return Math.round(node.path(field).asDouble(0) * 10.0) / 10.0; }
    private String normalize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[\\s、。！？,.!?]", ""); }
    private String extension(String name) {
        if (name == null || !name.contains(".")) return ".audio";
        String ext = name.substring(name.lastIndexOf('.')).replaceAll("[^A-Za-z0-9.]", "");
        return ext.length() > 8 ? ".audio" : ext;
    }
    private void convertToAzureWav(Path input, Path output) throws Exception {
        Process process = new ProcessBuilder("ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
            "-i", input.toString(), "-ac", "1", "-ar", "16000", "-c:a", "pcm_s16le", output.toString()).start();
        if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IOException("Audio conversion timed out."); }
        if (process.exitValue() != 0 || Files.size(output) <= 44) throw new IOException("The recorded audio format could not be processed.");
    }
}
