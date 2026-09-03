package japlearn.demo.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AzureSpeechService {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json;
    private final GuidedAudioNormalizer normalizer;
    @Value("${azure.speech.key:${AZURE_SPEECH_KEY:}}") private String key;
    @Value("${azure.speech.region:${AZURE_SPEECH_REGION:southeastasia}}") private String region;

    public AzureSpeechService(ObjectMapper json, GuidedAudioNormalizer normalizer) { this.json = json; this.normalizer = normalizer; }

    public boolean configured() { return key != null && !key.isBlank(); }

    public SpeechResult assess(byte[] audio, String contentType) {
        requireConfigured();
        try {
            String assessment = Base64.getEncoder().encodeToString(json.writeValueAsBytes(Map.of(
                "GradingSystem", "HundredMark", "Granularity", "Phoneme", "Dimension", "Comprehensive",
                "EnableMiscue", false)));
            String endpoint = "https://" + region + ".stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1?language=ja-JP&format=detailed";
            byte[] azureAudio = normalizer.toAzureWav(audio, contentType);
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Ocp-Apim-Subscription-Key", key)
                .header("Content-Type", "audio/wav; codecs=audio/pcm; samplerate=16000")
                .header("Pronunciation-Assessment", assessment)
                .POST(HttpRequest.BodyPublishers.ofByteArray(azureAudio)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Azure Speech could not evaluate this recording.");
            JsonNode root = json.readTree(response.body());
            JsonNode best = root.path("NBest").path(0);
            return new SpeechResult(best.path("Display").asText(root.path("DisplayText").asText()),
                score(best, "PronScore", "PronunciationScore"), score(best, "AccuracyScore", "AccuracyScore"),
                score(best, "FluencyScore", "FluencyScore"));
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Azure Speech is temporarily unavailable.", ex); }
    }

    private int score(JsonNode n, String direct, String assessment) {
        JsonNode value = n.path(direct); if (value.isNumber()) return value.asInt();
        return n.path("PronunciationAssessment").path(assessment).asInt(0);
    }
    private void requireConfigured(){ if(!configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Azure Speech is not configured on the server."); }
    public record SpeechResult(String transcript, int pronunciation, int accuracy, int fluency) {}
}
