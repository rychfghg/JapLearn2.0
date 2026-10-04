package japlearn.demo.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class EmailInputTest {

    @Test
    void acceptsOnePlainAddress() {
        assertThat(EmailInput.normalize("  Ana.Cruz+jl@Gmail.com ")).isEqualTo("ana.cruz+jl@gmail.com");
        assertThat(EmailInput.recipient("student@cit.edu").getAddress()).isEqualTo("student@cit.edu");
    }

    @Test
    void rejectsEveryReroutingAndInjectionAttempt() {
        String[] attacks = {
            "victim@gmail.com\r\nBcc: attacker@maxxspace.com",          // header injection (CRLF)
            "victim@gmail.com\nTo: attacker@maxxspace.com",
            "victim@gmail.com%0d%0aBcc:attacker@maxxspace.com",          // encoded CRLF
            "victim@gmail.com,attacker@maxxspace.com",                   // second recipient
            "victim@gmail.com, attacker@maxxspace.com",
            "victim@gmail.com;attacker@maxxspace.com",
            "Victim <attacker@maxxspace.com>",                           // display-name trick
            "\"victim@gmail.com\" <attacker@maxxspace.com>",
            "attacker@maxxspace.com (victim@gmail.com)",
            "victim@gmail.com attacker@maxxspace.com",
            "{\"$ne\":null}", "{\"$gt\":\"\"}", "$where:1",                // NoSQL operators
            "' OR '1'='1", "admin@gmail.com' --",                         // SQL style
            "<script>alert(1)</script>@gmail.com", "a@gmail.com<script>",  // script
            "a@gmail.com\u0000", "a@аpple.com",                     // NUL byte, Cyrillic look-alike
            "a..b@gmail.com", "@gmail.com", "a@", "a@gmail", "",
            "a".repeat(250) + "@gmail.com"                                // over-long
        };
        for (String attack : attacks) {
            assertThat(EmailInput.normalize(attack)).as(attack).isNull();
            assertThatThrownBy(() -> EmailInput.recipient(attack)).as(attack).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejectsNonStringValues() {
        assertThat(EmailInput.normalize(Map.of("$ne", ""))).isNull();
        assertThat(EmailInput.normalize(123)).isNull();
        assertThat(EmailInput.normalize(null)).isNull();
    }
}
