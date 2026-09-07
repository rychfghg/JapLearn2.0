package japlearn.demo.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiLiveConversationProvider implements LiveConversationProvider {
    private static final Logger log = LoggerFactory.getLogger(GeminiLiveConversationProvider.class);
    private static final String TOKEN_URL = "https://generativelanguage.googleapis.com/v1beta/auth_tokens";
    private static final String WS_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json;
    @Value("${gemini.api.key:}") private String apiKey;
    @Value("${gemini.live.model:gemini-3.1-flash-live-preview}") private String model;
    @Value("${gemini.sumi.voice:Aoede}") private String voice;

    public GeminiLiveConversationProvider(ObjectMapper json) { this.json = json; }
    @Override public boolean configured() { return apiKey != null && !apiKey.isBlank(); }
    @Override public String providerName() { return "GEMINI_LIVE"; }

    @Override
    public LiveAccess createGuidedPhraseAccess(String instruction) throws Exception {
        if (!configured()) throw new IllegalStateException("Gemini Live is not configured.");
        Instant now = Instant.now();
        Map<String,Object> config = new LinkedHashMap<>();
        config.put("responseModalities", List.of("AUDIO"));
        config.put("systemInstruction", Map.of("parts", List.of(Map.of("text", instruction))));
        config.put("speechConfig", Map.of("voiceConfig", Map.of("prebuiltVoiceConfig", Map.of("voiceName", voice))));
        config.put("inputAudioTranscription", Map.of());
        config.put("outputAudioTranscription", Map.of());
        // Gemini 3.1 Flash Live currently rejects Affective Dialog. Keep this
        // out of the setup instead of allowing token provisioning to fail 400.
        config.put("thinkingConfig", Map.of("thinkingLevel", "LOW"));
        config.put("realtimeInputConfig", Map.of("automaticActivityDetection", Map.of("disabled", true)));
        config.put("sessionResumption", Map.of());
        config.put("contextWindowCompression", Map.of("slidingWindow", Map.of()));
        config.put("tools", List.of(Map.of("functionDeclarations", List.of(
            Map.of("name","prepare_practice_turn","description","Call before speaking each of the five learner turns. Provide the Japanese phrase to assess and its learning support.","parameters",Map.of("type","OBJECT","properties",Map.of("targetJapanese",Map.of("type","STRING"),"englishMeaning",Map.of("type","STRING"),"learnerInstruction",Map.of("type","STRING")),"required",List.of("targetJapanese","englishMeaning","learnerInstruction"))),
            Map.of("name","evaluate_learner_meaning","description","Call after each learner answer to record contextual quality separately from pronunciation.","parameters",Map.of("type","OBJECT","properties",Map.of("contextScore",Map.of("type","INTEGER"),"appropriate",Map.of("type","BOOLEAN"),"explanation",Map.of("type","STRING"),"betterResponse",Map.of("type","STRING")),"required",List.of("contextScore","appropriate","explanation","betterResponse")))
        ))));
        Map<String,Object> body = Map.of(
            "uses", 1,
            "expireTime", now.plus(25, ChronoUnit.MINUTES).toString(),
            "newSessionExpireTime", now.plus(1, ChronoUnit.MINUTES).toString(),
            "liveConnectConstraints", Map.of("model", "models/" + model, "config", config));
        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL)).timeout(Duration.ofSeconds(20))
            .header("x-goog-api-key", apiKey).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Gemini Live token provisioning failed: status={} body={}", response.statusCode(), safeError(response.body()));
            if (response.statusCode() == 400) throw new IllegalStateException("Gemini Live rejected the session configuration. Check the configured model and Render environment values.");
            if (response.statusCode() == 401 || response.statusCode() == 403) throw new IllegalStateException("Gemini Live authorization failed. Check that the Render API key belongs to a project with Gemini API billing enabled.");
            if (response.statusCode() == 429) throw new IllegalStateException("The conversation service is currently at its usage limit. Please try again shortly.");
            throw new IllegalStateException("Gemini Live is temporarily unavailable (HTTP " + response.statusCode() + ").");
        }
        JsonNode result = json.readTree(response.body());
        String token = result.path("name").asText();
        if (token.isBlank()) throw new IllegalStateException("Gemini did not return a Live access token.");
        return new LiveAccess(token, model, WS_URL, voice);
    }

    private String safeError(String body) {
        if (body == null) return "";
        String redacted = body.replaceAll("AIza[0-9A-Za-z_-]+", "[redacted]");
        return redacted.substring(0, Math.min(redacted.length(), 1200));
    }
}
