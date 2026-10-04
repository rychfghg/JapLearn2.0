package japlearn.demo.Service;

import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

/**
 * One strict rule for every email typed into JapLearn (login, forgot password,
 * sign-up, admin edits) and for every address an email is sent to.
 *
 * Accepts exactly ONE plain address like name@gmail.com. Rejects anything that
 * could smuggle extra recipients or headers: commas, semicolons, spaces, line
 * breaks / control characters, quotes, angle brackets, display names, JSON or
 * database operators, scripts, and over-long input.
 */
public final class EmailInput {
    private EmailInput() {}

    private static final Pattern STRICT = Pattern.compile("^[a-z0-9](?:[a-z0-9._%+-]{0,62}[a-z0-9])?@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)*\\.[a-z]{2,24}$");

    /** Returns the normalized address, or null if the input is not exactly one safe email. */
    public static String normalize(Object raw) {
        if (!(raw instanceof String s)) return null;          // objects/arrays (e.g. {"$ne":null}) are rejected
        if (s.length() > 254) return null;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < 0x20 || ch == 0x7F) return null;           // CR/LF/tab/NUL: header-injection attempts
        }
        String v = s.trim().toLowerCase(Locale.ROOT);
        if (v.contains("..") || !STRICT.matcher(v).matches()) return null;
        return v;
    }

    /** Final guard right before sending: must parse as exactly one strict address. */
    public static InternetAddress recipient(String email) {
        String clean = normalize(email);
        if (clean == null) throw new IllegalArgumentException("Refusing to send to an invalid address");
        try {
            InternetAddress[] parsed = InternetAddress.parse(clean, true);
            if (parsed.length != 1) throw new IllegalArgumentException("Refusing to send to multiple addresses");
            parsed[0].validate();
            return parsed[0];
        } catch (AddressException e) {
            throw new IllegalArgumentException("Refusing to send to an invalid address");
        }
    }
}
