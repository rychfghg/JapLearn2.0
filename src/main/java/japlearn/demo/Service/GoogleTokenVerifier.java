package japlearn.demo.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Checks a Google ID token with Google before anyone is signed in. The token must be issued by
 * Google, for one of JapLearn's own OAuth client IDs (GOOGLE_CLIENT_IDS), not expired, and for a
 * verified email address. Client IDs are public; no client secret is used or needed.
 */
@Service
public class GoogleTokenVerifier {

    public record GoogleProfile(String subject, String email, String givenName, String familyName, String fullName) {}

    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Set<String> clientIds;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public GoogleTokenVerifier(@Value("${app.google.client-ids:}") String clientIds) {
        this.clientIds = Arrays.stream(clientIds == null ? new String[0] : clientIds.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    public boolean isConfigured() {
        return !clientIds.isEmpty();
    }

    /** Returns the verified profile, or null when the token is not a valid JapLearn Google sign-in. */
    public GoogleProfile verify(String idToken) {
        if (!isConfigured() || idToken == null || idToken.isBlank() || idToken.length() > 4096) return null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + URLEncoder.encode(idToken.trim(), StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            return check(JSON.readTree(response.body()));
        } catch (Exception e) {
            return null;
        }
    }

    /** Validates the decoded token claims (kept separate so it can be unit tested). */
    GoogleProfile check(JsonNode claims) {
        if (claims == null) return null;
        String aud = claims.path("aud").asText("");
        String iss = claims.path("iss").asText("");
        long exp = claims.path("exp").asLong(0);
        boolean verified = "true".equalsIgnoreCase(claims.path("email_verified").asText(""));
        String sub = claims.path("sub").asText("");
        String email = EmailInput.normalize(claims.path("email").asText(""));
        if (!clientIds.contains(aud) || !ISSUERS.contains(iss) || exp <= Instant.now().getEpochSecond()
                || !verified || sub.isBlank() || email == null) return null;
        return new GoogleProfile(sub, email, claims.path("given_name").asText(""),
                claims.path("family_name").asText(""), claims.path("name").asText(""));
    }
}
