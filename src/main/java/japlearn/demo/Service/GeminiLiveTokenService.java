package japlearn.demo.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import japlearn.demo.Entity.GuidedPracticeScenario;

@Service
public class GeminiLiveTokenService {
    private static final String TOKEN_URL = "https://generativelanguage.googleapis.com/v1beta/auth_tokens";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json;
    @Value("${gemini.api.key:${GEMINI_API_KEY:}}") private String apiKey;
    @Value("${gemini.live.model:${GEMINI_LIVE_MODEL:gemini-3.1-flash-live-preview}}") private String model;
    @Value("${gemini.sumi.voice:${SUMI_GEMINI_VOICE:Aoede}}") private String voice;

    public GeminiLiveTokenService(ObjectMapper json) { this.json = json; }

    public Token issue(GuidedPracticeScenario scenario, int remainingSeconds) {
        if (apiKey == null || apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini Live is not configured on the server.");
        try {
            String instruction = instruction(scenario, remainingSeconds);
            Map<String,Object> config = new LinkedHashMap<>();
            config.put("responseModalities", List.of("AUDIO"));
            config.put("inputAudioTranscription", Map.of());
            config.put("outputAudioTranscription", Map.of());
            config.put("sessionResumption", Map.of());
            config.put("speechConfig", Map.of("voiceConfig", Map.of("prebuiltVoiceConfig", Map.of("voiceName", voice))));
            config.put("systemInstruction", Map.of("parts", List.of(Map.of("text", instruction))));
            config.put("thinkingConfig", Map.of("thinkingLevel", "minimal"));
            Map<String,Object> body = new LinkedHashMap<>();
            body.put("uses", 1);
            body.put("expireTime", Instant.now().plus(Math.min(30 * 60, remainingSeconds + 120), ChronoUnit.SECONDS).toString());
            body.put("newSessionExpireTime", Instant.now().plus(60, ChronoUnit.SECONDS).toString());
            body.put("liveConnectConstraints", Map.of("model", "models/" + model, "config", config));
            HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .header("x-goog-api-key", apiKey).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini Live could not create a secure session token.");
            JsonNode result = json.readTree(response.body());
            String name = result.path("name").asText();
            if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini Live returned an invalid session token.");
            return new Token(name, model, voice, instruction);
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini Live is temporarily unavailable.", ex); }
    }

    private String instruction(GuidedPracticeScenario s, int remaining) {
        return """
            You are Sumi, the live Japanese conversation partner inside JapLearn Guided Phrase Practice.
            The learner selected this scenario: %s.
            Scenario objective: %s.
            Learner level: absolute beginner Japanese.
            Allowed topics: %s.
            Target vocabulary: %s.
            Target grammar: %s.
            Target expressions: %s.
            Conduct a spoken real-life simulation. Speak primarily in short, beginner-appropriate Japanese.
            At the beginning of the session, warmly greet the learner in English, briefly explain the selected situation and objective in no more than two short sentences, then transition naturally into Japanese and begin the role-play with one clear question.
            Decide every opening, reaction, question, clarification, and follow-up dynamically from what the learner actually says. Never follow a predetermined dialogue and never require one exact answer.
            Ask one main question at a time, remain inside the selected scenario, and accept different valid beginner responses. React naturally and supportively. Never mock pronunciation mistakes.
            If the learner struggles, briefly explain in English, then return to Japanese. Keep this conversational rather than quiz-like.
            Naturally conclude when the scenario objective is achieved, the learner ends the session, or the application ends the remaining %d seconds of today's 20-minute allowance.
            Wait until the application tells you to begin, then deliver the welcome and start the role-play.
            """.formatted(s.getTitle(), s.getObjective(), safe(s.getAllowedTopics()), safe(s.getAllowedVocabulary()), safe(s.getAllowedGrammar()), safe(s.getTargetExpressions()), remaining);
    }
    private List<String> safe(List<String> values) { return values == null ? List.of() : values; }
    public record Token(String name, String model, String voice, String instruction) {}
}
