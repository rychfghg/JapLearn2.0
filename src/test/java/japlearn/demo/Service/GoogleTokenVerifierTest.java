package japlearn.demo.Service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class GoogleTokenVerifierTest {

    private static final String WEB = "web-id.apps.googleusercontent.com";
    private final GoogleTokenVerifier verifier = new GoogleTokenVerifier(WEB + ", android-id.apps.googleusercontent.com");

    private ObjectNode claims() {
        ObjectNode c = new ObjectMapper().createObjectNode();
        c.put("aud", WEB);
        c.put("iss", "https://accounts.google.com");
        c.put("exp", Instant.now().getEpochSecond() + 600);
        c.put("email_verified", "true");
        c.put("sub", "1234567890");
        c.put("email", "Ana@Gmail.com");
        c.put("given_name", "Ana");
        c.put("family_name", "Santos");
        return c;
    }

    @Test
    void acceptsValidTokenForOurClient() {
        GoogleTokenVerifier.GoogleProfile p = verifier.check(claims());
        assertThat(p).isNotNull();
        assertThat(p.email()).isEqualTo("ana@gmail.com");
        assertThat(p.subject()).isEqualTo("1234567890");
    }

    @Test
    void rejectsOtherAppsExpiredUnverifiedAndForeignIssuers() {
        ObjectNode otherApp = claims(); otherApp.put("aud", "someone-else.apps.googleusercontent.com");
        ObjectNode expired = claims(); expired.put("exp", Instant.now().getEpochSecond() - 10);
        ObjectNode unverified = claims(); unverified.put("email_verified", "false");
        ObjectNode issuer = claims(); issuer.put("iss", "https://evil.example.com");
        ObjectNode badEmail = claims(); badEmail.put("email", "ana😀@gmail.com");
        for (ObjectNode c : new ObjectNode[] { otherApp, expired, unverified, issuer, badEmail })
            assertThat(verifier.check(c)).isNull();
    }

    @Test
    void notConfiguredWithoutClientIds() {
        assertThat(new GoogleTokenVerifier("").isConfigured()).isFalse();
        assertThat(new GoogleTokenVerifier("").verify("anything")).isNull();
    }
}
